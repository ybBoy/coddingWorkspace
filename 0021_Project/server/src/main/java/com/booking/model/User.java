package com.booking.model;

/**
 * User 用户模型
 * 职责：表示一个登录用户，包含身份信息和角色权限
 * role: user（普通用户，只能预约/取消自己）、admin（管理员，可签到、场次管理、导出）
 */
public class User {
    public static final String ROLE_USER = "user";
    public static final String ROLE_ADMIN = "admin";

    private String employeeId;
    private String userName;
    private String role;
    private long loginAt;
    private String wsSessionId;  // 关联 WebSocket 连接

    public User() {
    }

    public User(String employeeId, String userName, String role) {
        this.employeeId = employeeId;
        this.userName = userName;
        this.role = role;
        this.loginAt = System.currentTimeMillis();
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public long getLoginAt() {
        return loginAt;
    }

    public void setLoginAt(long loginAt) {
        this.loginAt = loginAt;
    }

    public String getWsSessionId() {
        return wsSessionId;
    }

    public void setWsSessionId(String wsSessionId) {
        this.wsSessionId = wsSessionId;
    }
}
