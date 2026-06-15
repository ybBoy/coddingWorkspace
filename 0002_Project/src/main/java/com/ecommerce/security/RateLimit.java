package com.ecommerce.security;

import java.lang.annotation.*;

/**
 * 限流注解
 * 用于标记需要限流的接口
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流的key前缀
     */
    String key() default "";

    /**
     * 每秒允许的请求数
     * 默认100
     */
    double permitsPerSecond() default 100.0;

    /**
     * 获取令牌的超时时间（毫秒）
     * 默认500ms
     */
    long timeout() default 500;

    /**
     * 限流类型
     */
    LimitType limitType() default LimitType.DEFAULT;

    /**
     * 限流类型枚举
     */
    enum LimitType {
        /**
         * 默认策略（全局限流）
         */
        DEFAULT,
        /**
         * 根据IP限流
         */
        IP,
        /**
         * 根据用户限流
         */
        USER
    }
}
