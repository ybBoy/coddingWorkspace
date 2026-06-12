package com.danmaku;

import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class DanmakuWebSocket {
    private static final Set<Session> allSessions =
            Collections.newSetFromMap(new ConcurrentHashMap<Session, Boolean>());
    private static final Set<Session> moderatorSessions =
            Collections.newSetFromMap(new ConcurrentHashMap<Session, Boolean>());
    private static final Gson gson = new Gson();
    private static final DanmakuService service = DanmakuService.getInstance();

    private Session session;
    private String role = "viewer";

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        allSessions.add(session);
        sendInitialState();
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        allSessions.remove(session);
        moderatorSessions.remove(session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        try {
            Map map = gson.fromJson(message, Map.class);
            String type = (String) map.get("type");

            if ("SEND_MESSAGE".equals(type)) {
                handleSendMessage(map);
            } else if ("APPROVE_MESSAGE".equals(type)) {
                handleApproveMessage(map);
            } else if ("REJECT_MESSAGE".equals(type)) {
                handleRejectMessage(map);
            } else if ("SET_ROLE".equals(type)) {
                handleSetRole(map);
            } else if ("CLEAR_SCREEN".equals(type)) {
                handleClearScreen();
            } else if ("TOGGLE_SENDING".equals(type)) {
                handleToggleSending(map);
            } else if ("GET_PENDING".equals(type)) {
                sendPendingList();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleSendMessage(Map map) {
        Map data = (Map) map.get("data");
        String content = (String) data.get("content");
        String nickname = (String) data.get("nickname");

        if (content == null || content.trim().isEmpty()) {
            return;
        }
        if (content.length() > 100) {
            content = content.substring(0, 100);
        }

        Message msg = service.addMessage(content.trim(), nickname);
        if (msg != null) {
            sendMessage(buildMessage("MESSAGE_QUEUED", msg));
        } else {
            sendMessage(buildSimpleMessage("SENDING_DISABLED"));
        }
    }

    private void handleApproveMessage(Map map) {
        if (!"moderator".equals(role)) return;
        Map data = (Map) map.get("data");
        String id = (String) data.get("id");
        service.approveMessage(id);
    }

    private void handleRejectMessage(Map map) {
        if (!"moderator".equals(role)) return;
        Map data = (Map) map.get("data");
        String id = (String) data.get("id");
        service.rejectMessage(id);
    }

    private void handleSetRole(Map map) {
        Map data = (Map) map.get("data");
        String newRole = (String) data.get("role");
        if ("moderator".equals(newRole)) {
            this.role = "moderator";
            moderatorSessions.add(session);
            sendPendingList();
        } else if ("viewer".equals(newRole)) {
            this.role = "viewer";
            moderatorSessions.remove(session);
        }
    }

    private void handleClearScreen() {
        if (!"moderator".equals(role)) return;
        service.clearScreen();
    }

    private void handleToggleSending(Map map) {
        if (!"moderator".equals(role)) return;
        Map data = (Map) map.get("data");
        Boolean enabled = (Boolean) data.get("enabled");
        if (enabled != null) {
            service.setSendingEnabled(enabled);
        }
    }

    private void sendInitialState() {
        Map<String, Object> state = new HashMap<String, Object>();
        state.put("type", "INITIAL_STATE");
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("sendingEnabled", service.isSendingEnabled());
        data.put("pendingCount", service.getPendingMessages().size());
        state.put("data", data);
        sendMessage(state);
    }

    private void sendPendingList() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("type", "PENDING_LIST");
        map.put("data", service.getPendingMessages());
        sendMessage(map);
    }

    private void sendMessage(Object message) {
        try {
            if (session != null && session.isOpen()) {
                session.getRemote().sendString(gson.toJson(message));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcast(Object message) {
        String json = gson.toJson(message);
        for (Session s : allSessions) {
            if (s.isOpen()) {
                try {
                    s.getRemote().sendString(json);
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public static void broadcastToModerators(Object message) {
        String json = gson.toJson(message);
        for (Session s : moderatorSessions) {
            if (s.isOpen()) {
                try {
                    s.getRemote().sendString(json);
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private static Map<String, Object> buildMessage(String type, Object data) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("type", type);
        map.put("data", data);
        return map;
    }

    private static Map<String, Object> buildSimpleMessage(String type) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("type", type);
        return map;
    }
}
