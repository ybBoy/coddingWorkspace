package com.danmaku;

public class Message {
    private String id;
    private String content;
    private String nickname;
    private long timestamp;
    private String status;
    private boolean sensitive;
    private String color;

    public Message() {
    }

    public Message(String id, String content, String nickname, long timestamp, String status, boolean sensitive, String color) {
        this.id = id;
        this.content = content;
        this.nickname = nickname;
        this.timestamp = timestamp;
        this.status = status;
        this.sensitive = sensitive;
        this.color = color;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isSensitive() { return sensitive; }
    public void setSensitive(boolean sensitive) { this.sensitive = sensitive; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
