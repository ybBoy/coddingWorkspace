package com.scenic.entity;

import java.time.LocalDateTime;

public class CrowdSuggestion {
    private Long id;
    private String type;
    private Long sourceAreaId;
    private String sourceAreaName;
    private Long targetAreaId;
    private String targetAreaName;
    private String message;
    private String suggestion;
    private int priority;
    private LocalDateTime createTime;
    private boolean active;

    public CrowdSuggestion() {
        this.createTime = LocalDateTime.now();
        this.active = true;
    }

    public CrowdSuggestion(Long id, String type, Long sourceAreaId, String sourceAreaName,
                           Long targetAreaId, String targetAreaName, String message, String suggestion, int priority) {
        this.id = id;
        this.type = type;
        this.sourceAreaId = sourceAreaId;
        this.sourceAreaName = sourceAreaName;
        this.targetAreaId = targetAreaId;
        this.targetAreaName = targetAreaName;
        this.message = message;
        this.suggestion = suggestion;
        this.priority = priority;
        this.createTime = LocalDateTime.now();
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getSourceAreaId() {
        return sourceAreaId;
    }

    public void setSourceAreaId(Long sourceAreaId) {
        this.sourceAreaId = sourceAreaId;
    }

    public String getSourceAreaName() {
        return sourceAreaName;
    }

    public void setSourceAreaName(String sourceAreaName) {
        this.sourceAreaName = sourceAreaName;
    }

    public Long getTargetAreaId() {
        return targetAreaId;
    }

    public void setTargetAreaId(Long targetAreaId) {
        this.targetAreaId = targetAreaId;
    }

    public String getTargetAreaName() {
        return targetAreaName;
    }

    public void setTargetAreaName(String targetAreaName) {
        this.targetAreaName = targetAreaName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}