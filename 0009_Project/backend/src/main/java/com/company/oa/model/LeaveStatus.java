package com.company.oa.model;

public enum LeaveStatus {
    PENDING("待审批"),
    APPROVED("已通过"),
    REJECTED("已拒绝");

    private String description;

    LeaveStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}