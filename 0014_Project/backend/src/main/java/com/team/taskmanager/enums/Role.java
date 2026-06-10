package com.team.taskmanager.enums;

public enum Role {
    LEADER("组长"),
    MEMBER("普通成员");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
