package domain;

public class Presence {
    private String userName;
    private String roomId;
    private String paragraphId;
    private boolean typing;
    private long typingSince;
    private long joinedAt;
    private long lastActiveAt;
    private boolean isOwner;
    private boolean isModerator;

    public Presence() {
        this.joinedAt = System.currentTimeMillis();
        this.lastActiveAt = System.currentTimeMillis();
    }

    public Presence(String userName, String roomId) {
        this.userName = userName;
        this.roomId = roomId;
        this.joinedAt = System.currentTimeMillis();
        this.lastActiveAt = System.currentTimeMillis();
        this.typing = false;
        this.isOwner = false;
        this.isModerator = false;
    }

    public Presence(String userName, String roomId, String paragraphId) {
        this.userName = userName;
        this.roomId = roomId;
        this.paragraphId = paragraphId;
        this.joinedAt = System.currentTimeMillis();
        this.lastActiveAt = System.currentTimeMillis();
        this.typing = false;
        this.isOwner = false;
        this.isModerator = false;
    }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getParagraphId() { return paragraphId; }
    public void setParagraphId(String paragraphId) { this.paragraphId = paragraphId; }

    public boolean isTyping() { return typing; }
    public void setTyping(boolean typing) {
        this.typing = typing;
        if (typing) this.typingSince = System.currentTimeMillis();
        this.lastActiveAt = System.currentTimeMillis();
    }

    public long getTypingSince() { return typingSince; }
    public void setTypingSince(long typingSince) { this.typingSince = typingSince; }

    public long getJoinedAt() { return joinedAt; }
    public void setJoinedAt(long joinedAt) { this.joinedAt = joinedAt; }

    public long getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(long lastActiveAt) { this.lastActiveAt = lastActiveAt; }

    public boolean isOwner() { return isOwner; }
    public void setOwner(boolean owner) { isOwner = owner; }

    public boolean isModerator() { return isModerator; }
    public void setModerator(boolean moderator) { isModerator = moderator; }

    public void touch() {
        this.lastActiveAt = System.currentTimeMillis();
    }
}
