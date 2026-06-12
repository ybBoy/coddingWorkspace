/**
 * QueueState 队列状态类
 * 职责：封装当前完整的队列状态，用于后端广播给所有前端
 * 迭代新增：missedQueue 过号列表、todayStats 今日统计
 */
package com.queue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class QueueState implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Ticket> waitingQueue;

    private List<Ticket> missedQueue;

    private List<Counter> counters;

    private Ticket currentCalling;

    private List<CallRecord> callRecords;

    private TodayStats todayStats;

    private int nextNumber;

    private String todayDate;

    public QueueState() {
        this.waitingQueue = new ArrayList<>();
        this.missedQueue = new ArrayList<>();
        this.counters = new ArrayList<>();
        this.callRecords = new ArrayList<>();
        this.todayStats = new TodayStats();
        this.nextNumber = 1;
        this.todayDate = "";
    }

    public List<Ticket> getWaitingQueue() {
        return waitingQueue;
    }

    public void setWaitingQueue(List<Ticket> waitingQueue) {
        this.waitingQueue = waitingQueue;
    }

    public List<Ticket> getMissedQueue() {
        return missedQueue;
    }

    public void setMissedQueue(List<Ticket> missedQueue) {
        this.missedQueue = missedQueue;
    }

    public List<Counter> getCounters() {
        return counters;
    }

    public void setCounters(List<Counter> counters) {
        this.counters = counters;
    }

    public Ticket getCurrentCalling() {
        return currentCalling;
    }

    public void setCurrentCalling(Ticket currentCalling) {
        this.currentCalling = currentCalling;
    }

    public List<CallRecord> getCallRecords() {
        return callRecords;
    }

    public void setCallRecords(List<CallRecord> callRecords) {
        this.callRecords = callRecords;
    }

    public TodayStats getTodayStats() {
        return todayStats;
    }

    public void setTodayStats(TodayStats todayStats) {
        this.todayStats = todayStats;
    }

    public int getNextNumber() {
        return nextNumber;
    }

    public void setNextNumber(int nextNumber) {
        this.nextNumber = nextNumber;
    }

    public String getTodayDate() {
        return todayDate;
    }

    public void setTodayDate(String todayDate) {
        this.todayDate = todayDate;
    }
}
