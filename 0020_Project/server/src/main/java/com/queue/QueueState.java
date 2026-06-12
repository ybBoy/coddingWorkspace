/**
 * QueueState 队列状态类
 * 职责：封装完整的队列状态，用于序列化后广播给所有前端
 * 包含：等待队列、窗口列表、当前叫号、叫号记录、下一个号码
 */
package com.queue;

import java.io.Serializable;
import java.util.List;

public class QueueState implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Ticket> waitingQueue;

    private List<Counter> counters;

    private Ticket currentCalling;

    private List<CallRecord> callRecords;

    private int nextNumber;

    public QueueState() {
    }

    public QueueState(List<Ticket> waitingQueue, List<Counter> counters,
                      Ticket currentCalling, List<CallRecord> callRecords, int nextNumber) {
        this.waitingQueue = waitingQueue;
        this.counters = counters;
        this.currentCalling = currentCalling;
        this.callRecords = callRecords;
        this.nextNumber = nextNumber;
    }

    public List<Ticket> getWaitingQueue() {
        return waitingQueue;
    }

    public void setWaitingQueue(List<Ticket> waitingQueue) {
        this.waitingQueue = waitingQueue;
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

    public int getNextNumber() {
        return nextNumber;
    }

    public void setNextNumber(int nextNumber) {
        this.nextNumber = nextNumber;
    }
}
