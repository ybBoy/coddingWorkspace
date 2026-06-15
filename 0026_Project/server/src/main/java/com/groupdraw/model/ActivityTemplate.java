package com.groupdraw.model;

import java.util.List;

public class ActivityTemplate {
    private String id;
    private String name;
    private String activityName;
    private int groupCount;
    private List<GroupRule> rules;
    private List<String> customFields;
    private long createdAt;

    public ActivityTemplate() {
    }

    public ActivityTemplate(String id, String name, String activityName, int groupCount, List<GroupRule> rules, List<String> customFields, long createdAt) {
        this.id = id;
        this.name = name;
        this.activityName = activityName;
        this.groupCount = groupCount;
        this.rules = rules;
        this.customFields = customFields;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<GroupRule> getRules() {
        return rules;
    }

    public void setRules(List<GroupRule> rules) {
        this.rules = rules;
    }

    public List<String> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(List<String> customFields) {
        this.customFields = customFields;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
