package ws;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.CheckInRecord;
import model.Visitor;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import service.ExpoService;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ExpoSocket extends WebSocketServer {

    private static final int PORT = 8887;
    private static final int SNAPSHOT_RECORD_LIMIT = 50;

    private static ExpoSocket instance;

    private final Set<WebSocket> connections;
    private final ExpoService expoService;
    private final Gson gson;

    private ExpoSocket() {
        super(new InetSocketAddress(PORT));
        this.connections = Collections.newSetFromMap(new ConcurrentHashMap<WebSocket, Boolean>());
        this.expoService = ExpoService.getInstance();
        this.gson = new Gson();
    }

    public static synchronized ExpoSocket getInstance() {
        if (instance == null) {
            instance = new ExpoSocket();
        }
        return instance;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        sendSnapshot(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject msgObj = JsonParser.parseString(message).getAsJsonObject();
            if (!msgObj.has("type")) {
                sendError(conn, "缺少type字段");
                return;
            }
            String type = msgObj.get("type").getAsString();
            JsonElement payload = msgObj.has("payload") ? msgObj.get("payload") : null;

            if ("checkIn".equals(type)) {
                handleCheckIn(conn, payload);
            } else if ("getStats".equals(type)) {
                sendSnapshot(conn);
            } else {
                sendError(conn, "未知的消息类型: " + type);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, "消息解析失败: " + e.getMessage());
        }
    }

    private void handleCheckIn(WebSocket conn, JsonElement payload) {
        if (payload == null || !payload.isJsonObject()) {
            sendError(conn, "checkIn需要payload");
            return;
        }
        try {
            JsonObject pl = payload.getAsJsonObject();
            String boothId = pl.has("boothId") ? pl.get("boothId").getAsString() : null;
            Visitor visitor = null;
            if (pl.has("visitor") && pl.get("visitor").isJsonObject()) {
                JsonObject vObj = pl.get("visitor").getAsJsonObject();
                String name = vObj.has("name") ? vObj.get("name").getAsString() : "";
                String phoneSuffix = vObj.has("phoneSuffix") ? vObj.get("phoneSuffix").getAsString() : "";
                visitor = new Visitor(name, phoneSuffix);
            }
            List<String> interestedProjects = new ArrayList<>();
            if (pl.has("interestedProjects") && pl.get("interestedProjects").isJsonArray()) {
                for (JsonElement elem : pl.get("interestedProjects").getAsJsonArray()) {
                    interestedProjects.add(elem.getAsString());
                }
            }

            CheckInRecord record = expoService.checkIn(boothId, visitor, interestedProjects);
            broadcastCheckInSuccess(record);
        } catch (IllegalStateException e) {
            sendError(conn, e.getMessage());
        } catch (IllegalArgumentException e) {
            sendError(conn, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, "签到失败: " + e.getMessage());
        }
    }

    private void broadcastCheckInSuccess(CheckInRecord record) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("record", record);
        payload.put("boothStats", expoService.getBoothStats());
        payload.put("projectStats", expoService.getProjectStats());
        payload.put("peakBooths", expoService.getPeakBooths());
        payload.put("recentRecords", expoService.getRecentRecords(SNAPSHOT_RECORD_LIMIT));

        Map<String, Object> message = new HashMap<>();
        message.put("type", "checkIn");
        message.put("payload", payload);

        broadcast(gson.toJson(message));
    }

    private void sendSnapshot(WebSocket conn) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("booths", expoService.getBooths());
        payload.put("records", expoService.getRecentRecords(SNAPSHOT_RECORD_LIMIT));
        payload.put("boothStats", expoService.getBoothStats());
        payload.put("projectStats", expoService.getProjectStats());
        payload.put("peakBooths", expoService.getPeakBooths());
        payload.put("availableProjects", ExpoService.DEFAULT_PROJECTS);

        Map<String, Object> message = new HashMap<>();
        message.put("type", "init");
        message.put("payload", payload);

        conn.send(gson.toJson(message));
    }

    private void sendError(WebSocket conn, String errorMsg) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "error");
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", errorMsg);
        message.put("payload", payload);
        conn.send(gson.toJson(message));
    }

    private void broadcast(String message) {
        for (WebSocket conn : connections) {
            if (conn.isOpen()) {
                conn.send(message);
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Expo WebSocket Server started on port " + PORT);
    }
}
