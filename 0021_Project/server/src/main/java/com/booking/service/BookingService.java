package com.booking.service;

import com.booking.model.Booking;
import com.booking.model.Session;
import com.booking.model.User;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BookingService 业务逻辑类
 * 职责：
 *   1. 管理所有场次和预约数据（内存存储）
 *   2. 管理用户登录、角色权限（普通用户 / 管理员）
 *   3. 场次 CRUD（新增、修改、关闭）
 *   4. 处理预约逻辑（身份=工号+姓名，避免同名；名额满时进入候补）
 *   5. 处理取消逻辑（校验身份；取消时候补队列第一位自动转为正式预约）
 *   6. 处理签到逻辑（按工号/姓名搜索）
 *   7. 签到统计 CSV 导出
 *   8. 操作日志（支持撤销的基础）
 *   9. 跨天自动生成今日场次模板
 *
 * 数据流：
 *   BookingWebSocket 接收前端消息 → BookingService 处理业务逻辑 → 更新内存数据
 *   → 通过 WebSocket 广播给所有客户端 → FileStore 定时持久化
 */
public class BookingService {

    // ============ 数据存储 ============
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();
    private final Map<String, List<String>> waitlistQueues = new ConcurrentHashMap<>();
    private final Map<String, User> usersByWs = new ConcurrentHashMap<>();  // wsSessionId -> User

    // 用于生成唯一 ID
    private final AtomicInteger bookingIdCounter = new AtomicInteger(0);
    private final AtomicInteger sessionIdCounter = new AtomicInteger(100);
    private final AtomicInteger logIdCounter = new AtomicInteger(0);

    // 操作日志栈，支持撤销（最近 50 条）
    private final Deque<OperationLog> operationLogs = new LinkedList<>();
    private static final int MAX_LOGS = 50;

    // 管理员工号列表（简单硬编码，实际可配文件）
    private static final Set<String> ADMIN_EMPLOYEE_IDS = new HashSet<>(
            Arrays.asList("admin", "A001", "A002")
    );

    public BookingService() {
    }

    // ============ 初始化 ============
    /**
     * 初始化默认场次数据（今天的场次）
     */
    public void initDefaultSessions() {
        String today = Session.todayString();
        addSession(new Session("s1", "晨间瑜伽课", today, "08:00", "09:00", 10, "system"));
        addSession(new Session("s2", "动感单车课", today, "10:00", "11:00", 15, "system"));
        addSession(new Session("s3", "力量训练", today, "14:00", "15:30", 12, "system"));
        addSession(new Session("s4", "普拉提", today, "16:00", "17:00", 8, "system"));
        addSession(new Session("s5", "有氧搏击", today, "19:00", "20:00", 20, "system"));
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

    // ============ 查询 ============
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

    public List<Session> getAllSessionsRaw() {
        return new ArrayList<>(sessions.values());
    }

    public List<Booking> getAllBookings() {
        return new ArrayList<>(bookings.values());
    }

    public Map<String, List<String>> getAllWaitlistQueues() {
        Map<String, List<String>> copy = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : waitlistQueues.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    public Session getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public Booking getBooking(String bookingId) {
        return bookings.get(bookingId);
    }

    public List<Booking> getBookingsBySession(String sessionId) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : bookings.values()) {
            if (b.getSessionId().equals(sessionId) && b.isActive()) {
                result.add(b);
            }
        }
        return result;
    }

    public User getUserByWs(String wsSessionId) {
        return usersByWs.get(wsSessionId);
    }

    // ============ 持久化辅助 ============
    public void setSessions(List<Session> sessionList) {
        sessions.clear();
        int maxId = 0;
        for (Session s : sessionList) {
            sessions.put(s.getId(), s);
            if (!waitlistQueues.containsKey(s.getId())) {
                waitlistQueues.put(s.getId(), Collections.synchronizedList(new ArrayList<String>()));
            }
            try {
                String numStr = s.getId().replaceAll("\\D", "");
                int num = Integer.parseInt(numStr);
                if (num > maxId) maxId = num;
            } catch (Exception ignored) {
            }
        }
        if (maxId > sessionIdCounter.get()) {
            sessionIdCounter.set(maxId);
        }
    }

    public void setBookings(List<Booking> bookingList) {
        bookings.clear();
        int maxId = 0;
        for (Booking b : bookingList) {
            bookings.put(b.getId(), b);
            try {
                String numStr = b.getId().replaceAll("\\D", "");
                int num = Integer.parseInt(numStr);
                if (num > maxId) maxId = num;
            } catch (Exception ignored) {
            }
        }
        bookingIdCounter.set(maxId);
    }

    public void setWaitlistQueues(Map<String, List<String>> queues) {
        waitlistQueues.clear();
        for (Map.Entry<String, List<String>> entry : queues.entrySet()) {
            waitlistQueues.put(entry.getKey(),
                    Collections.synchronizedList(new ArrayList<>(entry.getValue())));
        }
    }

    // ============ 用户登录 ============
    /**
     * 用户登录
     * @param employeeId 工号（必填，身份唯一标识）
     * @param userName 姓名
     * @param wsSessionId WebSocket 连接 ID
     * @return 登录后的用户对象
     */
    public synchronized User login(String employeeId, String userName, String wsSessionId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            throw new IllegalArgumentException("工号不能为空");
        }
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("姓名不能为空");
        }

        String role = ADMIN_EMPLOYEE_IDS.contains(employeeId.trim())
                ? User.ROLE_ADMIN
                : User.ROLE_USER;

        User user = new User(employeeId.trim(), userName.trim(), role);
        user.setWsSessionId(wsSessionId);
        usersByWs.put(wsSessionId, user);

        System.out.println("[BookingService] 用户登录: " + employeeId + " / " + userName
                + " / 角色: " + role + " / WS: " + wsSessionId);
        return user;
    }

    /**
     * 用户登出（连接断开时调用）
     */
    public void logout(String wsSessionId) {
        usersByWs.remove(wsSessionId);
    }

    // ============ 场次管理（管理员） ============
    /**
     * 新增场次
     */
    public synchronized Session addSessionAdmin(String name, String date, String startTime,
                                                 String endTime, int capacity, String createdBy) {
        String sessionId = "s" + sessionIdCounter.incrementAndGet();
        Session session = new Session(sessionId, name, date, startTime, endTime, capacity, createdBy);
        addSession(session);
        addLog("sessionAdd", createdBy, "新增场次: " + name, sessionId);
        return session;
    }

    /**
     * 修改场次基本信息
     */
    public synchronized Session updateSessionAdmin(String sessionId, String name, String date,
                                                   String startTime, String endTime,
                                                   int capacity, String operator) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("场次不存在");
        }
        if (name != null && !name.trim().isEmpty()) session.setName(name.trim());
        if (date != null && !date.trim().isEmpty()) session.setDate(date.trim());
        if (startTime != null && !startTime.trim().isEmpty()) session.setStartTime(startTime.trim());
        if (endTime != null && !endTime.trim().isEmpty()) session.setEndTime(endTime.trim());
        if (capacity > 0) session.setCapacity(capacity);

        addLog("sessionUpdate", operator, "修改场次: " + session.getName(), sessionId);
        return session;
    }

    /**
     * 关闭/开放场次
     */
    public synchronized Session setSessionStatus(String sessionId, String status, String operator) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("场次不存在");
        }
        session.setStatus(status);
        addLog("sessionStatus", operator,
                (Session.STATUS_CLOSED.equals(status) ? "关闭场次: " : "开放场次: ") + session.getName(),
                sessionId);
        return session;
    }

    /**
     * 内部添加场次（含初始化）
     */
    public void addSession(Session session) {
        sessions.put(session.getId(), session);
        waitlistQueues.put(session.getId(), Collections.synchronizedList(new ArrayList<String>()));
    }

    // ============ 预约 ============
    /**
     * 预约场次（用工号+姓名作为身份）
     */
    public synchronized BookingResult book(String sessionId, String employeeId, String userName, String phone) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            return BookingResult.fail("场次不存在");
        }
        if (!session.isOpenForBooking()) {
            return BookingResult.fail("本场次已关闭预约");
        }
        if (employeeId == null || employeeId.trim().isEmpty()) {
            return BookingResult.fail("工号不能为空");
        }
        if (userName == null || userName.trim().isEmpty()) {
            return BookingResult.fail("姓名不能为空");
        }

        employeeId = employeeId.trim();
        userName = userName.trim();

        // 同一场次同一工号不能重复预约
        for (Booking b : bookings.values()) {
            if (b.getSessionId().equals(sessionId)
                    && b.getEmployeeId() != null
                    && b.getEmployeeId().equals(employeeId)
                    && b.isActive()) {
                return BookingResult.fail("您已经预约过本场次（工号: " + employeeId + "）");
            }
        }

        String bookingId = "b" + bookingIdCounter.incrementAndGet();
        Booking booking;

        List<String> waitlist = waitlistQueues.computeIfAbsent(sessionId,
                k -> Collections.synchronizedList(new ArrayList<String>()));

        if (session.hasAvailableSpot()) {
            booking = new Booking(bookingId, sessionId, employeeId, userName, phone, Booking.STATUS_BOOKED);
            session.setBookedCount(session.getBookedCount() + 1);
        } else {
            booking = new Booking(bookingId, sessionId, employeeId, userName, phone, Booking.STATUS_WAITLIST);
            waitlist.add(bookingId);
            session.setWaitlistCount(waitlist.size());
        }

        bookings.put(bookingId, booking);

        BookingResult result = new BookingResult();
        result.success = true;
        result.booking = booking;
        result.isWaitlist = Booking.STATUS_WAITLIST.equals(booking.getStatus());
        result.message = result.isWaitlist ? "已满员，已加入候补队列" : "预约成功";
        return result;
    }

    // ============ 取消 ============
    /**
     * 取消预约（校验工号匹配）
     * @param bookingId 预约ID
     * @param employeeId 工号，必须与预约者一致
     * @param operator 操作人（管理员可强制取消）
     * @param isAdmin 是否管理员操作
     */
    public synchronized CancelResult cancelBooking(String bookingId, String employeeId,
                                                   String operator, boolean isAdmin) {
        Booking booking = bookings.get(bookingId);
        if (booking == null || booking.isCancelled()) {
            return CancelResult.fail("预约不存在或已取消");
        }

        // 身份校验：自己取消自己的，或管理员强制取消
        if (!isAdmin && !booking.getEmployeeId().equals(employeeId)) {
            return CancelResult.fail("无权取消他人的预约");
        }

        Session session = sessions.get(booking.getSessionId());
        if (session == null) {
            return CancelResult.fail("场次不存在");
        }

        String status = booking.getStatus();
        List<String> waitlist = waitlistQueues.computeIfAbsent(booking.getSessionId(),
                k -> Collections.synchronizedList(new ArrayList<String>()));

        booking.setStatus(Booking.STATUS_CANCELLED);

        CancelResult result = new CancelResult();
        result.success = true;
        result.bookingId = bookingId;
        result.promotedBooking = null;
        result.cancelledBooking = booking;

        if (Booking.STATUS_BOOKED.equals(status) || Booking.STATUS_CHECKED_IN.equals(status)) {
            session.setBookedCount(Math.max(0, session.getBookedCount() - 1));
            if (Booking.STATUS_CHECKED_IN.equals(status)) {
                session.setCheckedInCount(Math.max(0, session.getCheckedInCount() - 1));
            }

            if (!waitlist.isEmpty()) {
                String firstWaitlistId = waitlist.remove(0);
                Booking waitlistBooking = bookings.get(firstWaitlistId);
                if (waitlistBooking != null && Booking.STATUS_WAITLIST.equals(waitlistBooking.getStatus())) {
                    waitlistBooking.setStatus(Booking.STATUS_BOOKED);
                    session.setBookedCount(session.getBookedCount() + 1);
                    session.setWaitlistCount(waitlist.size());
                    result.promotedBooking = waitlistBooking;
                }
            } else {
                session.setWaitlistCount(0);
            }
        } else if (Booking.STATUS_WAITLIST.equals(status)) {
            waitlist.remove(bookingId);
            session.setWaitlistCount(waitlist.size());
        }

        addLog("cancel", operator,
                "取消预约: " + booking.getUserName() + " / " + session.getName(),
                bookingId);
        return result;
    }

    // ============ 签到 ============
    /**
     * 签到（需管理员权限）
     */
    public synchronized CheckInResult checkIn(String bookingId, String operator) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            return CheckInResult.fail("预约不存在");
        }
        if (booking.isCancelled()) {
            return CheckInResult.fail("预约已取消，无法签到");
        }
        if (Booking.STATUS_CHECKED_IN.equals(booking.getStatus())) {
            return CheckInResult.fail("已签到，无需重复操作");
        }
        if (Booking.STATUS_WAITLIST.equals(booking.getStatus())) {
            return CheckInResult.fail("候补中，请等待转为正式预约后再签到");
        }

        booking.setStatus(Booking.STATUS_CHECKED_IN);

        Session session = sessions.get(booking.getSessionId());
        if (session != null) {
            session.setCheckedInCount(session.getCheckedInCount() + 1);
        }

        addLog("checkIn", operator,
                "签到: " + booking.getUserName() + " / " + (session != null ? session.getName() : ""),
                bookingId);

        CheckInResult result = new CheckInResult();
        result.success = true;
        result.bookingId = bookingId;
        result.booking = booking;
        return result;
    }

    /**
     * 搜索预约（用于签到面板搜索，支持工号或姓名模糊匹配）
     */
    public List<Booking> searchBookings(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllBookings();
        }
        String kw = keyword.trim().toLowerCase();
        List<Booking> result = new ArrayList<>();
        for (Booking b : bookings.values()) {
            if (b.isActive() && (
                    (b.getEmployeeId() != null && b.getEmployeeId().toLowerCase().contains(kw))
                    || (b.getUserName() != null && b.getUserName().toLowerCase().contains(kw))
            )) {
                result.add(b);
            }
        }
        return result;
    }

    // ============ CSV 导出 ============
    /**
     * 导出某个场次的签到统计 CSV
     */
    public String exportSessionCsv(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("场次不存在");
        }

        List<Booking> sessionBookings = new ArrayList<>();
        for (Booking b : bookings.values()) {
            if (b.getSessionId().equals(sessionId)) {
                sessionBookings.add(b);
            }
        }

        // 按状态排序：checkedIn > booked > waitlist > cancelled
        final Map<String, Integer> order = new HashMap<>();
        order.put(Booking.STATUS_CHECKED_IN, 0);
        order.put(Booking.STATUS_BOOKED, 1);
        order.put(Booking.STATUS_WAITLIST, 2);
        order.put(Booking.STATUS_CANCELLED, 3);
        Collections.sort(sessionBookings, new Comparator<Booking>() {
            @Override
            public int compare(Booking b1, Booking b2) {
                int o1 = order.getOrDefault(b1.getStatus(), 99);
                int o2 = order.getOrDefault(b2.getStatus(), 99);
                if (o1 != o2) return Integer.compare(o1, o2);
                return Long.compare(b1.getCreatedAt(), b2.getCreatedAt());
            }
        });

        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        sb.append("工号,姓名,手机号,状态,预约时间,签到时间\n");

        String statusText;
        for (Booking b : sessionBookings) {
            switch (b.getStatus()) {
                case Booking.STATUS_CHECKED_IN: statusText = "已签到"; break;
                case Booking.STATUS_BOOKED: statusText = "已预约"; break;
                case Booking.STATUS_WAITLIST: statusText = "候补中"; break;
                case Booking.STATUS_CANCELLED: statusText = "已取消"; break;
                default: statusText = b.getStatus();
            }
            String createdAt = timeFmt.format(new Date(b.getCreatedAt()));
            String checkInTime = Booking.STATUS_CHECKED_IN.equals(b.getStatus())
                    ? timeFmt.format(new Date(b.getCreatedAt() + 60000))
                    : "";
            sb.append(escapeCsv(b.getEmployeeId())).append(",")
              .append(escapeCsv(b.getUserName())).append(",")
              .append(escapeCsv(b.getPhone())).append(",")
              .append(statusText).append(",")
              .append(createdAt).append(",")
              .append(checkInTime).append("\n");
        }

        // 底部统计
        sb.append("\n").append("场次名称:,").append(escapeCsv(session.getName())).append("\n")
          .append("日期:,").append(session.getDate()).append("\n")
          .append("时间:,").append(session.getStartTime()).append(" - ").append(session.getEndTime()).append("\n")
          .append("容量:,").append(session.getCapacity()).append("\n")
          .append("已预约:,").append(session.getBookedCount()).append("\n")
          .append("已签到:,").append(session.getCheckedInCount()).append("\n")
          .append("候补中:,").append(session.getWaitlistCount()).append("\n");

        System.out.println("[BookingService] 已导出 CSV: " + session.getName()
                + ", 共 " + sessionBookings.size() + " 条记录");
        return sb.toString();
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // ============ 操作日志 & 撤销（基础结构） ============
    private void addLog(String type, String operator, String detail, String targetId) {
        OperationLog log = new OperationLog();
        log.id = "l" + logIdCounter.incrementAndGet();
        log.type = type;
        log.operator = operator;
        log.detail = detail;
        log.targetId = targetId;
        log.timestamp = System.currentTimeMillis();

        synchronized (operationLogs) {
            operationLogs.push(log);
            while (operationLogs.size() > MAX_LOGS) {
                operationLogs.removeLast();
            }
        }
    }

    public List<OperationLog> getRecentLogs() {
        synchronized (operationLogs) {
            return new ArrayList<>(operationLogs);
        }
    }

    public static class OperationLog {
        public String id;
        public String type;
        public String operator;
        public String detail;
        public String targetId;
        public long timestamp;
    }

    // ============ 结果包装类 ============
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
        public Booking cancelledBooking;
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
