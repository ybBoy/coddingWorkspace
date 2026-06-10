package com.team.taskmanager.enums;

public enum Priority {
    HIGH("高"),
    MEDIUM("中"),
    LOW("低");

    private final String displayName;

    Priority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
