/**
 * QueueService 业务逻辑层
 * 职责：管理排队队列的核心业务逻辑，包括取号、叫号、完成、过号、过号管理、窗口配置、统计计算
 * 所有操作都在内存中执行，然后通知 FileStore 持久化，并通过 WebSocket 广播状态
 *
 * 迭代新增：
 * 1. 过号列表管理（missedQueue）：支持重新入队、直接重叫、标记结束
 * 2. 按业务类型筛选叫号：支持指定业务类型或全部业务
 * 3. 窗口配置 CRUD：新增窗口、修改名称和业务类型、启用/停用
 * 4. 今日统计：按日期实时计算取号总数、等待、办理中、完成、过号、平均等待时长
 * 5. 跨天重置：日期变更时重置号码计数和统计数据
 *
 * 线程安全：所有读写方法都加了 synchronized 锁
 *
 * 数据流：
 * 接收 WebSocket 消息 -> 调用对应方法更新内存数据 -> 添加叫号记录 -> 计算今日统计
 *   -> 获取最新 QueueState（深拷贝） -> 通知 WebSocket 广播给所有客户端
 */
package com.queue;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class QueueService {

    private final List<Ticket> waitingQueue;
    private final List<Ticket> missedQueue;
    private final List<Counter> counters;
    private final List<CallRecord> callRecords;
    private int nextNumber;
    private Ticket currentCalling;
    private String todayDate;

    public QueueService() {
        this.waitingQueue = new CopyOnWriteArrayList<>();
        this.missedQueue = new CopyOnWriteArrayList<>();
        this.counters = new CopyOnWriteArrayList<>();
        this.callRecords = new CopyOnWriteArrayList<>();
        this.nextNumber = 1;
        this.todayDate = getTodayDateStr();
    }

    public void setCounters(List<Counter> initialCounters) {
        if (initialCounters != null && !initialCounters.isEmpty()) {
            counters.clear();
            counters.addAll(initialCounters);
        }
    }

    private String getTodayDateStr() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date());
    }

    private long getStartOfTodayMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private boolean isToday(long timestamp) {
        return timestamp >= getStartOfTodayMillis();
    }

    /**
     * 检查日期是否变更，跨天则重置号码计数
     */
    private void checkDayChange() {
        String newDate = getTodayDateStr();
        if (!newDate.equals(todayDate)) {
            System.out.println("检测到日期变更: " + todayDate + " -> " + newDate + "，重置号码计数");
            todayDate = newDate;
            nextNumber = 1;
        }
    }

    public synchronized void restoreState(QueueState state) {
        checkDayChange();
        if (state == null) {
            return;
        }

        String savedDate = state.getTodayDate();
        boolean dateChanged = (savedDate == null || !savedDate.equals(todayDate));

        if (state.getWaitingQueue() != null) {
            waitingQueue.clear();
            if (!dateChanged) {
                waitingQueue.addAll(state.getWaitingQueue());
            } else {
                System.out.println("跨天：清空等待队列，共丢弃 " + state.getWaitingQueue().size() + " 个号");
            }
        }
        if (state.getMissedQueue() != null) {
            missedQueue.clear();
            if (!dateChanged) {
                missedQueue.addAll(state.getMissedQueue());
            } else {
                System.out.println("跨天：清空过号队列，共丢弃 " + state.getMissedQueue().size() + " 个号");
            }
        }
        if (state.getCounters() != null && !state.getCounters().isEmpty()) {
            counters.clear();
            for (Counter savedCounter : state.getCounters()) {
                Counter counterCopy = new Counter(savedCounter);
                if (dateChanged && counterCopy.getCurrentTicket() != null) {
                    System.out.println("跨天：清除窗口 " + counterCopy.getName() + " 的办理中号票");
                    counterCopy.setCurrentTicket(null);
                    counterCopy.setStatus("idle");
                }
                counters.add(counterCopy);
            }
        }
        if (state.getCallRecords() != null) {
            callRecords.clear();
            long startOfToday = getStartOfTodayMillis();
            for (CallRecord r : state.getCallRecords()) {
                if (r.getTimestamp() >= startOfToday) {
                    callRecords.add(r);
                }
            }
            System.out.println("恢复今日叫号记录: " + callRecords.size() + " 条");
        }
        if (!dateChanged && state.getNextNumber() > 0) {
            nextNumber = state.getNextNumber();
        } else {
            nextNumber = 1;
        }
        updateCurrentCalling();
        System.out.println("队列状态已恢复，等待: " + waitingQueue.size() +
                ", 过号: " + missedQueue.size() +
                ", 下一个号码: " + nextNumber +
                ", 今日日期: " + todayDate);
    }

    public synchronized Ticket takeTicket(String businessType) {
        checkDayChange();
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
        return callNextByType(counterId, null);
    }

    public synchronized Ticket callNextByType(String counterId, String businessType) {
        checkDayChange();
        Counter counter = findEnabledCounter(counterId);
        if (counter == null) {
            return null;
        }
        if (counter.getCurrentTicket() != null) {
            System.out.println("窗口 " + counter.getName() + " 正在办理业务，无法叫号");
            return null;
        }

        Ticket ticket = findNextTicket(businessType, counter.getSupportedBusinessTypes());
        if (ticket == null) {
            System.out.println("没有等待的号票" + (businessType != null ? "（业务类型：" + businessType + "）" : ""));
            return null;
        }

        waitingQueue.remove(ticket);
        ticket.setStatus("calling");
        ticket.setCalledAt(System.currentTimeMillis());
        ticket.setCounterId(counterId);

        counter.setStatus("busy");
        counter.setCurrentTicket(ticket);
        currentCalling = ticket;

        addCallRecord(ticket, counter.getName(), "called");
        System.out.println("窗口 " + counter.getName() + " 叫号: " + ticket.getNumber() +
                (businessType != null ? "（" + businessType + "）" : ""));
        return ticket;
    }

    private Ticket findNextTicket(String requestedType, List<String> counterSupportedTypes) {
        if (waitingQueue.isEmpty()) {
            return null;
        }

        List<Ticket> sortedQueue = new ArrayList<>(waitingQueue);
        Collections.sort(sortedQueue, new Comparator<Ticket>() {
            @Override
            public int compare(Ticket t1, Ticket t2) {
                return Long.compare(t1.getCreatedAt(), t2.getCreatedAt());
            }
        });

        if (requestedType != null && !"all".equals(requestedType)) {
            for (Ticket t : sortedQueue) {
                if (requestedType.equals(t.getBusinessType())) {
                    return t;
                }
            }
            return null;
        }

        if (counterSupportedTypes != null && !counterSupportedTypes.isEmpty()) {
            Set<String> supportedSet = new HashSet<>(counterSupportedTypes);
            for (Ticket t : sortedQueue) {
                if (supportedSet.contains(t.getBusinessType())) {
                    return t;
                }
            }
            return null;
        }

        return sortedQueue.get(0);
    }

    public synchronized boolean completeTicket(String counterId, String ticketId) {
        checkDayChange();
        Counter counter = findEnabledCounter(counterId);
        if (counter == null || counter.getCurrentTicket() == null) {
            return false;
        }
        Ticket ticket = counter.getCurrentTicket();
        if (ticketId != null && !ticket.getId().equals(ticketId)) {
            return false;
        }

        ticket.setStatus("completed");
        ticket.setCompletedAt(System.currentTimeMillis());

        addCallRecord(ticket, counter.getName(), "completed");

        counter.setStatus("idle");
        counter.setCurrentTicket(null);
        updateCurrentCalling();

        System.out.println("窗口 " + counter.getName() + " 完成: " + ticket.getNumber());
        return true;
    }

    public synchronized boolean missTicket(String counterId, String ticketId) {
        checkDayChange();
        Counter counter = findEnabledCounter(counterId);
        if (counter == null || counter.getCurrentTicket() == null) {
            return false;
        }
        Ticket ticket = counter.getCurrentTicket();
        if (ticketId != null && !ticket.getId().equals(ticketId)) {
            return false;
        }

        ticket.setStatus("missed");
        missedQueue.add(ticket);
        addCallRecord(ticket, counter.getName(), "missed");

        counter.setStatus("idle");
        counter.setCurrentTicket(null);
        updateCurrentCalling();

        System.out.println("窗口 " + counter.getName() + " 过号: " + ticket.getNumber());
        return true;
    }

    public synchronized boolean recallTicket(String counterId, String ticketId) {
        checkDayChange();
        Counter counter = findEnabledCounter(counterId);
        if (counter == null) {
            return false;
        }

        Ticket ticket = findTicketById(ticketId);
        if (ticket == null) {
            return false;
        }

        if (counter.getCurrentTicket() != null && !counter.getCurrentTicket().getId().equals(ticketId)) {
            return false;
        }

        if ("missed".equals(ticket.getStatus())) {
            missedQueue.remove(ticket);
        }

        ticket.setStatus("calling");
        ticket.setCalledAt(System.currentTimeMillis());
        ticket.setCounterId(counterId);
        counter.setStatus("busy");
        counter.setCurrentTicket(ticket);
        currentCalling = ticket;

        addCallRecord(ticket, counter.getName(), "recalled");

        System.out.println("窗口 " + counter.getName() + " 重新叫号: " + ticket.getNumber());
        return true;
    }

    public synchronized boolean requeueMissed(String ticketId) {
        checkDayChange();
        Ticket ticket = findMissedTicket(ticketId);
        if (ticket == null) {
            return false;
        }

        missedQueue.remove(ticket);
        ticket.setStatus("waiting");
        ticket.setCalledAt(null);
        ticket.setCounterId(null);
        waitingQueue.add(ticket);

        addCallRecord(ticket, "系统", "requeue");
        System.out.println("过号 " + ticket.getNumber() + " 已重新加入等待队列");
        return true;
    }

    public synchronized boolean finishMissed(String ticketId) {
        checkDayChange();
        Ticket ticket = findMissedTicket(ticketId);
        if (ticket == null) {
            return false;
        }

        missedQueue.remove(ticket);
        ticket.setStatus("finished");
        ticket.setCompletedAt(System.currentTimeMillis());

        addCallRecord(ticket, "系统", "finished");
        System.out.println("过号 " + ticket.getNumber() + " 已标记结束");
        return true;
    }

    public synchronized Counter addCounter(String name, List<String> supportedTypes) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String id = "counter-" + System.currentTimeMillis();
        List<String> types = (supportedTypes != null && !supportedTypes.isEmpty())
                ? supportedTypes
                : Arrays.asList("咨询", "办理", "售后");
        Counter counter = new Counter(id, name.trim(), types);
        counters.add(counter);
        System.out.println("已新增窗口: " + counter.getName());
        return counter;
    }

    public synchronized boolean updateCounter(String counterId, String name, List<String> supportedTypes) {
        Counter counter = findCounter(counterId);
        if (counter == null) {
            return false;
        }
        if (name != null && !name.trim().isEmpty()) {
            counter.setName(name.trim());
        }
        if (supportedTypes != null && !supportedTypes.isEmpty()) {
            counter.setSupportedBusinessTypes(supportedTypes);
        }
        System.out.println("已更新窗口: " + counter.getName());
        return true;
    }

    public synchronized boolean toggleCounter(String counterId, boolean enabled) {
        Counter counter = findCounter(counterId);
        if (counter == null) {
            return false;
        }
        if (!enabled && counter.getCurrentTicket() != null) {
            return false;
        }
        counter.setEnabled(enabled);
        if (!enabled) {
            counter.setStatus("idle");
        }
        System.out.println("窗口 " + counter.getName() + " 已" + (enabled ? "启用" : "停用"));
        return true;
    }

    private void updateCurrentCalling() {
        Ticket latestCalling = null;
        for (Counter c : counters) {
            if (c.isEnabled() && c.getCurrentTicket() != null) {
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
        if (counterId == null) return null;
        for (Counter counter : counters) {
            if (counterId.equals(counter.getId())) {
                return counter;
            }
        }
        return null;
    }

    private Counter findEnabledCounter(String counterId) {
        Counter counter = findCounter(counterId);
        if (counter != null && counter.isEnabled()) {
            return counter;
        }
        return null;
    }

    private Ticket findTicketById(String ticketId) {
        if (ticketId == null) return null;
        for (Ticket t : waitingQueue) {
            if (ticketId.equals(t.getId())) return t;
        }
        for (Ticket t : missedQueue) {
            if (ticketId.equals(t.getId())) return t;
        }
        for (Counter c : counters) {
            if (c.getCurrentTicket() != null && ticketId.equals(c.getCurrentTicket().getId())) {
                return c.getCurrentTicket();
            }
        }
        return null;
    }

    private Ticket findMissedTicket(String ticketId) {
        if (ticketId == null) return null;
        for (Ticket t : missedQueue) {
            if (ticketId.equals(t.getId())) {
                return t;
            }
        }
        return null;
    }

    /**
     * 按今日日期过滤，准确计算今日统计
     */
    private TodayStats calculateTodayStats() {
        checkDayChange();
        long startOfToday = getStartOfTodayMillis();

        int totalTakenToday = 0;
        int waiting = 0;
        int inProgress = 0;
        int completed = 0;
        int missed = 0;
        long totalWaitSeconds = 0;
        int completedWithWait = 0;

        for (Ticket t : waitingQueue) {
            if (isToday(t.getCreatedAt())) {
                waiting++;
                totalTakenToday++;
            }
        }

        for (Ticket t : missedQueue) {
            if (isToday(t.getCreatedAt())) {
                missed++;
                totalTakenToday++;
            }
        }

        for (Counter c : counters) {
            if (c.isEnabled() && c.getCurrentTicket() != null) {
                Ticket t = c.getCurrentTicket();
                if (isToday(t.getCreatedAt())) {
                    inProgress++;
                    totalTakenToday++;
                }
            }
        }

        for (CallRecord record : callRecords) {
            if (!isToday(record.getTimestamp())) {
                continue;
            }
            String action = record.getAction();
            if ("completed".equals(action)) {
                completed++;
                Ticket t = record.getTicket();
                if (t != null && t.getCreatedAt() > 0 && !isToday(t.getCreatedAt())) {
                    continue;
                }
                totalTakenToday++;
                if (t != null && t.getCalledAt() != null && t.getCreatedAt() > 0) {
                    long waitMs = t.getCalledAt() - t.getCreatedAt();
                    if (waitMs >= 0) {
                        totalWaitSeconds += waitMs / 1000;
                        completedWithWait++;
                    }
                }
            } else if ("missed".equals(action)) {
                Ticket t = record.getTicket();
                if (t != null && t.getCreatedAt() > 0 && !isToday(t.getCreatedAt())) {
                    continue;
                }
                totalTakenToday++;
            } else if ("finished".equals(action)) {
                Ticket t = record.getTicket();
                if (t != null && t.getCreatedAt() > 0 && !isToday(t.getCreatedAt())) {
                    continue;
                }
                totalTakenToday++;
            }
        }

        long avgWait = completedWithWait > 0 ? totalWaitSeconds / completedWithWait : 0;

        return new TodayStats(totalTakenToday, waiting, inProgress, completed, missed, avgWait);
    }

    /**
     * 获取队列状态，返回全部对象的深拷贝，避免序列化过程中被并发修改
     */
    public synchronized QueueState getQueueState() {
        checkDayChange();

        List<Ticket> waitingCopy = new ArrayList<>();
        for (Ticket t : waitingQueue) {
            waitingCopy.add(new Ticket(t));
        }
        Collections.sort(waitingCopy, new Comparator<Ticket>() {
            @Override
            public int compare(Ticket t1, Ticket t2) {
                return Long.compare(t1.getCreatedAt(), t2.getCreatedAt());
            }
        });

        List<Ticket> missedCopy = new ArrayList<>();
        for (Ticket t : missedQueue) {
            missedCopy.add(new Ticket(t));
        }
        Collections.sort(missedCopy, new Comparator<Ticket>() {
            @Override
            public int compare(Ticket t1, Ticket t2) {
                Long called1 = t1.getCalledAt();
                Long called2 = t2.getCalledAt();
                if (called1 == null) called1 = 0L;
                if (called2 == null) called2 = 0L;
                return Long.compare(called2, called1);
            }
        });

        List<Counter> countersCopy = new ArrayList<>();
        for (Counter c : counters) {
            countersCopy.add(new Counter(c));
        }

        List<CallRecord> recordsCopy = new ArrayList<>();
        for (CallRecord r : callRecords) {
            recordsCopy.add(new CallRecord(r));
        }

        Ticket callingCopy = (currentCalling != null) ? new Ticket(currentCalling) : null;

        QueueState state = new QueueState();
        state.setWaitingQueue(waitingCopy);
        state.setMissedQueue(missedCopy);
        state.setCounters(countersCopy);
        state.setCurrentCalling(callingCopy);
        state.setCallRecords(recordsCopy);
        state.setTodayStats(calculateTodayStats());
        state.setNextNumber(nextNumber);
        state.setTodayDate(todayDate);
        return state;
    }

    public synchronized List<Counter> getCounters() {
        List<Counter> result = new ArrayList<>();
        for (Counter c : counters) {
            result.add(new Counter(c));
        }
        return result;
    }
}
