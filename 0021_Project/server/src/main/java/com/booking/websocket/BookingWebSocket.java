package com.booking.websocket;

import com.booking.model.Booking;
import com.booking.service.BookingService;
import com.booking.store.FileStore;
import com.google.gson.Gson;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * BookingWebSocket WebSocket 处理器
 * 职责：
 *   1. 管理所有客户端连接
 *   2. 接收前端发来的消息（预约/取消/签到/初始化）
 *   3. 调用 BookingService 处理业务逻辑
 *   4. 将结果和更新广播给所有连接的客户端
 *   5. 生成活动动态并推送
 *
 * 注意：这里的 Session 是 javax.websocket.Session（WebSocket 连接会话），
 *      业务场次模型 com.booking.model.Session 使用全限定名以避免命名冲突。
 *
 * 消息格式（JSON）：
 *   { type: "booking", payload: { sessionId, userName } }
 *   { type: "cancel", payload: { bookingId, userName } }
 *   { type: "checkin", payload: { bookingId } }
 *   { type: "init", payload: {} }
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

    // ========== 消息处理方法 ==========

    /**
     * 初始化：发送全量数据
     */
    private void handleInit(Session session) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sessions", bookingService.getAllSessions());
        payload.put("bookings", bookingService.getAllBookings());
        sendMessage(session, "init", payload);
    }

    /**
     * 处理预约
     */
    private void handleBooking(Session session, Map<String, Object> payload) {
        String sessionId = (String) payload.get("sessionId");
        String userName = (String) payload.get("userName");

        if (sessionId == null || userName == null || userName.trim().isEmpty()) {
            sendMessage(session, "bookingFail", Collections.singletonMap("message", "参数不完整"));
            return;
        }

        BookingService.BookingResult result = bookingService.book(sessionId, userName.trim());

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
        String activityMsg = result.isWaitlist
                ? userName + " 加入了「" + sessionInfo.name + "」的候补队列"
                : userName + " 预约了「" + sessionInfo.name + "」";
        broadcastActivity(activityType, userName, sessionInfo.name, activityMsg);

        // 广播场次更新给所有客户端
        broadcastSessionsUpdate();

        // 保存数据
        saveAsync();
    }

    /**
     * 处理取消预约
     * 安全校验：必须提供与 booking 记录一致的 userName
     */
    private void handleCancel(Session session, Map<String, Object> payload) {
        String bookingId = (String) payload.get("bookingId");
        String userName = (String) payload.get("userName");

        if (bookingId == null || userName == null || userName.trim().isEmpty()) {
            sendMessage(session, "error", Collections.singletonMap("message", "参数不完整"));
            return;
        }

        Booking booking = bookingService.getBooking(bookingId);
        if (booking == null) {
            sendMessage(session, "error", Collections.singletonMap("message", "预约不存在"));
            return;
        }

        // 安全校验：userName 必须匹配
        if (!booking.getUserName().equals(userName.trim())) {
            sendMessage(session, "error", Collections.singletonMap("message", "无权取消他人的预约"));
            return;
        }

        String sessionName = getSessionInfo(booking.getSessionId()).name;

        BookingService.CancelResult result = bookingService.cancelBooking(bookingId);

        if (!result.success) {
            sendMessage(session, "error", Collections.singletonMap("message", result.message));
            return;
        }

        // 通知请求方取消成功
        Map<String, Object> okPayload = new HashMap<>();
        okPayload.put("bookingId", bookingId);
        okPayload.put("sessions", bookingService.getAllSessions());
        sendMessage(session, "cancelOk", okPayload);

        // 广播取消活动
        String cancelMsg = userName + " 取消了「" + sessionName + "」的预约";
        broadcastActivity("cancel", userName, sessionName, cancelMsg);

        // 如果有候补转正，也广播
        if (result.promotedBooking != null) {
            String promoteMsg = result.promotedBooking.getUserName()
                    + " 从候补转为「" + sessionName + "」的正式预约";
            broadcastActivity("autoPromote", result.promotedBooking.getUserName(),
                    sessionName, promoteMsg);
        }

        // 广播场次更新
        broadcastSessionsUpdate();

        // 保存数据
        saveAsync();
    }

    /**
     * 处理签到
     */
    private void handleCheckIn(Session session, Map<String, Object> payload) {
        String bookingId = (String) payload.get("bookingId");

        if (bookingId == null) {
            sendMessage(session, "error", Collections.singletonMap("message", "参数不完整"));
            return;
        }

        BookingService.CheckInResult result = bookingService.checkIn(bookingId);

        if (!result.success) {
            sendMessage(session, "error", Collections.singletonMap("message", result.message));
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
            String checkInMsg = result.booking.getUserName()
                    + " 在「" + sessionInfo.name + "」完成签到";
            broadcastActivity("checkIn", result.booking.getUserName(),
                    sessionInfo.name, checkInMsg);
        }

        // 广播场次更新
        broadcastSessionsUpdate();

        // 保存数据
        saveAsync();
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
    private void broadcastActivity(String type, String userName, String sessionName, String message) {
        Map<String, Object> activity = new HashMap<>();
        activity.put("id", "act_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000));
        activity.put("time", formatTime(System.currentTimeMillis()));
        activity.put("type", type);
        activity.put("userName", userName);
        activity.put("sessionName", sessionName);
        activity.put("message", message);

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
