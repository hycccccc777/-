package com.ruoyi.campus.observability;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.MDC;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

/**
 * 把日志直接推给 Loki（按 Loki push API 的 JSON 格式），用于多实例下的日志集中化。
 *
 * <p>为什么推而不是让 Promtail 抓文件：
 * <ul>
 *   <li>容器里日志写到 stdout 或临时卷，宿主机上抓文件要额外挂载与解析</li>
 *   <li>多副本时每个 pod 一个目录，采集配置要跟着变；应用自己推则与部署形态无关</li>
 *   <li>直接推带结构化字段（traceId / instance / level），查询时不用再正则解析</li>
 * </ul>
 *
 * <p>可靠性取舍（写进代码注释，因为这是最容易被问的点）：
 * <ul>
 *   <li>有界队列 + 满则丢弃最旧：日志不该把业务线程堵死，也不该把内存吃光</li>
 *   <li>推送失败只计数、不重试：重试会在 Loki 故障时堆积成雪崩</li>
 *   <li>丢弃与失败次数每 30 秒打一行到 stderr，避免"日志悄悄丢了却没人知道"</li>
 * </ul>
 *
 * <p>默认关闭：只有显式配置 {@code LOKI_URL} 时才启用，本机开发与 CI 不受影响。
 *
 * @author campus
 */
public class LokiLogAppender extends UnsynchronizedAppenderBase<ILoggingEvent>
{
    /** 单条 value 的时间戳精度到纳秒（Loki 要求字符串形式的纳秒） */
    private static final long NANOS_PER_MILLI = 1_000_000L;

    private final BlockingQueue<String[]> queue = new ArrayBlockingQueue<>(4096);

    private final AtomicLong dropped = new AtomicLong();

    private final AtomicLong failed = new AtomicLong();

    private final AtomicLong sent = new AtomicLong();

    private String url;

    private String application = "campus-shop";

    private String instance = "unknown";

    private int batchSize = 200;

    private HttpClient client;

    private Thread worker;

    private volatile boolean running;

    private volatile long lastReport = System.currentTimeMillis();

    public void setUrl(String url)
    {
        this.url = url;
    }

    public void setApplication(String application)
    {
        this.application = application;
    }

    public void setInstance(String instance)
    {
        this.instance = (instance == null || instance.isBlank()) ? "unknown" : instance;
    }

    public void setBatchSize(int batchSize)
    {
        this.batchSize = Math.max(1, batchSize);
    }

    @Override
    public void start()
    {
        if (url == null || url.isBlank())
        {
            // 未配置地址视为关闭：不报错，避免本机开发时刷一屏告警
            addInfo("LokiLogAppender 未配置 url，已跳过");
            super.stop();
            return;
        }
        try
        {
            client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            running = true;
            worker = new Thread(this::drainLoop, "loki-log-shipper");
            worker.setDaemon(true);
            worker.start();
            super.start();
            addInfo("LokiLogAppender 已启用 -> " + url + "  instance=" + instance);
        }
        catch (RuntimeException e)
        {
            addError("LokiLogAppender 启动失败：" + e.getMessage(), e);
            super.stop();
        }
    }

    @Override
    protected void append(ILoggingEvent event)
    {
        if (!running)
        {
            return;
        }
        String[] value = toLokiValue(event);
        if (!queue.offer(value))
        {
            // 队列满：丢最旧的一条再放新的。丢掉的是最老的日志，保住最近的现场
            queue.poll();
            queue.offer(value);
            dropped.incrementAndGet();
        }
    }

    private String[] toLokiValue(ILoggingEvent event)
    {
        String ts = Long.toString(event.getTimeStamp() * NANOS_PER_MILLI);
        return new String[] { ts, toJsonLine(event) };
    }

    private String toJsonLine(ILoggingEvent event)
    {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        field(sb, "ts", Long.toString(event.getTimeStamp())).append(',');
        field(sb, "level", event.getLevel().toString()).append(',');
        field(sb, "thread", event.getThreadName()).append(',');
        field(sb, "logger", event.getLoggerName()).append(',');
        // traceId 从 MDC 取：与响应头、访问日志用的是同一个来源，才能串起来
        String traceId = MDC.get(TraceContext.MDC_KEY);
        field(sb, "traceId", traceId == null ? "" : traceId).append(',');
        field(sb, "instance", instance).append(',');
        field(sb, "msg", event.getFormattedMessage());
        IThrowableProxy throwable = event.getThrowableProxy();
        if (throwable != null)
        {
            sb.append(',');
            field(sb, "error", throwable.getClassName() + ": " + throwable.getMessage());
        }
        sb.append('}');
        return sb.toString();
    }

    private static StringBuilder field(StringBuilder sb, String name, String value)
    {
        sb.append('"').append(name).append("\":\"");
        escape(sb, value);
        sb.append('"');
        return sb;
    }

    /** 手写转义：不引第三方 JSON 库，避免为一个 appender 增加依赖面 */
    private static void escape(StringBuilder sb, String s)
    {
        if (s == null)
        {
            return;
        }
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            switch (c)
            {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default ->
                {
                    if (c < 0x20)
                    {
                        sb.append(String.format("\\u%04x", (int) c));
                    }
                    else
                    {
                        sb.append(c);
                    }
                }
            }
        }
    }

    private void drainLoop()
    {
        List<String[]> batch = new ArrayList<>(batchSize);
        while (running || !queue.isEmpty())
        {
            try
            {
                String[] first = queue.poll(500, TimeUnit.MILLISECONDS);
                if (first == null)
                {
                    reportIfDue();
                    continue;
                }
                batch.clear();
                batch.add(first);
                queue.drainTo(batch, batchSize - 1);
                push(batch);
                reportIfDue();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return;
            }
            catch (RuntimeException e)
            {
                failed.incrementAndGet();
            }
        }
    }

    private void push(List<String[]> batch)
    {
        StringBuilder body = new StringBuilder(1024);
        body.append("{\"streams\":[{\"stream\":{");
        body.append("\"application\":\"").append(application).append("\",");
        body.append("\"instance\":\"").append(instance).append("\"},");
        body.append("\"values\":[");
        for (int i = 0; i < batch.size(); i++)
        {
            if (i > 0)
            {
                body.append(',');
            }
            String[] v = batch.get(i);
            body.append("[\"").append(v[0]).append("\",\"");
            escape(body, v[1]);
            body.append("\"]");
        }
        body.append("]}]}");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        try
        {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            int code = response.statusCode();
            if (code >= 200 && code < 300)
            {
                sent.addAndGet(batch.size());
            }
            else if (code != 429 && code != 400)
            {
                // 429（限流）与 400（个别脏数据）属于可容忍，不计失败避免噪音
                failed.incrementAndGet();
            }
        }
        catch (Exception e)
        {
            // 不重试：Loki 故障时重试会堆积成雪崩；这里只计数，由下面的定期报告暴露
            failed.incrementAndGet();
        }
    }

    private void reportIfDue()
    {
        long now = System.currentTimeMillis();
        if (now - lastReport < 30_000)
        {
            return;
        }
        lastReport = now;
        long f = failed.get();
        long d = dropped.get();
        if (f > 0 || d > 0)
        {
            System.err.println("[loki] sent=" + sent.get() + " failedBatches=" + f + " droppedLines=" + d
                    + "（失败不重试、队列满丢最旧，详见 LokiLogAppender 注释）");
        }
    }

    @Override
    public void stop()
    {
        running = false;
        if (worker != null)
        {
            try
            {
                worker.join(3000);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
        super.stop();
    }
}
