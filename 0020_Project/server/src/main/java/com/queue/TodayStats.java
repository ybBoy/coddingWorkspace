/**
 * TodayStats 今日统计实体类
 * 职责：计算并保存今日的排队业务统计数据，实时更新并推送到前端
 * 迭代新增：用于今日统计面板展示
 */
package com.queue;

import java.io.Serializable;

public class TodayStats implements Serializable {

    private static final long serialVersionUID = 1L;

    private int totalTaken;

    private int waiting;

    private int inProgress;

    private int completed;

    private int missed;

    private long avgWaitSeconds;

    public TodayStats() {
    }

    public TodayStats(int totalTaken, int waiting, int inProgress, int completed, int missed, long avgWaitSeconds) {
        this.totalTaken = totalTaken;
        this.waiting = waiting;
        this.inProgress = inProgress;
        this.completed = completed;
        this.missed = missed;
        this.avgWaitSeconds = avgWaitSeconds;
    }

    public int getTotalTaken() {
        return totalTaken;
    }

    public void setTotalTaken(int totalTaken) {
        this.totalTaken = totalTaken;
    }

    public int getWaiting() {
        return waiting;
    }

    public void setWaiting(int waiting) {
        this.waiting = waiting;
    }

    public int getInProgress() {
        return inProgress;
    }

    public void setInProgress(int inProgress) {
        this.inProgress = inProgress;
    }

    public int getCompleted() {
        return completed;
    }

    public void setCompleted(int completed) {
        this.completed = completed;
    }

    public int getMissed() {
        return missed;
    }

    public void setMissed(int missed) {
        this.missed = missed;
    }

    public long getAvgWaitSeconds() {
        return avgWaitSeconds;
    }

    public void setAvgWaitSeconds(long avgWaitSeconds) {
        this.avgWaitSeconds = avgWaitSeconds;
    }
}
