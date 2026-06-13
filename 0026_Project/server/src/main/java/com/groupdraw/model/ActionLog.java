package com.groupdraw.model;

import java.util.ArrayList;
import java.util.List;

public class ActionLog {
    private long timestamp;
    private String action;
    private String description;
    private List<Group> groupsSnapshot;
    private String operatorId;
    private String operatorName;
    private String operatorType;
    private List<String> affectedParticipantIds;

    public ActionLog() {
        this.affectedParticipantIds = new ArrayList<String>();
    }

    public ActionLog(String action, String description, List<Group> groupsSnapshot) {
        this.timestamp = System.currentTimeMillis();
        this.action = action;
        this.description = description;
        this.groupsSnapshot = groupsSnapshot;
        this.affectedParticipantIds = new ArrayList<String>();
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

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getOperatorType() {
        return operatorType;
    }

    public void setOperatorType(String operatorType) {
        this.operatorType = operatorType;
    }

    public List<String> getAffectedParticipantIds() {
        return affectedParticipantIds;
    }

    public void setAffectedParticipantIds(List<String> affectedParticipantIds) {
        this.affectedParticipantIds = affectedParticipantIds;
    }
}
