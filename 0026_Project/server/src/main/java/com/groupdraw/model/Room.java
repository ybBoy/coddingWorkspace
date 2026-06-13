package com.groupdraw.model;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private String code;
    private String activityName;
    private int groupCount;
    private String hostToken;
    private List<Participant> participants;
    private List<Group> groups;
    private List<ActionLog> actionLogs;
    private List<GroupRule> rules;

    public Room() {
        this.participants = new ArrayList<Participant>();
        this.groups = new ArrayList<Group>();
        this.actionLogs = new ArrayList<ActionLog>();
        this.rules = new ArrayList<GroupRule>();
    }

    public Room(String code, String activityName, String hostToken) {
        this.code = code;
        this.activityName = activityName;
        this.groupCount = 4;
        this.hostToken = hostToken;
        this.participants = new ArrayList<Participant>();
        this.groups = new ArrayList<Group>();
        this.actionLogs = new ArrayList<ActionLog>();
        this.rules = new ArrayList<GroupRule>();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public int getGroupCount() {
        return groupCount;
    }

    public void setGroupCount(int groupCount) {
        this.groupCount = groupCount;
    }

    public String getHostToken() {
        return hostToken;
    }

    public void setHostToken(String hostToken) {
        this.hostToken = hostToken;
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<Participant> participants) {
        this.participants = participants;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public List<ActionLog> getActionLogs() {
        return actionLogs;
    }

    public void setActionLogs(List<ActionLog> actionLogs) {
        this.actionLogs = actionLogs;
    }

    public List<GroupRule> getRules() {
        return rules;
    }

    public void setRules(List<GroupRule> rules) {
        this.rules = rules;
    }
}
