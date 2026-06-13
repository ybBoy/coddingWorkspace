package com.studyroom.model;

public class Seat {
    private int id;
    private int row;
    private int col;
    private String status;
    private String nickname;
    private long awaySince;

    public Seat() {}

    public Seat(int id, int row, int col) {
        this.id = id;
        this.row = row;
        this.col = col;
        this.status = "free";
        this.nickname = null;
        this.awaySince = 0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }

    public int getCol() { return col; }
    public void setCol(int col) { this.col = col; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public long getAwaySince() { return awaySince; }
    public void setAwaySince(long awaySince) { this.awaySince = awaySince; }

    public boolean isReleasable() {
        if ("away".equals(status) && awaySince > 0) {
            long elapsed = System.currentTimeMillis() - awaySince;
            return elapsed >= 15 * 60 * 1000;
        }
        return false;
    }
}
