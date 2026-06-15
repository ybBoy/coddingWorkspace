package com.ecommerce.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 限流切面
 * 使用Guava RateLimiter实现接口限流
 * 防止恶意攻击和接口滥用
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);

    /**
     * 存储不同key的RateLimiter
     * key: 限流key, value: RateLimiter实例
     */
    private final ConcurrentHashMap<String, RateLimiter> rateLimiterMap = new ConcurrentHashMap<>();

    /**
     * IP级别限流缓存
     * 使用Guava Cache自动过期
     */
    private final Cache<String, RateLimiter> ipRateLimiterCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    /**
     * 用户级别限流缓存
     */
    private final Cache<String, RateLimiter> userRateLimiterCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    /**
     * 环绕通知，处理限流逻辑
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        // 获取限流key
        String key = getLimitKey(point, rateLimit);

        // 获取或创建RateLimiter
        RateLimiter rateLimiter = getRateLimiter(key, rateLimit);

        // 尝试获取令牌
        boolean acquired = rateLimiter.tryAcquire(rateLimit.timeout(), TimeUnit.MILLISECONDS);

        if (!acquired) {
            logger.warn("Rate limit exceeded for key: {}, permitsPerSecond: {}", 
                    key, rateLimit.permitsPerSecond());
            throw new RuntimeException("系统繁忙，请稍后再试");
        }

        return point.proceed();
    }

    /**
     * 获取限流key
     */
    private String getLimitKey(ProceedingJoinPoint point, RateLimit rateLimit) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String baseKey = rateLimit.key().isEmpty() ? 
                className + ":" + methodName : rateLimit.key();

        switch (rateLimit.limitType()) {
            case IP:
                String ip = getClientIp();
                return "IP:" + ip + ":" + baseKey;
            case USER:
                String userId = getCurrentUserId();
                return "USER:" + (userId != null ? userId : "anonymous") + ":" + baseKey;
            default:
                return "GLOBAL:" + baseKey;
        }
    }

    /**
     * 获取或创建RateLimiter
     */
    private RateLimiter getRateLimiter(String key, RateLimit rateLimit) {
        switch (rateLimit.limitType()) {
            case IP:
                try {
                    return ipRateLimiterCache.get(key, () -> 
                            RateLimiter.create(rateLimit.permitsPerSecond()));
                } catch (Exception e) {
                    return RateLimiter.create(rateLimit.permitsPerSecond());
                }
            case USER:
                try {
                    return userRateLimiterCache.get(key, () -> 
                            RateLimiter.create(rateLimit.permitsPerSecond()));
                } catch (Exception e) {
                    return RateLimiter.create(rateLimit.permitsPerSecond());
                }
            default:
                return rateLimiterMap.computeIfAbsent(key, 
                        k -> RateLimiter.create(rateLimit.permitsPerSecond()));
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) 
                    RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return "unknown";
            }
            HttpServletRequest request = attributes.getRequest();
            
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            
            // 多个代理时取第一个IP
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 获取当前用户ID
     * 从Request Attribute中获取（由JWT Filter设置）
     */
    private String getCurrentUserId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) 
                    RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            HttpServletRequest request = attributes.getRequest();
            return (String) request.getAttribute("userId");
        } catch (Exception e) {
            return null;
        }
    }
}
