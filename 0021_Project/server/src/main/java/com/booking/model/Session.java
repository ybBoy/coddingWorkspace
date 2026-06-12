package com.booking.model;

/**
 * Session 活动场次实体类
 * 职责：表示一个活动场次的基本信息和实时统计数据
 * 包含：场次 ID、名称、起止时间、容量、已预约数、已签到数、候补数
 */
public class Session {
    private String id;
    private String name;
    private String startTime;
    private String endTime;
    private int capacity;
    private int bookedCount;
    private int checkedInCount;
    private int waitlistCount;

    public Session() {
    }

    public Session(String id, String name, String startTime, String endTime, int capacity) {
        this.id = id;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.bookedCount = 0;
        this.checkedInCount = 0;
        this.waitlistCount = 0;
    }

    // 是否还有名额
    public boolean hasAvailableSpot() {
        return bookedCount < capacity;
    }

    // 剩余名额
    public int getRemainingSpots() {
        return Math.max(0, capacity - bookedCount);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getBookedCount() {
        return bookedCount;
    }

    public void setBookedCount(int bookedCount) {
        this.bookedCount = bookedCount;
    }

    public int getCheckedInCount() {
        return checkedInCount;
    }

    public void setCheckedInCount(int checkedInCount) {
        this.checkedInCount = checkedInCount;
    }

    public int getWaitlistCount() {
        return waitlistCount;
    }

    public void setWaitlistCount(int waitlistCount) {
        this.waitlistCount = waitlistCount;
    }
}
