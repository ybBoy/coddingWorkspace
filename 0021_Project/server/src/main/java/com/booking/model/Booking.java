package com.booking.model;

/**
 * Booking 预约记录实体类
 * 职责：表示一条预约记录，包含用户、场次、状态等信息
 * 状态：booked（已预约）、waitlist（候补中）、checkedIn（已签到）、cancelled（已取消）
 */
public class Booking {
    private String id;
    private String sessionId;
    private String userName;
    private String status; // booked, waitlist, checkedIn, cancelled
    private long createdAt;

    public Booking() {
    }

    public Booking(String id, String sessionId, String userName, String status) {
        this.id = id;
        this.sessionId = sessionId;
        this.userName = userName;
        this.status = status;
        this.createdAt = System.currentTimeMillis();
    }

    public boolean isActive() {
        return "booked".equals(status) || "waitlist".equals(status) || "checkedIn".equals(status);
    }

    public boolean isCancelled() {
        return "cancelled".equals(status);
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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
