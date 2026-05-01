package com.scenic.entity;

import java.time.LocalDateTime;

public class GuidanceMessage {
    private Long id;
    private String title;
    private String content;
    private MessageType type;
    private MessageSource source;
    private String targetDisplay;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private boolean active;

    public enum MessageType {
        CROWD_WARNING,
        GUIDANCE_SUGGESTION,
        EMERGENCY,
        INFO
    }

    public enum MessageSource {
        AUTO,
        MANUAL
    }

    public GuidanceMessage() {
        this.createTime = LocalDateTime.now();
        this.active = true;
    }

    public GuidanceMessage(Long id, String title, String content, MessageType type,
                           MessageSource source, String targetDisplay, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.type = type;
        this.source = source;
        this.targetDisplay = targetDisplay;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createTime = LocalDateTime.now();
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public MessageSource getSource() {
        return source;
    }

    public void setSource(MessageSource source) {
        this.source = source;
    }

    public String getTargetDisplay() {
        return targetDisplay;
    }

    public void setTargetDisplay(String targetDisplay) {
        this.targetDisplay = targetDisplay;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
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