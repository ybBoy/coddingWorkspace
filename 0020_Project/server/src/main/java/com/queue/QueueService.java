/**
 * QueueService 业务逻辑层
 * 职责：管理排队队列的核心业务逻辑，包括取号、叫号、完成、过号、重新叫号
 * 所有操作都在内存中执行，然后通知 FileStore 持久化，并通过 WebSocket 广播状态
 *
 * 数据流：
 * 接收 WebSocket 消息 -> 调用对应方法更新内存数据 -> 添加叫号记录
 *   -> 获取最新 QueueState -> 通知 WebSocket 广播给所有客户端
 */
package com.queue;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class QueueService {

    private final List<Ticket> waitingQueue;
    private final List<Counter> counters;
    private final List<CallRecord> callRecords;
    private int nextNumber;
    private Ticket currentCalling;

    public QueueService() {
        this.waitingQueue = new CopyOnWriteArrayList<>();
        this.counters = new CopyOnWriteArrayList<>();
        this.callRecords = new CopyOnWriteArrayList<>();
        this.nextNumber = 1;
        initCounters();
    }

    private void initCounters() {
        counters.add(new Counter("counter-1", "1号窗口", "idle"));
        counters.add(new Counter("counter-2", "2号窗口", "idle"));
        counters.add(new Counter("counter-3", "3号窗口", "idle"));
    }

    public void restoreState(QueueState state) {
        if (state == null) {
            return;
        }
        if (state.getWaitingQueue() != null) {
            waitingQueue.clear();
            waitingQueue.addAll(state.getWaitingQueue());
        }
        if (state.getCounters() != null && !state.getCounters().isEmpty()) {
            counters.clear();
            counters.addAll(state.getCounters());
        }
        if (state.getCallRecords() != null) {
            callRecords.clear();
            callRecords.addAll(state.getCallRecords());
        }
        if (state.getNextNumber() > 0) {
            nextNumber = state.getNextNumber();
        }
        currentCalling = state.getCurrentCalling();
        System.out.println("队列状态已恢复，等待人数: " + waitingQueue.size() +
                ", 下一个号码: " + nextNumber);
    }

    public synchronized Ticket takeTicket(String businessType) {
        Ticket ticket = new Ticket(
                UUID.randomUUID().toString(),
                nextNumber++,
                businessType,
                "waiting",
                System.currentTimeMillis()
        );
        waitingQueue.add(ticket);
        System.out.println("取号成功: " + ticket.getNumber() + " - " + businessType);
        return ticket;
    }

    public synchronized Ticket callNext(String counterId) {
        Counter counter = findCounter(counterId);
        if (counter == null || waitingQueue.isEmpty()) {
            return null;
        }
        if (counter.getCurrentTicket() != null) {
            System.out.println("窗口 " + counter.getName() + " 正在办理业务，无法叫号");
            return null;
        }

        Ticket ticket = waitingQueue.remove(0);
        ticket.setStatus("calling");
        ticket.setCalledAt(System.currentTimeMillis());
        ticket.setCounterId(counterId);

        counter.setStatus("busy");
        counter.setCurrentTicket(ticket);
        currentCalling = ticket;

        addCallRecord(ticket, counter.getName(), "called");
        System.out.println("窗口 " + counter.getName() + " 叫号: " + ticket.getNumber());
        return ticket;
    }

    public synchronized boolean completeTicket(String counterId, String ticketId) {
        Counter counter = findCounter(counterId);
        if (counter == null || counter.getCurrentTicket() == null) {
            return false;
        }
        Ticket ticket = counter.getCurrentTicket();
        if (!ticket.getId().equals(ticketId)) {
            return false;
        }

        ticket.setStatus("completed");
        addCallRecord(ticket, counter.getName(), "completed");

        counter.setStatus("idle");
        counter.setCurrentTicket(null);
        updateCurrentCalling();

        System.out.println("窗口 " + counter.getName() + " 完成: " + ticket.getNumber());
        return true;
    }

    public synchronized boolean missTicket(String counterId, String ticketId) {
        Counter counter = findCounter(counterId);
        if (counter == null || counter.getCurrentTicket() == null) {
            return false;
        }
        Ticket ticket = counter.getCurrentTicket();
        if (!ticket.getId().equals(ticketId)) {
            return false;
        }

        ticket.setStatus("missed");
        addCallRecord(ticket, counter.getName(), "missed");

        counter.setStatus("idle");
        counter.setCurrentTicket(null);
        updateCurrentCalling();

        System.out.println("窗口 " + counter.getName() + " 过号: " + ticket.getNumber());
        return true;
    }

    public synchronized boolean recallTicket(String counterId, String ticketId) {
        Counter counter = findCounter(counterId);
        if (counter == null || counter.getCurrentTicket() == null) {
            return false;
        }
        Ticket ticket = counter.getCurrentTicket();
        if (!ticket.getId().equals(ticketId)) {
            return false;
        }

        ticket.setCalledAt(System.currentTimeMillis());
        currentCalling = ticket;
        addCallRecord(ticket, counter.getName(), "recalled");

        System.out.println("窗口 " + counter.getName() + " 重新叫号: " + ticket.getNumber());
        return true;
    }

    private void updateCurrentCalling() {
        Ticket latestCalling = null;
        for (Counter c : counters) {
            if (c.getCurrentTicket() != null) {
                if (latestCalling == null ||
                        (c.getCurrentTicket().getCalledAt() != null &&
                                latestCalling.getCalledAt() != null &&
                                c.getCurrentTicket().getCalledAt() > latestCalling.getCalledAt())) {
                    latestCalling = c.getCurrentTicket();
                }
            }
        }
        currentCalling = latestCalling;
    }

    private void addCallRecord(Ticket ticket, String counterName, String action) {
        CallRecord record = new CallRecord(ticket, counterName, action, System.currentTimeMillis());
        callRecords.add(0, record);
        while (callRecords.size() > 50) {
            callRecords.remove(callRecords.size() - 1);
        }
    }

    private Counter findCounter(String counterId) {
        for (Counter counter : counters) {
            if (counter.getId().equals(counterId)) {
                return counter;
            }
        }
        return null;
    }

    public QueueState getQueueState() {
        List<Ticket> waitingCopy = new ArrayList<>(waitingQueue);
        Collections.sort(waitingCopy, new Comparator<Ticket>() {
            @Override
            public int compare(Ticket t1, Ticket t2) {
                return Long.compare(t1.getCreatedAt(), t2.getCreatedAt());
            }
        });
        return new QueueState(
                waitingCopy,
                new ArrayList<>(counters),
                currentCalling,
                new ArrayList<>(callRecords),
                nextNumber
        );
    }

    public List<Counter> getCounters() {
        return new ArrayList<>(counters);
    }
}
