package com.booking.model;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Session 活动场次实体类
 * 职责：表示一个活动场次的基本信息和实时统计数据
 * 包含：场次 ID、名称、日期、起止时间、容量、状态、创建人
 * date 字段格式：yyyy-MM-dd（如 2026-06-12），用于按当天过滤
 * status: active（开放预约）、closed（关闭预约）
 */
public class Session {
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_CLOSED = "closed";

    private String id;
    private String name;
    private String date;        // 场次日期 yyyy-MM-dd
    private String startTime;
    private String endTime;
    private int capacity;
    private String status;      // active / closed
    private String createdBy;   // 创建人（工号或姓名）
    private long createdAt;
    private int bookedCount;
    private int checkedInCount;
    private int waitlistCount;

    public Session() {
        this.createdAt = System.currentTimeMillis();
        this.status = STATUS_ACTIVE;
    }

    public Session(String id, String name, String date, String startTime, String endTime, int capacity) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.status = STATUS_ACTIVE;
        this.createdAt = System.currentTimeMillis();
        this.bookedCount = 0;
        this.checkedInCount = 0;
        this.waitlistCount = 0;
    }

    public Session(String id, String name, String date, String startTime, String endTime, int capacity, String createdBy) {
        this(id, name, date, startTime, endTime, capacity);
        this.createdBy = createdBy;
    }

    // 判断是否是今天的场次
    public boolean isToday() {
        if (date == null) return true;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(new Date());
        return date.equals(today);
    }

    // 获取今天日期字符串（工具方法）
    public static String todayString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date());
    }

    // 是否还有名额
    public boolean hasAvailableSpot() {
        return bookedCount < capacity;
    }

    // 剩余名额
    public int getRemainingSpots() {
        return Math.max(0, capacity - bookedCount);
    }

    // 是否开放预约
    public boolean isOpenForBooking() {
        return STATUS_ACTIVE.equals(status);
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
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
