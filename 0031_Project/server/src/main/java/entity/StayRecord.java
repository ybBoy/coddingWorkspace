package entity;

public class StayRecord {
    private String id;
    private String roomId;
    private String guestName;
    private long checkInTime;
    private long expectedCheckOutTime;
    private Long actualCheckOutTime;
    private double price;
    private double deposit;
    private boolean settled;
    private String checkInOperator;
    private String checkOutOperator;

    public StayRecord() {
    }

    public StayRecord(String id, String roomId, String guestName, long checkInTime, long expectedCheckOutTime,
                      double price, double deposit, String checkInOperator) {
        this.id = id;
        this.roomId = roomId;
        this.guestName = guestName;
        this.checkInTime = checkInTime;
        this.expectedCheckOutTime = expectedCheckOutTime;
        this.price = price;
        this.deposit = deposit;
        this.settled = false;
        this.checkInOperator = checkInOperator;
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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getDeposit() {
        return deposit;
    }

    public void setDeposit(double deposit) {
        this.deposit = deposit;
    }

    public boolean isSettled() {
        return settled;
    }

    public void setSettled(boolean settled) {
        this.settled = settled;
    }

    public String getCheckInOperator() {
        return checkInOperator;
    }

    public void setCheckInOperator(String checkInOperator) {
        this.checkInOperator = checkInOperator;
    }

    public String getCheckOutOperator() {
        return checkOutOperator;
    }

    public void setCheckOutOperator(String checkOutOperator) {
        this.checkOutOperator = checkOutOperator;
    }
}
