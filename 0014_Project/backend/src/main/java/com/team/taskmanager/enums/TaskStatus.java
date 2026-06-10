package com.team.taskmanager.enums;

public enum TaskStatus {
    PENDING("未开始"),
    IN_PROGRESS("进行中"),
    COMPLETED("已完成");

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
