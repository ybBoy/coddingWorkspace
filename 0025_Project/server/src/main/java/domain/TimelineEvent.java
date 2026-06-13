package domain;

public class TimelineEvent {
    public enum EventType {
        JOIN, LEAVE, NOTE_ADDED, REPLY_ADDED, LIKE, HIGHLIGHT, PARAGRAPH_SWITCH,
        DISCUSSION_QUEUE_UPDATED, TYPING_START, TYPING_END, ARTICLE_UPDATED
    }

    private String id;
    private long timestamp;
    private EventType type;
    private String userName;
    private String roomId;
    private Object data;

    public TimelineEvent() {}

    public TimelineEvent(EventType type, String userName, Object data) {
        this.id = "evt_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.userName = userName;
        this.data = data;
    }

    public TimelineEvent(String id, EventType type, String userName, String roomId, Object data) {
        this.id = id;
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.userName = userName;
        this.roomId = roomId;
        this.data = data;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}
