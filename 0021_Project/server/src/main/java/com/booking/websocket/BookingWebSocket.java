package com.booking.websocket;

import com.booking.model.Booking;
import com.booking.model.User;
import com.booking.service.BookingService;
import com.booking.store.FileStore;
import com.google.gson.Gson;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * BookingWebSocket WebSocket 处理器
 * 职责：
 *   1. 管理所有客户端连接
 *   2. 用户登录与角色识别
 *   3. 接收前端发来的消息（登录/预约/取消/签到/场次管理/导出）
 *   4. 调用 BookingService 处理业务逻辑，鉴权
 *   5. 将结果和更新广播给所有连接的客户端
 *   6. 生成活动动态并推送
 *
 * 注意：这里的 Session 是 javax.websocket.Session（WebSocket 连接会话），
 *      业务场次模型 com.booking.model.Session 使用全限定名以避免命名冲突。
 *
 * 消息格式（JSON）：
 *   { type: "login",     payload: { employeeId, userName } }
 *   { type: "init",      payload: {} }
 *   { type: "booking",   payload: { sessionId, employeeId, userName, phone } }
 *   { type: "cancel",    payload: { bookingId, employeeId } }
 *   { type: "checkin",   payload: { bookingId } }
 *   { type: "sessionAdd",    payload: { name, date, startTime, endTime, capacity } }
 *   { type: "sessionUpdate", payload: { sessionId, name, startTime, endTime, capacity } }
 *   { type: "sessionClose",  payload: { sessionId } }
 *   { type: "exportCsv",     payload: { sessionId } }
 *
 * 数据流：
 *   前端 → WebSocket 消息 → BookingWebSocket 解析 → BookingService 处理
 *   → 组装响应消息 → 广播给所有客户端
 */
@ServerEndpoint("/ws")
public class BookingWebSocket {

    // 所有连接的 WebSocket 会话（javax.websocket.Session）
    private static final Set<Session> wsSessions = new CopyOnWriteArraySet<>();

    // 业务服务（由 AppServer 注入）
    private static BookingService bookingService;
    private static FileStore fileStore;

    private final Gson gson = new Gson();

    // 注入服务实例
    public static void setServices(BookingService service, FileStore store) {
        bookingService = service;
        fileStore = store;
    }

    /**
     * 连接建立时调用
     */
    @OnOpen
    public void onOpen(Session session) {
        wsSessions.add(session);
        System.out.println("[WebSocket] 新连接: " + session.getId() + "，当前连接数: " + wsSessions.size());
    }

    /**
     * 连接关闭时调用
     */
    @OnClose
    public void onClose(Session session, CloseReason reason) {
        wsSessions.remove(session);
        bookingService.logout(session.getId());
        System.out.println("[WebSocket] 连接断开: " + session.getId() + "，当前连接数: " + wsSessions.size());
    }

    /**
     * 收到消息时调用
     */
    @OnMessage
    public void onMessage(Session session, String message) {
        try {
            Map<String, Object> msg = gson.fromJson(message, Map.class);
            String type = (String) msg.get("type");
            Map<String, Object> payload = (Map<String, Object>) msg.get("payload");

            switch (type) {
                case "login":
                    handleLogin(session, payload);
                    break;
                case "init":
                    handleInit(session);
                    break;
                case "booking":
                    handleBooking(session, payload);
                    break;
                case "cancel":
                    handleCancel(session, payload);
                    break;
                case "checkin":
                    handleCheckIn(session, payload);
                    break;
                case "sessionAdd":
                    handleSessionAdd(session, payload);
                    break;
                case "sessionUpdate":
                    handleSessionUpdate(session, payload);
                    break;
                case "sessionClose":
                    handleSessionClose(session, payload);
                    break;
                case "exportCsv":
                    handleExportCsv(session, payload);
                    break;
                default:
                    sendError(session, "未知消息类型: " + type);
            }
        } catch (Exception e) {
            System.err.println("[WebSocket] 处理消息失败: " + e.getMessage());
            e.printStackTrace();
            sendError(session, "消息处理失败: " + e.getMessage());
        }
    }

    /**
     * 连接错误时调用
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("[WebSocket] 连接错误: " + session.getId() + " - " + error.getMessage());
    }

    // ========== 权限辅助 ==========
    private User requireLogin(Session session) {
        User user = bookingService.getUserByWs(session.getId());
        if (user == null) {
            throw new SecurityException("请先登录");
        }
        return user;
    }

    private User requireAdmin(Session session) {
        User user = requireLogin(session);
        if (!user.isAdmin()) {
            throw new SecurityException("需要管理员权限");
        }
        return user;
    }

    // ========== 消息处理方法 ==========

    /**
     * 登录
     */
    private void handleLogin(Session session, Map<String, Object> payload) {
        String employeeId = (String) payload.get("employeeId");
        String userName = (String) payload.get("userName");

        if (employeeId == null || employeeId.trim().isEmpty()) {
            sendError(session, "工号不能为空");
            return;
        }
        if (userName == null || userName.trim().isEmpty()) {
            sendError(session, "姓名不能为空");
            return;
        }

        try {
            User user = bookingService.login(employeeId, userName, session.getId());
            Map<String, Object> okPayload = new HashMap<>();
            okPayload.put("user", user);
            okPayload.put("sessions", bookingService.getAllSessions());
            okPayload.put("bookings", bookingService.getAllBookings());
            sendMessage(session, "loginOk", okPayload);
            System.out.println("[WebSocket] 登录成功: " + employeeId + " / " + userName
                    + " (角色: " + user.getRole() + ")");
        } catch (Exception e) {
            sendError(session, e.getMessage());
        }
    }

    /**
     * 初始化：发送全量数据
     */
    private void handleInit(Session session) {
        Map<String, Object> payload = new HashMap<>();
        User user = bookingService.getUserByWs(session.getId());
        if (user != null) {
            payload.put("user", user);
        }
        payload.put("sessions", bookingService.getAllSessions());
        payload.put("bookings", bookingService.getAllBookings());
        sendMessage(session, "init", payload);
    }

    /**
     * 处理预约（需要登录）
     */
    private void handleBooking(Session session, Map<String, Object> payload) {
        User user = requireLogin(session);

        String sessionId = (String) payload.get("sessionId");
        String employeeId = (String) payload.get("employeeId");
        String userName = (String) payload.get("userName");
        String phone = (String) payload.get("phone");

        if (sessionId == null || employeeId == null || employeeId.trim().isEmpty()
                || userName == null || userName.trim().isEmpty()) {
            sendError(session, "参数不完整（需要 sessionId、工号、姓名）");
            return;
        }

        // 普通用户只能以自己的身份预约
        if (!user.isAdmin() && !employeeId.trim().equals(user.getEmployeeId())) {
            sendError(session, "只能以自己的工号预约");
            return;
        }

        BookingService.BookingResult result = bookingService.book(
                sessionId, employeeId.trim(), userName.trim(), phone != null ? phone.trim() : null);

        if (!result.success) {
            sendMessage(session, "bookingFail", Collections.singletonMap("message", result.message));
            return;
        }

        // 预约成功，通知请求方
        Map<String, Object> okPayload = new HashMap<>();
        okPayload.put("booking", result.booking);
        okPayload.put("sessions", bookingService.getAllSessions());
        sendMessage(session, "bookingOk", okPayload);

        // 生成活动动态
        SessionInfo sessionInfo = getSessionInfo(sessionId);
        String activityType = result.isWaitlist ? "waitlist" : "booking";
        String displayName = userName.trim() + "（" + employeeId.trim() + "）";
        String activityMsg = result.isWaitlist
                ? displayName + " 加入了「" + sessionInfo.name + "」的候补队列"
                : displayName + " 预约了「" + sessionInfo.name + "」";
        broadcastActivity(activityType, displayName, sessionInfo.name, activityMsg);

        // 广播场次更新给所有客户端
        broadcastSessionsUpdate();

        // 保存数据
        saveAsync();
    }

    /**
     * 处理取消预约
     * 安全校验：普通用户只能取消自己的（工号匹配），管理员可强制取消
     */
    private void handleCancel(Session session, Map<String, Object> payload) {
        User user = requireLogin(session);

        String bookingId = (String) payload.get("bookingId");
        String employeeId = (String) payload.get("employeeId");

        if (bookingId == null || employeeId == null || employeeId.trim().isEmpty()) {
            sendError(session, "参数不完整（需要 bookingId、employeeId）");
            return;
        }

        Booking booking = bookingService.getBooking(bookingId);
        if (booking == null) {
            sendError(session, "预约不存在");
            return;
        }

        // 普通用户只能取消自己的预约
        if (!user.isAdmin() && !booking.getEmployeeId().equals(employeeId.trim())) {
            sendError(session, "无权取消他人的预约");
            return;
        }
        // 普通用户必须用自己的工号取消（不能冒充他人）
        if (!user.isAdmin() && !employeeId.trim().equals(user.getEmployeeId())) {
            sendError(session, "只能以自己的工号取消");
            return;
        }

        String sessionName = getSessionInfo(booking.getSessionId()).name;
        String operator = user.getUserName() + "（" + user.getEmployeeId() + "）";

        BookingService.CancelResult result = bookingService.cancelBooking(
                bookingId, employeeId.trim(), operator, user.isAdmin());

        if (!result.success) {
            sendError(session, result.message);
            return;
        }

        // 通知请求方取消成功
        Map<String, Object> okPayload = new HashMap<>();
        okPayload.put("bookingId", bookingId);
        okPayload.put("sessions", bookingService.getAllSessions());
        sendMessage(session, "cancelOk", okPayload);

        // 广播取消活动
        String displayName = booking.getUserName() + "（" + booking.getEmployeeId() + "）";
        String cancelMsg = displayName + " 取消了「" + sessionName + "」的预约";
        broadcastActivity("cancel", displayName, sessionName, cancelMsg);

        // 如果有候补转正，也广播（带 promoted 标志，前端醒目提示）
        if (result.promotedBooking != null) {
            String promoteName = result.promotedBooking.getUserName()
                    + "（" + result.promotedBooking.getEmployeeId() + "）";
            String promoteMsg = promoteName
                    + " 从候补转为「" + sessionName + "」的正式预约 🎉";
            broadcastActivity("autoPromote", promoteName,
                    sessionName, promoteMsg, true);  // promoted=true 标识醒目提示
        }

        // 广播场次更新
        broadcastSessionsUpdate();

        // 保存数据
        saveAsync();
    }

    /**
     * 处理签到（需要管理员权限）
     */
    private void handleCheckIn(Session session, Map<String, Object> payload) {
        User admin = requireAdmin(session);

        String bookingId = (String) payload.get("bookingId");

        if (bookingId == null) {
            sendError(session, "参数不完整");
            return;
        }

        String operator = admin.getUserName() + "（" + admin.getEmployeeId() + "）";
        BookingService.CheckInResult result = bookingService.checkIn(bookingId, operator);

        if (!result.success) {
            sendError(session, result.message);
            return;
        }

        // 通知请求方签到成功
        Map<String, Object> okPayload = new HashMap<>();
        okPayload.put("bookingId", bookingId);
        okPayload.put("sessions", bookingService.getAllSessions());
        sendMessage(session, "checkInOk", okPayload);

        // 广播签到动态
        if (result.booking != null) {
            SessionInfo sessionInfo = getSessionInfo(result.booking.getSessionId());
            String displayName = result.booking.getUserName()
                    + "（" + result.booking.getEmployeeId() + "）";
            String checkInMsg = displayName
                    + " 在「" + sessionInfo.name + "」完成签到";
            broadcastActivity("checkIn", displayName,
                    sessionInfo.name, checkInMsg);
        }

        // 广播场次更新
        broadcastSessionsUpdate();

        // 保存数据
        saveAsync();
    }

    /**
     * 新增场次（管理员）
     */
    private void handleSessionAdd(Session session, Map<String, Object> payload) {
        User admin = requireAdmin(session);

        String name = (String) payload.get("name");
        String date = (String) payload.get("date");
        String startTime = (String) payload.get("startTime");
        String endTime = (String) payload.get("endTime");
        Object capObj = payload.get("capacity");
        int capacity = capObj instanceof Number ? ((Number) capObj).intValue() : 10;

        if (name == null || name.trim().isEmpty() || date == null
                || startTime == null || endTime == null) {
            sendError(session, "场次信息不完整");
            return;
        }

        try {
            String operator = admin.getUserName() + "（" + admin.getEmployeeId() + "）";
            com.booking.model.Session s = bookingService.addSessionAdmin(
                    name.trim(), date.trim(), startTime.trim(), endTime.trim(),
                    capacity, operator);

            Map<String, Object> okPayload = new HashMap<>();
            okPayload.put("session", s);
            okPayload.put("sessions", bookingService.getAllSessions());
            sendMessage(session, "sessionOk", okPayload);

            String activityMsg = operator + " 新增了场次「" + s.getName() + "」";
            broadcastActivity("sessionAdd", operator, s.getName(), activityMsg);
            broadcastSessionsUpdate();
            saveAsync();
        } catch (Exception e) {
            sendError(session, e.getMessage());
        }
    }

    /**
     * 修改场次（管理员）
     */
    private void handleSessionUpdate(Session session, Map<String, Object> payload) {
        User admin = requireAdmin(session);

        String sessionId = (String) payload.get("sessionId");
        String name = (String) payload.get("name");
        String startTime = (String) payload.get("startTime");
        String endTime = (String) payload.get("endTime");
        Object capObj = payload.get("capacity");
        Integer capacity = null;
        if (capObj instanceof Number) {
            capacity = ((Number) capObj).intValue();
        }

        if (sessionId == null) {
            sendError(session, "场次ID不能为空");
            return;
        }

        try {
            String operator = admin.getUserName() + "（" + admin.getEmployeeId() + "）";
            com.booking.model.Session s = bookingService.updateSessionAdmin(
                    sessionId, name, startTime, endTime,
                    capacity != null ? capacity : 0, operator);

            Map<String, Object> okPayload = new HashMap<>();
            okPayload.put("session", s);
            okPayload.put("sessions", bookingService.getAllSessions());
            sendMessage(session, "sessionOk", okPayload);

            String activityMsg = operator + " 修改了场次「" + s.getName() + "」";
            broadcastActivity("sessionUpdate", operator, s.getName(), activityMsg);
            broadcastSessionsUpdate();
            saveAsync();
        } catch (Exception e) {
            sendError(session, e.getMessage());
        }
    }

    /**
     * 关闭/开放场次（管理员）
     */
    private void handleSessionClose(Session session, Map<String, Object> payload) {
        User admin = requireAdmin(session);

        String sessionId = (String) payload.get("sessionId");
        Boolean close = (Boolean) payload.get("close");
        if (close == null) close = true;

        if (sessionId == null) {
            sendError(session, "场次ID不能为空");
            return;
        }

        try {
            String operator = admin.getUserName() + "（" + admin.getEmployeeId() + "）";
            String status = close ? com.booking.model.Session.STATUS_CLOSED
                                  : com.booking.model.Session.STATUS_ACTIVE;
            com.booking.model.Session s = bookingService.setSessionStatus(
                    sessionId, status, operator);

            Map<String, Object> okPayload = new HashMap<>();
            okPayload.put("session", s);
            okPayload.put("sessions", bookingService.getAllSessions());
            sendMessage(session, "sessionOk", okPayload);

            String activityMsg = operator + (close ? " 关闭了 " : " 开放了 ")
                    + "场次「" + s.getName() + "」";
            broadcastActivity("sessionStatus", operator, s.getName(), activityMsg);
            broadcastSessionsUpdate();
            saveAsync();
        } catch (Exception e) {
            sendError(session, e.getMessage());
        }
    }

    /**
     * 导出场次 CSV（管理员）
     * 返回 base64 编码的 CSV 内容供前端下载
     */
    private void handleExportCsv(Session session, Map<String, Object> payload) {
        User admin = requireAdmin(session);

        String sessionId = (String) payload.get("sessionId");
        if (sessionId == null) {
            sendError(session, "场次ID不能为空");
            return;
        }

        try {
            String csv = bookingService.exportSessionCsv(sessionId);
            com.booking.model.Session s = bookingService.getSession(sessionId);
            String filename = (s != null ? s.getName() : "签到统计")
                    + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv";

            Map<String, Object> okPayload = new HashMap<>();
            okPayload.put("sessionId", sessionId);
            okPayload.put("filename", filename);
            try {
                okPayload.put("csvBase64", Base64.getEncoder().encodeToString(
                        csv.getBytes("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                okPayload.put("csvBase64", Base64.getEncoder().encodeToString(
                        csv.getBytes()));
            }
            sendMessage(session, "exportCsvOk", okPayload);

            System.out.println("[WebSocket] " + admin.getUserName()
                    + " 导出了场次 CSV: " + filename);
        } catch (Exception e) {
            sendError(session, e.getMessage());
        }
    }

    // ========== 广播方法 ==========

    /**
     * 广播场次更新给所有客户端
     */
    private void broadcastSessionsUpdate() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sessions", bookingService.getAllSessions());
        payload.put("bookings", bookingService.getAllBookings());
        broadcast("sessions", payload);
    }

    /**
     * 广播活动动态
     */
    private void broadcastActivity(String type, String userName,
                                    String sessionName, String message) {
        broadcastActivity(type, userName, sessionName, message, false);
    }

    private void broadcastActivity(String type, String userName,
                                    String sessionName, String message,
                                    boolean promoted) {
        Map<String, Object> activity = new HashMap<>();
        activity.put("id", "act_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000));
        activity.put("time", formatTime(System.currentTimeMillis()));
        activity.put("type", type);
        activity.put("userName", userName);
        activity.put("sessionName", sessionName);
        activity.put("message", message);
        activity.put("promoted", promoted);

        Map<String, Object> payload = new HashMap<>();
        payload.put("activity", activity);
        payload.put("sessions", bookingService.getAllSessions());
        payload.put("bookings", bookingService.getAllBookings());
        broadcast("activity", payload);
    }

    /**
     * 广播消息给所有连接的客户端
     */
    private void broadcast(String type, Object payload) {
        String json = gson.toJson(buildMessage(type, payload));
        for (Session s : wsSessions) {
            if (s.isOpen()) {
                try {
                    s.getBasicRemote().sendText(json);
                } catch (IOException e) {
                    System.err.println("[WebSocket] 发送消息失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 发送消息给单个客户端
     */
    private void sendMessage(Session session, String type, Object payload) {
        try {
            String json = gson.toJson(buildMessage(type, payload));
            session.getBasicRemote().sendText(json);
        } catch (IOException e) {
            System.err.println("[WebSocket] 发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 发送错误消息
     */
    private void sendError(Session session, String message) {
        sendMessage(session, "error", Collections.singletonMap("message", message));
    }

    /**
     * 构建消息对象
     */
    private Map<String, Object> buildMessage(String type, Object payload) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", type);
        msg.put("payload", payload);
        return msg;
    }

    /**
     * 异步保存数据
     */
    private void saveAsync() {
        if (fileStore != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    fileStore.save();
                }
            }).start();
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 获取场次简要信息（使用全限定名避免命名冲突）
     */
    private SessionInfo getSessionInfo(String sessionId) {
        SessionInfo info = new SessionInfo();
        info.id = sessionId;
        info.name = sessionId;
        com.booking.model.Session s = bookingService.getSession(sessionId);
        if (s != null) {
            info.name = s.getName();
        }
        return info;
    }

    /**
     * 格式化时间为 HH:mm:ss
     */
    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private static class SessionInfo {
        String id;
        String name;
    }
}
