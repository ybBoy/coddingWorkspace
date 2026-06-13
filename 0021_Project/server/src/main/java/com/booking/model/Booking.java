package com.booking.model;

/**
 * Booking 预约记录实体类
 * 职责：表示一条预约记录，包含用户身份、场次、状态等信息
 * 身份：employeeId（工号，必填，唯一标识）+ userName（姓名）+ phone（可选）
 * 状态：booked（已预约）、waitlist（候补中）、checkedIn（已签到）、cancelled（已取消）
 */
public class Booking {
    public static final String STATUS_BOOKED = "booked";
    public static final String STATUS_WAITLIST = "waitlist";
    public static final String STATUS_CHECKED_IN = "checkedIn";
    public static final String STATUS_CANCELLED = "cancelled";

    private String id;
    private String sessionId;
    private String employeeId;  // 工号，身份唯一标识
    private String userName;
    private String phone;       // 手机号，可选
    private String status;      // booked, waitlist, checkedIn, cancelled
    private long createdAt;

    public Booking() {
    }

    public Booking(String id, String sessionId, String employeeId, String userName, String status) {
        this.id = id;
        this.sessionId = sessionId;
        this.employeeId = employeeId;
        this.userName = userName;
        this.status = status;
        this.createdAt = System.currentTimeMillis();
    }

    public Booking(String id, String sessionId, String employeeId, String userName, String phone, String status) {
        this(id, sessionId, employeeId, userName, status);
        this.phone = phone;
    }

    public boolean isActive() {
        return STATUS_BOOKED.equals(status) || STATUS_WAITLIST.equals(status) || STATUS_CHECKED_IN.equals(status);
    }

    public boolean isCancelled() {
        return STATUS_CANCELLED.equals(status);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
