package com.ruoyi.campus.observability;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 给每个响应打上「由哪个实例处理」的标记。
 *
 * <p>为什么需要：多实例部署后，同一个接口报错，光看日志不知道是哪台机器处理的那一次；
 * 压测时也看不出负载均衡是否真的把流量分到了所有节点。加上响应头之后，
 * 一条 curl 就能看出分流情况，排障时也能直接对上机器。
 *
 * <p>取值优先级：环境变量 {@code CAMPUS_INSTANCE_ID} &gt; {@code 主机名:端口}。
 * 容器里通常用 pod 名注入，本机则是 {@code LAPTOP-XXXX:8080}。
 *
 * <p>注意：这是只读的观测信息，不参与任何鉴权或路由判断。
 *
 * @author campus
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class InstanceIdFilter extends OncePerRequestFilter
{
    public static final String HEADER = "X-Instance-Id";

    private final String instanceId;

    public InstanceIdFilter(@Value("${CAMPUS_INSTANCE_ID:}") String configured,
            @Value("${server.port:8080}") String port)
    {
        String id = (configured == null) ? "" : configured.trim();
        if (id.isEmpty())
        {
            id = hostname() + ":" + port;
        }
        this.instanceId = id;
    }

    public String getInstanceId()
    {
        return instanceId;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        response.setHeader(HEADER, instanceId);
        chain.doFilter(request, response);
    }

    private static String hostname()
    {
        String env = System.getenv("COMPUTERNAME");
        if (env == null || env.isBlank())
        {
            env = System.getenv("HOSTNAME");
        }
        if (env != null && !env.isBlank())
        {
            return env;
        }
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e)
        {
            return "unknown";
        }
    }
}
