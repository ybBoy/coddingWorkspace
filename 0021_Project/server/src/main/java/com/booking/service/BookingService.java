package com.booking.service;

import com.booking.model.Booking;
import com.booking.model.Session;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BookingService 业务逻辑类
 * 职责：
 *   1. 管理所有场次和预约数据（内存存储）
 *   2. 处理预约逻辑（名额满时进入候补队列）
 *   3. 处理取消逻辑（取消时候补队列第一位自动转为正式预约）
 *   4. 处理签到逻辑
 *   5. 维护每个场次的候补队列（按时间顺序，FIFO）
 *   6. 提供查询方法供 WebSocket 层调用
 *
 * 数据流：
 *   BookingWebSocket 接收前端消息 → BookingService 处理业务逻辑 → 更新内存数据
 *   → 通过 WebSocket 广播给所有客户端 → FileStore 定时持久化
 */
public class BookingService {

    // 场次数据：key=sessionId
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    // 预约数据：key=bookingId
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();

    // 候补队列：key=sessionId, value=bookingId 列表（按候补先后顺序）
    private final Map<String, List<String>> waitlistQueues = new ConcurrentHashMap<>();

    // 用于生成唯一 ID
    private final AtomicInteger bookingIdCounter = new AtomicInteger(0);

    public BookingService() {
    }

    /**
     * 初始化默认场次数据（今天的场次）
     */
    public void initDefaultSessions() {
        String today = Session.todayString();
        addSession(new Session("s1", "晨间瑜伽课", today, "08:00", "09:00", 10));
        addSession(new Session("s2", "动感单车课", today, "10:00", "11:00", 15));
        addSession(new Session("s3", "力量训练", today, "14:00", "15:30", 12));
        addSession(new Session("s4", "普拉提", today, "16:00", "17:00", 8));
        addSession(new Session("s5", "有氧搏击", today, "19:00", "20:00", 20));
    }

    /**
     * 获取今天的场次（按开始时间排序）
     */
    public List<Session> getAllSessions() {
        List<Session> list = new ArrayList<>();
        for (Session s : sessions.values()) {
            if (s.isToday()) {
                list.add(s);
            }
        }
        Collections.sort(list, new Comparator<Session>() {
            @Override
            public int compare(Session s1, Session s2) {
                return s1.getStartTime().compareTo(s2.getStartTime());
            }
        });
        return list;
    }

    /**
     * 获取所有场次（含非今天，用于管理或调试）
     */
    public List<Session> getAllSessionsRaw() {
        return new ArrayList<>(sessions.values());
    }

    /**
     * 确保今天的默认场次存在（用于从持久化文件加载后补齐今天场次）
     * 如果今天已经有任何场次则不覆盖（保留已经存在的自定义场次及其预约）
     */
    public void ensureTodaySessions() {
        String today = Session.todayString();
        boolean hasToday = false;
        for (Session s : sessions.values()) {
            if (today.equals(s.getDate())) {
                hasToday = true;
                break;
            }
        }
        if (!hasToday) {
            System.out.println("[BookingService] 未检测到今日场次，自动生成 " + today + " 的默认场次");
            initDefaultSessions();
        }
    }

    /**
     * 获取所有预约
     */
    public List<Booking> getAllBookings() {
        return new ArrayList<>(bookings.values());
    }

    /**
     * 获取所有候补队列
     */
    public Map<String, List<String>> getAllWaitlistQueues() {
        Map<String, List<String>> copy = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : waitlistQueues.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    /**
     * 设置场次（从文件恢复时使用）
     */
    public void setSessions(List<Session> sessionList) {
        sessions.clear();
        for (Session s : sessionList) {
            sessions.put(s.getId(), s);
            if (!waitlistQueues.containsKey(s.getId())) {
                waitlistQueues.put(s.getId(), new ArrayList<String>());
            }
        }
    }

    /**
     * 设置预约（从文件恢复时使用）
     */
    public void setBookings(List<Booking> bookingList) {
        bookings.clear();
        int maxId = 0;
        for (Booking b : bookingList) {
            bookings.put(b.getId(), b);
            // 解析 ID 中的数字部分，更新计数器
            try {
                String numStr = b.getId().replaceAll("\\D", "");
                int num = Integer.parseInt(numStr);
                if (num > maxId) maxId = num;
            } catch (Exception ignored) {
            }
        }
        bookingIdCounter.set(maxId);
    }

    /**
     * 设置候补队列（从文件恢复时使用）
     */
    public void setWaitlistQueues(Map<String, List<String>> queues) {
        waitlistQueues.clear();
        for (Map.Entry<String, List<String>> entry : queues.entrySet()) {
            waitlistQueues.put(entry.getKey(),
                    Collections.synchronizedList(new ArrayList<>(entry.getValue())));
        }
    }

    /**
     * 添加场次
     */
    public void addSession(Session session) {
        sessions.put(session.getId(), session);
        waitlistQueues.put(session.getId(), Collections.synchronizedList(new ArrayList<String>()));
    }

    /**
     * 根据 ID 获取场次
     */
    public Session getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 根据 ID 获取预约
     */
    public Booking getBooking(String bookingId) {
        return bookings.get(bookingId);
    }

    /**
     * 获取某个场次的所有有效预约（非取消状态）
     */
    public List<Booking> getBookingsBySession(String sessionId) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : bookings.values()) {
            if (b.getSessionId().equals(sessionId) && b.isActive()) {
                result.add(b);
            }
        }
        return result;
    }

    /**
     * 预约场次
     * 逻辑：
     *   - 如果场次有剩余名额 → 直接预约成功，状态为 booked
     *   - 如果场次已满 → 进入候补队列，状态为 waitlist
     *   - 同一场次同一姓名不能重复预约
     *
     * @return 预约结果，包含 booking 和 isWaitlist（是否进入候补）
     */
    public synchronized BookingResult book(String sessionId, String userName) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            return BookingResult.fail("场次不存在");
        }

        // 检查是否已经有有效预约
        for (Booking b : bookings.values()) {
            if (b.getSessionId().equals(sessionId)
                    && b.getUserName().equals(userName)
                    && b.isActive()) {
                return BookingResult.fail("您已经预约过本场次");
            }
        }

        // 生成预约 ID
        String bookingId = "b" + bookingIdCounter.incrementAndGet();
        Booking booking;

        List<String> waitlist = waitlistQueues.get(sessionId);
        if (waitlist == null) {
            waitlist = Collections.synchronizedList(new ArrayList<String>());
            waitlistQueues.put(sessionId, waitlist);
        }

        if (session.hasAvailableSpot()) {
            // 有名额，直接预约成功
            booking = new Booking(bookingId, sessionId, userName, "booked");
            session.setBookedCount(session.getBookedCount() + 1);
        } else {
            // 没有名额，进入候补
            booking = new Booking(bookingId, sessionId, userName, "waitlist");
            waitlist.add(bookingId);
            session.setWaitlistCount(waitlist.size());
        }

        bookings.put(bookingId, booking);

        BookingResult result = new BookingResult();
        result.success = true;
        result.booking = booking;
        result.isWaitlist = "waitlist".equals(booking.getStatus());
        result.message = result.isWaitlist ? "已加入候补队列" : "预约成功";
        return result;
    }

    /**
     * 取消预约
     * 逻辑：
     *   - 取消已预约 → 名额释放
     *   - 如果有候补队列，第一位自动转为正式预约
     *   - 取消候补 → 直接从候补队列移除
     */
    public synchronized CancelResult cancelBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null || booking.isCancelled()) {
            return CancelResult.fail("预约不存在或已取消");
        }

        Session session = sessions.get(booking.getSessionId());
        if (session == null) {
            return CancelResult.fail("场次不存在");
        }

        String status = booking.getStatus();
        List<String> waitlist = waitlistQueues.get(booking.getSessionId());
        if (waitlist == null) {
            waitlist = Collections.synchronizedList(new ArrayList<String>());
            waitlistQueues.put(booking.getSessionId(), waitlist);
        }

        // 标记为已取消
        booking.setStatus("cancelled");

        CancelResult result = new CancelResult();
        result.success = true;
        result.bookingId = bookingId;
        result.promotedBooking = null;

        if ("booked".equals(status) || "checkedIn".equals(status)) {
            // 释放名额
            session.setBookedCount(Math.max(0, session.getBookedCount() - 1));
            if ("checkedIn".equals(status)) {
                session.setCheckedInCount(Math.max(0, session.getCheckedInCount() - 1));
            }

            // 候补队列第一位自动转为正式预约
            if (!waitlist.isEmpty()) {
                String firstWaitlistId = waitlist.remove(0);
                Booking waitlistBooking = bookings.get(firstWaitlistId);
                if (waitlistBooking != null && "waitlist".equals(waitlistBooking.getStatus())) {
                    waitlistBooking.setStatus("booked");
                    session.setBookedCount(session.getBookedCount() + 1);
                    session.setWaitlistCount(waitlist.size());
                    result.promotedBooking = waitlistBooking;
                }
            } else {
                session.setWaitlistCount(0);
            }
        } else if ("waitlist".equals(status)) {
            // 从候补队列移除
            waitlist.remove(bookingId);
            session.setWaitlistCount(waitlist.size());
        }

        return result;
    }

    /**
     * 签到
     * 只能对已预约（booked）状态的预约进行签到
     */
    public synchronized CheckInResult checkIn(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            return CheckInResult.fail("预约不存在");
        }
        if (booking.isCancelled()) {
            return CheckInResult.fail("预约已取消，无法签到");
        }
        if ("checkedIn".equals(booking.getStatus())) {
            return CheckInResult.fail("已签到，无需重复操作");
        }
        if ("waitlist".equals(booking.getStatus())) {
            return CheckInResult.fail("候补中，请等待转为正式预约后再签到");
        }

        booking.setStatus("checkedIn");

        Session session = sessions.get(booking.getSessionId());
        if (session != null) {
            session.setCheckedInCount(session.getCheckedInCount() + 1);
        }

        CheckInResult result = new CheckInResult();
        result.success = true;
        result.bookingId = bookingId;
        result.booking = booking;
        return result;
    }

    // ========== 结果包装类 ==========

    public static class BookingResult {
        public boolean success;
        public Booking booking;
        public boolean isWaitlist;
        public String message;

        public static BookingResult fail(String message) {
            BookingResult r = new BookingResult();
            r.success = false;
            r.message = message;
            return r;
        }
    }

    public static class CancelResult {
        public boolean success;
        public String bookingId;
        public Booking promotedBooking; // 候补转正的预约（如果有）
        public String message;

        public static CancelResult fail(String message) {
            CancelResult r = new CancelResult();
            r.success = false;
            r.message = message;
            return r;
        }
    }

    public static class CheckInResult {
        public boolean success;
        public String bookingId;
        public Booking booking;
        public String message;

        public static CheckInResult fail(String message) {
            CheckInResult r = new CheckInResult();
            r.success = false;
            r.message = message;
            return r;
        }
    }
}
