package com.studyroom.model;

public class SeatAction {
    private int seatId;
    private String action;
    private String nickname;
    private long timestamp;

    public SeatAction() {}

    public SeatAction(int seatId, String action, String nickname) {
        this.seatId = seatId;
        this.action = action;
        this.nickname = nickname;
        this.timestamp = System.currentTimeMillis();
    }

    public int getSeatId() { return seatId; }
    public void setSeatId(int seatId) { this.seatId = seatId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
