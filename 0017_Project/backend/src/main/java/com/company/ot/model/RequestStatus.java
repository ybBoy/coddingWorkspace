package com.company.ot.model;

public enum RequestStatus {
    PENDING("待审批"),
    APPROVED("已通过"),
    REJECTED("已拒绝");

    private final String label;

    RequestStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
