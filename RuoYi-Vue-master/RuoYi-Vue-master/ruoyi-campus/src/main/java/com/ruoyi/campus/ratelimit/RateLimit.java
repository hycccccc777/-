package com.ruoyi.campus.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流。
 *
 * <p>标在 Controller 方法上：<pre>{@code
 * @RateLimit(permits = 5, seconds = 60, key = "login")
 * }</pre>
 *
 * <p>作用是兜住暴力破解、刷单、爬接口这类流量。正常用户几乎不会触发，
 * 所以阈值可以设得比较宽松。
 *
 * @author campus
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit
{
    /** 时间窗口内允许的请求数 */
    int permits() default 10;

    /** 窗口长度（秒） */
    int seconds() default 60;

    /** 限流维度名，仅用于日志区分 */
    String key() default "default";

    /** 限流维度：按登录用户（默认）、按 IP */
    Scope scope() default Scope.USER;

    enum Scope
    {
        USER, IP
    }
}