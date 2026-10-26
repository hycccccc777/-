#!/usr/bin/env node
// 本地负载均衡代理（仅用于无 Docker/Nginx 环境下的多实例验证）
//
// 生产用 docs/ops/nginx-lb.conf；这个脚本只做同样的事的最小实现：
//   least_conn 选节点 -> 转发 -> 节点失败自动摘除 -> 恢复后回池
//
// 用法：
//   node scripts/lb-proxy.mjs --listen 8088 --backends 8080,8082
//
// 为什么需要它：验证「多实例是否真的无状态」不能只靠看代码。
// 起两个后端，经代理轮流打，若令牌/限流/定时任务没做到共享，
// 立刻会出现「刚登录就 401」「配额翻倍」「任务跑两遍」。

import http from 'node:http';

function arg(name, def) {
  const i = process.argv.indexOf('--' + name);
  return i > -1 && process.argv[i + 1] ? process.argv[i + 1] : def;
}

const listenPort = Number(arg('listen', '8088'));
const backends = arg('backends', '8080,8082')
  .split(',')
  .map((s) => s.trim())
  .filter(Boolean)
  .map((port) => ({ port: Number(port), inflight: 0, failUntil: 0, ok: 0, fail: 0 }));

// 灰度：把一定比例的请求单独导到 canary 端口（对应 nginx split_clients）
//   --canary-port 8083        灰度节点
//   --canary-percent 10       10% 流量走灰度（0 = 全量切回）
//   --canary-header X-Canary  带该头的请求强制走灰度（方便验证，不影响其他用户）
const canaryPort = arg('canary-port', '0') === '0' ? 0 : Number(arg('canary-port', '0'));
const canaryPercent = Number(arg('canary-percent', '0'));
const canaryHeader = arg('canary-header', 'x-canary');
const canary = canaryPort ? { port: canaryPort, inflight: 0, failUntil: 0, ok: 0, fail: 0 } : null;

const FAIL_COOLDOWN_MS = 5000; // 失败后 5 秒内不再分配（对应 nginx fail_timeout）

let rr = 0;

// 灰度判定：显式 header 优先（用于验证），其次按百分比抽样（用于真实放量）
function pickCanary(req) {
  if (!canary) return null;
  // 写接口永不进灰度：否则会出现「写在新版、读在旧版」的数据错乱，
  // 或新版 bug 直接产生脏数据（与 nginx-lb.conf 的写接口钉稳定池一致）
  const method = (req.method || 'GET').toUpperCase();
  if (method !== 'GET' && method !== 'HEAD') return null;
  const forced = (req.headers[canaryHeader] || '').toString().toLowerCase();
  if (forced === '1' || forced === 'true') return canary;
  if (forced === '0' || forced === 'false') return null;
  if (canaryPercent <= 0) return null;
  return Math.random() * 100 < canaryPercent ? canary : null;
}

function pick() {
  const now = Date.now();
  const alive = backends.filter((b) => b.failUntil <= now);
  const pool = alive.length ? alive : backends; // 全挂时退回全部，让请求自然失败而不是空转
  const min = Math.min(...pool.map((b) => b.inflight));
  const cands = pool.filter((b) => b.inflight === min);
  // 与 nginx least_conn 一致：并发数打平时按轮询分配，
  // 否则串行请求永远落到第一个节点（in-flight 都是 0），看不出分流效果
  const chosen = cands[rr % cands.length];
  rr = (rr + 1) % Number.MAX_SAFE_INTEGER;
  return chosen;
}

const server = http.createServer((req, res) => {
  // 调试端点：看每个节点的分流计数与当前 in-flight
  if (req.url === '/lb/stats') {
    const all = canary ? backends.concat([canary]) : backends;
    const body = JSON.stringify(
      {
        canaryPercent,
        nodes: all.map((b) => ({
          port: b.port,
          role: canary && b.port === canary.port ? 'canary' : 'stable',
          ok: b.ok,
          fail: b.fail,
          inflight: b.inflight,
          down: b.failUntil > Date.now()
        }))
      },
      null,
      2
    );
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(body);
    return;
  }

  const target = pickCanary(req) || pick();
  target.inflight += 1;

  const headers = { ...req.headers };
  headers.host = 'localhost:' + target.port;
  // 透传真实客户端 IP，限流按 IP 计数才不会被所有请求算成同一个
  headers['x-real-ip'] = req.socket.remoteAddress?.replace('::ffff:', '') ?? '127.0.0.1';
  headers['x-forwarded-for'] = headers['x-real-ip'];

  const upstream = http.request(
    { host: '127.0.0.1', port: target.port, path: req.url, method: req.method, headers, timeout: 30000 },
    (upRes) => {
      target.inflight -= 1;
      target.ok += 1;
      // 把节点标识一并回给调用方，肉眼可验证分流
      res.writeHead(upRes.statusCode || 502, { ...upRes.headers, 'X-LB-Backend': String(target.port) });
      upRes.pipe(res);
    }
  );

  upstream.on('timeout', () => upstream.destroy(new Error('upstream timeout')));
  upstream.on('error', (err) => {
    target.inflight = Math.max(0, target.inflight - 1);
    target.fail += 1;
    target.failUntil = Date.now() + FAIL_COOLDOWN_MS;
    if (!res.headersSent) {
      res.writeHead(502, { 'Content-Type': 'application/json', 'X-LB-Backend': String(target.port) });
      res.end(JSON.stringify({ code: 502, message: 'backend unreachable: ' + target.port + ' (' + err.code + ')' }));
    }
  });

  // 保留请求体：POST /api/v1/auth/login 这类如果丢了 body 会直接 500
  req.pipe(upstream);
});

server.listen(listenPort, '127.0.0.1', () => {
  console.log('[lb] listening on http://127.0.0.1:' + listenPort);
  console.log('[lb] backends: ' + backends.map((b) => b.port).join(', '));
  if (canary) { console.log('[lb] canary  : ' + canary.port + '  ' + canaryPercent + '%  (header ' + canaryHeader + '=1 强制)'); }
  console.log('[lb] stats:    http://127.0.0.1:' + listenPort + '/lb/stats');
});