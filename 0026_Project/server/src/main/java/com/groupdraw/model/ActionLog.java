package com.groupdraw.model;

import java.util.List;

public class ActionLog {
    private long timestamp;
    private String action;
    private String description;
    private List<Group> groupsSnapshot;

    public ActionLog() {
    }

    public ActionLog(String action, String description, List<Group> groupsSnapshot) {
        this.timestamp = System.currentTimeMillis();
        this.action = action;
        this.description = description;
        this.groupsSnapshot = groupsSnapshot;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Group> getGroupsSnapshot() {
        return groupsSnapshot;
    }

    public void setGroupsSnapshot(List<Group> groupsSnapshot) {
        this.groupsSnapshot = groupsSnapshot;
    }
}
