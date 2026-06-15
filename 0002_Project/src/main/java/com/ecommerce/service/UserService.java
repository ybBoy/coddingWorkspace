package com.ecommerce.service;

import com.ecommerce.model.User;
import com.ecommerce.repository.DataPersistenceManager;
import com.ecommerce.repository.DataStore;
import com.ecommerce.security.JwtTokenUtil;
import com.ecommerce.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务类
 * 负责用户注册、登录、认证等业务逻辑
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final DataStore dataStore;
    private final DataPersistenceManager dataPersistenceManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    @Value("${init.admin.username:admin}")
    private String initAdminUsername;

    @Value("${init.admin.password:admin123}")
    private String initAdminPassword;

    @Value("${init.admin.email:admin@ecommerce.com}")
    private String initAdminEmail;

    @Autowired
    public UserService(DataPersistenceManager dataPersistenceManager,
                       PasswordEncoder passwordEncoder,
                       JwtTokenUtil jwtTokenUtil) {
        this.dataStore = DataStore.getInstance();
        this.dataPersistenceManager = dataPersistenceManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * 初始化管理员账号
     */
    @PostConstruct
    public void initAdminUser() {
        // 检查是否已有管理员
        boolean hasAdmin = dataStore.getAllUsers().stream()
                .anyMatch(User::isAdmin);

        if (!hasAdmin) {
            // 检查管理员用户名是否已被使用
            if (!dataStore.existsByUsername(initAdminUsername)) {
                User admin = new User();
                admin.setId(IdGenerator.generateUUID());
                admin.setUsername(initAdminUsername);
                admin.setPassword(passwordEncoder.encode(initAdminPassword));
                admin.setEmail(initAdminEmail);
                admin.setNickname("System Admin");
                admin.setRole("ADMIN");
                admin.setStatus("ACTIVE");
                admin.setCreatedAt(LocalDateTime.now());
                admin.setUpdatedAt(LocalDateTime.now());

                dataStore.addUser(admin);
                dataPersistenceManager.saveData();
                logger.info("Initial admin user created: {}", initAdminUsername);
            }
        }
    }

    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码
     * @param email 邮箱
     * @return 注册的用户
     */
    public User register(String username, String password, String email) {
        // 校验用户名
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Username cannot be empty");
        }
        if (username.length() < 3 || username.length() > 20) {
            throw new RuntimeException("Username must be between 3 and 20 characters");
        }

        // 校验密码
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("Password cannot be empty");
        }
        if (password.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        // 检查用户名是否已存在
        if (dataStore.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        // 创建用户
        User user = new User();
        user.setId(IdGenerator.generateUUID());
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setNickname(username);
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        dataStore.addUser(user);
        dataPersistenceManager.saveData();

        logger.info("User registered: {}", username);
        return user;
    }

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录结果（包含Token和用户信息）
     */
    public Map<String, Object> login(String username, String password, String loginIp) {
        // 校验参数
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Username cannot be empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("Password cannot be empty");
        }

        // 查找用户
        User user = dataStore.getUserByUsername(username);
        if (user == null) {
            throw new RuntimeException("Invalid username or password");
        }

        // 检查账号状态
        if (!user.isActive()) {
            throw new RuntimeException("Account is disabled");
        }

        // 校验密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        // 更新登录信息
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(loginIp);
        user.setUpdatedAt(LocalDateTime.now());
        dataStore.updateUser(user);
        dataPersistenceManager.saveData();

        // 生成JWT Token
        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("email", user.getEmail());
        result.put("role", user.getRole());
        result.put("avatar", user.getAvatar());

        logger.info("User logged in: {}", username);
        return result;
    }

    /**
     * 根据ID获取用户
     * @param id 用户ID
     * @return 用户对象
     */
    public User getUserById(String id) {
        return dataStore.getUserById(id);
    }

    /**
     * 根据用户名获取用户
     * @param username 用户名
     * @return 用户对象
     */
    public User getUserByUsername(String username) {
        return dataStore.getUserByUsername(username);
    }

    /**
     * 更新用户信息
     * @param id 用户ID
     * @param updateUser 更新的用户信息
     * @return 更新后的用户
     */
    public User updateUser(String id, User updateUser) {
        User user = dataStore.getUserById(id);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (updateUser.getNickname() != null) {
            user.setNickname(updateUser.getNickname());
        }
        if (updateUser.getEmail() != null) {
            user.setEmail(updateUser.getEmail());
        }
        if (updateUser.getAvatar() != null) {
            user.setAvatar(updateUser.getAvatar());
        }

        user.setUpdatedAt(LocalDateTime.now());
        dataStore.updateUser(user);
        dataPersistenceManager.saveData();

        return user;
    }

    /**
     * 修改密码
     * @param id 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    public void changePassword(String id, String oldPassword, String newPassword) {
        User user = dataStore.getUserById(id);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // 校验旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }

        // 校验新密码
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        dataStore.updateUser(user);
        dataPersistenceManager.saveData();
    }

    /**
     * 检查用户名是否已存在
     * @param username 用户名
     * @return 是否存在
     */
    public boolean existsByUsername(String username) {
        return dataStore.existsByUsername(username);
    }
}
