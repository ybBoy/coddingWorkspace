package entity;

public class AlertItem {
    private String id;
    private String type;
    private String roomId;
    private String roomNo;
    private String message;
    private long timestamp;
    private long triggerTime;

    public AlertItem() {
    }

    public AlertItem(String id, String type, String roomId, String roomNo, String message, long triggerTime) {
        this.id = id;
        this.type = type;
        this.roomId = roomId;
        this.roomNo = roomNo;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.triggerTime = triggerTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(long triggerTime) {
        this.triggerTime = triggerTime;
    }
}
