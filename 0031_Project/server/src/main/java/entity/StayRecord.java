package entity;

public class StayRecord {
    private String id;
    private String roomId;
    private String guestName;
    private long checkInTime;
    private long expectedCheckOutTime;
    private Long actualCheckOutTime;

    public StayRecord() {
    }

    public StayRecord(String id, String roomId, String guestName, long checkInTime, long expectedCheckOutTime) {
        this.id = id;
        this.roomId = roomId;
        this.guestName = guestName;
        this.checkInTime = checkInTime;
        this.expectedCheckOutTime = expectedCheckOutTime;
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

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public long getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(long checkInTime) {
        this.checkInTime = checkInTime;
    }

    public long getExpectedCheckOutTime() {
        return expectedCheckOutTime;
    }

    public void setExpectedCheckOutTime(long expectedCheckOutTime) {
        this.expectedCheckOutTime = expectedCheckOutTime;
    }

    public Long getActualCheckOutTime() {
        return actualCheckOutTime;
    }

    public void setActualCheckOutTime(Long actualCheckOutTime) {
        this.actualCheckOutTime = actualCheckOutTime;
    }
}
