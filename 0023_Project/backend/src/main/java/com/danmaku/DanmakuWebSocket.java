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
    private static final Map<String, Long> moderatorTokens = new ConcurrentHashMap<String, Long>();
    private static final long TOKEN_TTL = 8 * 3600 * 1000;

    private Session session;
    private String role = "viewer";
    private String token = null;

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        allSessions.add(session);
        sendInitialState();
        broadcastOnlineCount();
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        allSessions.remove(session);
        moderatorSessions.remove(session);
        broadcastOnlineCount();
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        try {
            Map map = gson.fromJson(message, Map.class);
            String type = (String) map.get("type");

            if ("SEND_MESSAGE".equals(type)) {
                handleSendMessage(map);
            } else if ("APPROVE_MESSAGE".equals(type)) {
                if (isModerator()) { Map d = (Map) map.get("data"); service.approveMessage((String) d.get("id")); }
            } else if ("REJECT_MESSAGE".equals(type)) {
                if (isModerator()) { Map d = (Map) map.get("data"); service.rejectMessage((String) d.get("id")); }
            } else if ("SET_ROLE".equals(type)) {
                handleSetRole(map);
            } else if ("CLEAR_SCREEN".equals(type)) {
                if (isModerator()) service.clearScreen();
            } else if ("TOGGLE_SENDING".equals(type)) {
                if (isModerator()) { Map d = (Map) map.get("data"); service.setSendingEnabled((Boolean) d.get("enabled")); }
            } else if ("GET_PENDING".equals(type)) {
                if (isModerator()) sendPendingList();
            } else if ("GET_HISTORY".equals(type)) {
                sendHistoryMessages();
            } else if ("TOGGLE_PLAYBACK".equals(type)) {
                if (isModerator()) { Map d = (Map) map.get("data"); service.setPlaybackPaused((Boolean) d.get("paused")); }
            } else if ("TOGGLE_PIN".equals(type)) {
                if (isModerator()) { Map d = (Map) map.get("data"); service.togglePinMessage((String) d.get("id")); }
            } else if ("APPROVE_NORMAL_ONLY".equals(type)) {
                if (isModerator()) service.approveNormalOnly();
            } else if ("UPDATE_SETTINGS".equals(type)) {
                handleUpdateSettings(map);
            } else if ("GET_LOGS".equals(type)) {
                if (isModerator()) sendLogs();
            } else if ("EXPORT_DATA".equals(type)) {
                if (isModerator()) handleExport();
            } else if ("ROTATE_BACKUP".equals(type)) {
                if (isModerator()) { FileStore.rotateBackup(); sendMessage(buildSimpleMessage("BACKUP_DONE")); }
            } else if ("VALIDATE_TOKEN".equals(type)) {
                handleValidateToken(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isModerator() {
        return "moderator".equals(role) && token != null && isTokenValid(token);
    }

    private boolean isTokenValid(String tok) {
        Long ts = moderatorTokens.get(tok);
        return ts != null && (System.currentTimeMillis() - ts) < TOKEN_TTL;
    }

    private void handleSetRole(Map map) {
        Map data = (Map) map.get("data");
        String newRole = (String) data.get("role");
        if ("moderator".equals(newRole)) {
            String password = (String) data.get("password");
            String savedPw = service.getSettings().getModeratorPassword();
            if (savedPw == null) savedPw = "admin123";
            if (!savedPw.equals(password)) {
                sendMessage(buildSimpleMessage("AUTH_FAILED"));
                return;
            }
            this.role = "moderator";
            this.token = UUID.randomUUID().toString();
            moderatorTokens.put(token, System.currentTimeMillis());
            moderatorSessions.add(session);
            Map<String, Object> authData = new HashMap<String, Object>();
            authData.put("token", token);
            authData.put("role", "moderator");
            sendMessage(buildDataMessage("AUTH_SUCCESS", authData));
            sendPendingList();
        } else if ("viewer".equals(newRole)) {
            this.role = "viewer";
            if (token != null) moderatorTokens.remove(token);
            this.token = null;
            moderatorSessions.remove(session);
        }
    }

    private void handleValidateToken(Map map) {
        Map data = (Map) map.get("data");
        String tok = (String) data.get("token");
        if (tok != null && isTokenValid(tok)) {
            this.role = "moderator";
            this.token = tok;
            moderatorTokens.put(tok, System.currentTimeMillis());
            moderatorSessions.add(session);
            Map<String, Object> authData = new HashMap<String, Object>();
            authData.put("token", tok);
            authData.put("role", "moderator");
            sendMessage(buildDataMessage("AUTH_SUCCESS", authData));
            sendPendingList();
        } else {
            sendMessage(buildSimpleMessage("AUTH_FAILED"));
        }
    }

    private void handleSendMessage(Map map) {
        Map data = (Map) map.get("data");
        String content = (String) data.get("content");
        String nickname = (String) data.get("nickname");
        if (content == null || content.trim().isEmpty()) return;
        if (content.length() > 100) content = content.substring(0, 100);

        Message msg = service.addMessage(content.trim(), nickname);
        if (msg != null) {
            if ("error".equals(msg.getStatus())) {
                Map<String, Object> errData = new HashMap<String, Object>();
                errData.put("reason", msg.getContent());
                sendMessage(buildDataMessage("SEND_REJECTED", errData));
            } else {
                sendMessage(buildMessage("MESSAGE_QUEUED", msg));
            }
        } else {
            sendMessage(buildSimpleMessage("SENDING_DISABLED"));
        }
    }

    private void handleUpdateSettings(Map map) {
        if (!isModerator()) return;
        Map data = (Map) map.get("data");
        FileStore.Settings s = service.getSettings();
        if (data.containsKey("eventTitle")) s.setEventTitle((String) data.get("eventTitle"));
        if (data.containsKey("welcomeMessage")) s.setWelcomeMessage((String) data.get("welcomeMessage"));
        if (data.containsKey("colorTheme")) s.setColorTheme((String) data.get("colorTheme"));
        if (data.containsKey("customColors")) {
            List<String> colors = new ArrayList<String>();
            List raw = (List) data.get("customColors");
            for (Object o : raw) colors.add((String) o);
            s.setCustomColors(colors);
        }
        if (data.containsKey("sensitiveWords")) {
            List<String> words = new ArrayList<String>();
            List raw = (List) data.get("sensitiveWords");
            for (Object o : raw) words.add((String) o);
            s.setSensitiveWords(words);
        }
        if (data.containsKey("moderatorPassword")) s.setModeratorPassword((String) data.get("moderatorPassword"));
        if (data.containsKey("speedMin")) s.setSpeedMin(((Number) data.get("speedMin")).intValue());
        if (data.containsKey("speedMax")) s.setSpeedMax(((Number) data.get("speedMax")).intValue());
        if (data.containsKey("fontSize")) s.setFontSize(((Number) data.get("fontSize")).intValue());
        if (data.containsKey("trackCount")) s.setTrackCount(((Number) data.get("trackCount")).intValue());
        service.updateSettings(s);
    }

    private void handleExport() {
        String path = FileStore.exportData();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("exportPath", path);
        sendMessage(buildDataMessage("EXPORT_DONE", data));
    }

    private void sendInitialState() {
        Map<String, Object> state = new HashMap<String, Object>();
        state.put("type", "INITIAL_STATE");
        Map<String, Object> data = service.settingsToMap();
        data.put("pendingCount", service.getPendingMessages().size());
        data.put("onlineCount", allSessions.size());
        state.put("data", data);
        sendMessage(state);
    }

    private void sendPendingList() {
        sendMessage(buildDataMessage("PENDING_LIST", service.getPendingMessages()));
    }

    private void sendHistoryMessages() {
        sendMessage(buildDataMessage("HISTORY_MESSAGES", service.getRecentApproved()));
    }

    private void sendLogs() {
        sendMessage(buildDataMessage("OPERATION_LOGS", service.getRecentLogs()));
    }

    private void broadcastOnlineCount() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("onlineCount", allSessions.size());
        broadcast(buildDataMessage("ONLINE_COUNT", data));
    }

    private void sendMessage(Object message) {
        try {
            if (session != null && session.isOpen()) {
                session.getRemote().sendString(gson.toJson(message));
            }
        } catch (IOException e) { /* ignore */ }
    }

    public static void broadcast(Object message) {
        String json = gson.toJson(message);
        for (Session s : allSessions) {
            if (s.isOpen()) {
                try { s.getRemote().sendString(json); } catch (IOException e) { /* ignore */ }
            }
        }
    }

    public static void broadcastToModerators(Object message) {
        String json = gson.toJson(message);
        for (Session s : moderatorSessions) {
            if (s.isOpen()) {
                try { s.getRemote().sendString(json); } catch (IOException e) { /* ignore */ }
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

    private static Map<String, Object> buildDataMessage(String type, Map<String, Object> data) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("type", type);
        map.put("data", data);
        return map;
    }
}
