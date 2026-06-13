package entity;

public class RoomLog {
    private String id;
    private String roomId;
    private String roomNo;
    private String action;
    private String operator;
    private long timestamp;
    private String remark;

    public RoomLog() {
    }

    public RoomLog(String id, String roomId, String roomNo, String action, String operator, long timestamp) {
        this.id = id;
        this.roomId = roomId;
        this.roomNo = roomNo;
        this.action = action;
        this.operator = operator;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
