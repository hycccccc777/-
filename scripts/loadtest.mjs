// 轻量压测：统计 QPS / 响应时间分位 / 错误率
// 用法：node scripts/loadtest.mjs [baseUrl] [concurrency] [seconds]
// 例：  node scripts/loadtest.mjs http://localhost:8080 50 20
import http from 'node:http';

const base = process.argv[2] || 'http://localhost:8080';
const concurrency = Number(process.argv[3] || 50);
const seconds = Number(process.argv[4] || 20);

const scenarios = [
  { name: '健康探活', path: '/api/v1/system/health', weight: 1 },
  { name: '商品列表(分页20)', path: '/api/v1/products?page=1&pageSize=20', weight: 4 },
  { name: '类目列表', path: '/api/v1/categories', weight: 2 },
  { name: '商品详情', path: '/api/v1/products/1', weight: 2 },
];

const weighted = [];
for (const s of scenarios) for (let i = 0; i < s.weight; i++) weighted.push(s);

function once(target) {
  return new Promise((resolve) => {
    const url = new URL(target.path, base);
    const started = process.hrtime.bigint();
    const req = http.get({ hostname: url.hostname, port: url.port, path: url.pathname + url.search }, (res) => {
      res.resume();
      res.on('end', () => {
        const ms = Number(process.hrtime.bigint() - started) / 1e6;
        resolve({ ms, ok: res.statusCode >= 200 && res.statusCode < 300, status: res.statusCode, name: target.name });
      });
    });
    req.on('error', () => {
      const ms = Number(process.hrtime.bigint() - started) / 1e6;
      resolve({ ms, ok: false, status: 0, name: target.name });
    });
    req.setTimeout(10000, () => { req.destroy(); });
  });
}

function pct(arr, p) {
  if (!arr.length) return 0;
  const i = Math.min(arr.length - 1, Math.floor((p / 100) * arr.length));
  return arr[i];
}

const results = [];
const perPath = new Map();
const deadline = Date.now() + seconds * 1000;

const workers = Array.from({ length: concurrency }, async (_, id) => {
  let i = id;
  while (Date.now() < deadline) {
    const target = weighted[i++ % weighted.length];
    const r = await once(target);
    results.push(r);
    if (!perPath.has(r.name)) perPath.set(r.name, []);
    perPath.get(r.name).push(r);
  }
});

await Promise.all(workers);

const total = results.length;
const okCount = results.filter((r) => r.ok).length;
const times = results.map((r) => r.ms).sort((a, b) => a - b);
const qps = total / seconds;

const rows = [...perPath.entries()].map(([name, arr]) => {
  const t = arr.map((x) => x.ms).sort((a, b) => a - b);
  const ok = arr.filter((x) => x.ok).length;
  return { name, n: arr.length, qps: (arr.length / seconds).toFixed(1), avg: (t.reduce((a, b) => a + b, 0) / t.length).toFixed(1), p95: pct(t, 95).toFixed(1), p99: pct(t, 99).toFixed(1), err: (((arr.length - ok) / arr.length) * 100).toFixed(2) };
});

console.log(`\n压测目标: ${base}`);
console.log(`并发 ${concurrency} · 持续 ${seconds}s · 请求总数 ${total}`);
console.log(`\n总 QPS: ${qps.toFixed(1)}   成功 ${okCount}/${total}   错误率 ${(((total - okCount) / total) * 100).toFixed(2)}%`);
console.log(`RT  avg ${(times.reduce((a, b) => a + b, 0) / times.length).toFixed(1)}ms  p50 ${pct(times, 50).toFixed(1)}ms  p95 ${pct(times, 95).toFixed(1)}ms  p99 ${pct(times, 99).toFixed(1)}ms  max ${times[times.length - 1].toFixed(1)}ms`);

console.log(`\n| 场景 | 请求数 | QPS | avg(ms) | p95(ms) | p99(ms) | 错误率 |`);
console.log(`|---|---|---|---|---|---|---|`);
for (const r of rows) console.log(`| ${r.name} | ${r.n} | ${r.qps} | ${r.avg} | ${r.p95} | ${r.p99} | ${r.err}% |`);

console.log(`\nSUMMARY ${JSON.stringify({ base, concurrency, seconds, total, qps: Number(qps.toFixed(1)), errorRate: Number((((total - okCount) / total) * 100).toFixed(2)), avg: Number((times.reduce((a, b) => a + b, 0) / times.length).toFixed(1)), p50: Number(pct(times, 50).toFixed(1)), p95: Number(pct(times, 95).toFixed(1)), p99: Number(pct(times, 99).toFixed(1)), max: Number(times[times.length - 1].toFixed(1)) })}`);
