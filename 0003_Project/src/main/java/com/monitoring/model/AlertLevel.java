package com.monitoring.model;

public enum AlertLevel {

    NORMAL("正常", 0),
    WARNING("警告", 1),
    CRITICAL("严重", 2);

    private final String displayName;
    private final int priority;

    AlertLevel(String displayName, int priority) {
        this.displayName = displayName;
        this.priority = priority;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isMoreSevereThan(AlertLevel other) {
        return this.priority > other.priority;
    }

    public static AlertLevel fromPriority(int priority) {
        for (AlertLevel level : values()) {
            if (level.priority == priority) {
                return level;
            }
        }
        return NORMAL;
    }
}
