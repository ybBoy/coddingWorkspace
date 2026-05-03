package com.company.oa.model;

public enum EmployeeStatus {
    NORMAL("正常"),
    ON_LEAVE("请假中");

    private String description;

    EmployeeStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}