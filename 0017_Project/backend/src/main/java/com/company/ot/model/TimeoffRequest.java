package com.company.ot.model;

import java.time.LocalDate;

public class TimeoffRequest {
    private Long id;
    private Long userId;
    private String userName;
    private Department department;
    private LocalDate timeoffDate;
    private TimeoffType timeoffType;
    private double hours;
    private String reason;
    private RequestStatus status;
    private String approvalComment;
    private LocalDate createTime;

    public TimeoffRequest() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public LocalDate getTimeoffDate() {
        return timeoffDate;
    }

    public void setTimeoffDate(LocalDate timeoffDate) {
        this.timeoffDate = timeoffDate;
    }

    public TimeoffType getTimeoffType() {
        return timeoffType;
    }

    public void setTimeoffType(TimeoffType timeoffType) {
        this.timeoffType = timeoffType;
    }

    public double getHours() {
        return hours;
    }

    public void setHours(double hours) {
        this.hours = hours;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getApprovalComment() {
        return approvalComment;
    }

    public void setApprovalComment(String approvalComment) {
        this.approvalComment = approvalComment;
    }

    public LocalDate getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDate createTime) {
        this.createTime = createTime;
    }
}
