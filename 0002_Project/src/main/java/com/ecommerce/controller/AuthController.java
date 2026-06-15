package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.model.User;
import com.ecommerce.security.RateLimit;
import com.ecommerce.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 用户认证控制器
 * 提供用户注册、登录、获取用户信息等接口
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户注册
     * POST /api/auth/register
     * 限流：每分钟10次（防止恶意注册）
     */
    @PostMapping("/register")
    @RateLimit(permitsPerSecond = 10.0 / 60.0, timeout = 1000, limitType = RateLimit.LimitType.IP)
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        String email = params.get("email");

        try {
            User user = userService.register(username, password, email);
            
            // 注册成功后自动登录
            // 这里不自动登录，让用户重新登录
            return Result.success("Registration successful", null);
        } catch (RuntimeException e) {
            logger.error("Registration failed: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户登录
     * POST /api/auth/login
     * 限流：每分钟10次（防止暴力破解）
     */
    @PostMapping("/login")
    @RateLimit(permitsPerSecond = 10.0 / 60.0, timeout = 1000, limitType = RateLimit.LimitType.IP)
    public Result<Map<String, Object>> login(
            @RequestBody Map<String, String> params,
            HttpServletRequest request) {
        String username = params.get("username");
        String password = params.get("password");

        // 获取客户端IP
        String loginIp = getClientIp(request);

        try {
            Map<String, Object> result = userService.login(username, password, loginIp);
            return Result.success("Login successful", result);
        } catch (RuntimeException e) {
            logger.error("Login failed for user {}: {}", username, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查用户名是否已存在
     * GET /api/auth/check-username?username=xxx
     */
    @GetMapping("/check-username")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        if (username == null || username.trim().isEmpty()) {
            return Result.error("Username cannot be empty");
        }
        boolean exists = userService.existsByUsername(username);
        return Result.success(exists);
    }

    /**
     * 获取当前用户信息
     * GET /api/auth/me
     * 需要登录
     */
    @GetMapping("/me")
    public Result<User> getCurrentUser(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }

        User user = userService.getUserById(userId);
        if (user == null) {
            return Result.error("User not found");
        }

        // 不返回密码
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 更新用户信息
     * PUT /api/auth/me
     * 需要登录
     */
    @PutMapping("/me")
    public Result<User> updateUser(
            @RequestBody User updateUser,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }

        try {
            User user = userService.updateUser(userId, updateUser);
            user.setPassword(null);
            return Result.success("Profile updated successfully", user);
        } catch (RuntimeException e) {
            logger.error("Update profile failed: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改密码
     * POST /api/auth/change-password
     * 需要登录
     */
    @PostMapping("/change-password")
    public Result<Void> changePassword(
            @RequestBody Map<String, String> params,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }

        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        try {
            userService.changePassword(userId, oldPassword, newPassword);
            return Result.success("Password changed successfully");
        } catch (RuntimeException e) {
            logger.error("Change password failed: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 登出
     * POST /api/auth/logout
     * 实际上JWT是无状态的，这里主要是前端清除Token
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        // JWT是无状态的，服务端不需要做特殊处理
        // 这里可以记录登出日志
        String userId = (String) request.getAttribute("userId");
        if (userId != null) {
            logger.info("User logged out: {}", userId);
        }
        return Result.success("Logged out successfully");
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
