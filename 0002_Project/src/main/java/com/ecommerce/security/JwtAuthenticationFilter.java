package com.ecommerce.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 * 从请求中提取JWT Token并进行认证
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Value("${security.public.urls:}")
    private String[] publicUrls;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 从请求中获取JWT Token
        String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token) && jwtTokenUtil.validateToken(token)) {
            try {
                // 从Token中获取用户信息
                String username = jwtTokenUtil.getUsernameFromToken(token);
                String userId = jwtTokenUtil.getUserIdFromToken(token);
                String role = jwtTokenUtil.getRoleFromToken(token);

                // 将用户信息设置到Request Attribute中，供后续使用
                request.setAttribute("userId", userId);
                request.setAttribute("username", username);
                request.setAttribute("role", role);

                // 创建Authentication对象
                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                // 设置到SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                logger.debug("User authenticated: userId={}, username={}, role={}", userId, username, role);
            } catch (Exception e) {
                logger.error("Failed to set user authentication: {}", e.getMessage());
                // 不抛出异常，继续执行（让后续的Security过滤器处理）
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中获取JWT Token
     * 支持两种方式：
     * 1. Authorization Header: Bearer <token>
     * 2. Request Parameter: token=<token>
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 从Header中获取
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 从Parameter中获取（用于WebSocket等场景）
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }

        return null;
    }
}
