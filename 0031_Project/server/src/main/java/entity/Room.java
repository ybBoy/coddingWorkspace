package entity;

public class Room {
    private String id;
    private String roomNo;
    private int floor;
    private RoomStatus status;
    private String type;
    private StayRecord currentStay;
    private boolean overdue;
    private double defaultPrice;

    public Room() {
    }

    public Room(String id, String roomNo, int floor, RoomStatus status, String type, double defaultPrice) {
        this.id = id;
        this.roomNo = roomNo;
        this.floor = floor;
        this.status = status;
        this.type = type;
        this.defaultPrice = defaultPrice;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public StayRecord getCurrentStay() {
        return currentStay;
    }

    public void setCurrentStay(StayRecord currentStay) {
        this.currentStay = currentStay;
    }

    public boolean isOverdue() {
        return overdue;
    }

    public void setOverdue(boolean overdue) {
        this.overdue = overdue;
    }

    public double getDefaultPrice() {
        return defaultPrice;
    }

    public void setDefaultPrice(double defaultPrice) {
        this.defaultPrice = defaultPrice;
    }
}
