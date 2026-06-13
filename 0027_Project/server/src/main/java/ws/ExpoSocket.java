package ws;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.Booth;
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
                sendError(conn, "缺少type字段", null);
                return;
            }
            String type = msgObj.get("type").getAsString();
            JsonElement payload = msgObj.has("payload") ? msgObj.get("payload") : null;

            if ("checkIn".equals(type)) {
                handleCheckIn(conn, payload);
            } else if ("getStats".equals(type)) {
                sendSnapshot(conn);
            } else if ("getRecordsByRange".equals(type)) {
                handleGetRecordsByRange(conn, payload);
            } else if ("exportRecords".equals(type)) {
                handleExportRecords(conn);
            } else if ("backupData".equals(type)) {
                handleBackupData(conn);
            } else if ("clearAllData".equals(type)) {
                handleClearAllData(conn, payload);
            } else if ("addBooth".equals(type)) {
                handleAddBooth(conn, payload);
            } else if ("updateBooth".equals(type)) {
                handleUpdateBooth(conn, payload);
            } else if ("deleteBooth".equals(type)) {
                handleDeleteBooth(conn, payload);
            } else if ("addProject".equals(type)) {
                handleAddProject(conn, payload);
            } else if ("updateProject".equals(type)) {
                handleUpdateProject(conn, payload);
            } else if ("deleteProject".equals(type)) {
                handleDeleteProject(conn, payload);
            } else {
                sendError(conn, "未知的消息类型: " + type, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, "消息解析失败: " + e.getMessage(), null);
        }
    }

    private void handleCheckIn(WebSocket conn, JsonElement payload) {
        if (payload == null || !payload.isJsonObject()) {
            sendError(conn, "checkIn需要payload", null);
            return;
        }
        String requestId = null;
        try {
            JsonObject pl = payload.getAsJsonObject();
            requestId = pl.has("requestId") ? pl.get("requestId").getAsString() : null;
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

            sendCheckInAck(conn, requestId, record);
            broadcastCheckInSuccess(record);
        } catch (IllegalStateException e) {
            sendError(conn, e.getMessage(), requestId);
        } catch (IllegalArgumentException e) {
            sendError(conn, e.getMessage(), requestId);
        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, "签到失败: " + e.getMessage(), requestId);
        }
    }

    private void sendCheckInAck(WebSocket conn, String requestId, CheckInRecord record) {
        Map<String, Object> payload = new HashMap<>();
        if (requestId != null) {
            payload.put("requestId", requestId);
        }
        payload.put("record", record);

        Map<String, Object> message = new HashMap<>();
        message.put("type", "checkInAck");
        message.put("payload", payload);
        conn.send(gson.toJson(message));
    }

    private void handleExportRecords(WebSocket conn) {
        try {
            List<CheckInRecord> allRecords = expoService.getAllRecords();
            Map<String, Object> payload = new HashMap<>();
            payload.put("records", allRecords);

            Map<String, Object> message = new HashMap<>();
            message.put("type", "exportRecords");
            message.put("payload", payload);
            conn.send(gson.toJson(message));
        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, "导出记录失败: " + e.getMessage(), null);
        }
    }

    private void handleBackupData(WebSocket conn) {
        try {
            String backupJson = expoService.exportBackup();

            Map<String, Object> payload = new HashMap<>();
            payload.put("backupJson", backupJson);
            payload.put("filename", "expo_backup_" + System.currentTimeMillis() + ".json");

            Map<String, Object> message = new HashMap<>();
            message.put("type", "backupData");
            message.put("payload", payload);
            conn.send(gson.toJson(message));
        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, "备份数据失败: " + e.getMessage(), null);
        }
    }

    private void handleClearAllData(WebSocket conn, JsonElement payload) {
        try {
            String confirm = null;
            if (payload != null && payload.isJsonObject()) {
                JsonObject pl = payload.getAsJsonObject();
                confirm = pl.has("confirm") ? pl.get("confirm").getAsString() : null;
            }
            if (!"CLEAR_ALL".equals(confirm)) {
                sendError(conn, "清场确认不匹配，需传入 CLEAR_ALL", null);
                return;
            }
            expoService.clearAllData();
            Map<String, Object> message = new HashMap<>();
            message.put("type", "clearAllDataAck");
            message.put("payload", Collections.singletonMap("success", true));
            conn.send(gson.toJson(message));
            broadcastAllStats();
        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, "清场失败: " + e.getMessage(), null);
        }
    }

    private void handleAddBooth(WebSocket conn, JsonElement payload) {
        try {
            if (payload == null || !payload.isJsonObject()) {
                sendError(conn, "addBooth需要payload", null);
                return;
            }
            JsonObject pl = payload.getAsJsonObject();
            String name = pl.has("name") ? pl.get("name").getAsString() : null;
            String description = pl.has("description") ? pl.get("description").getAsString() : "";
            Booth booth = expoService.addBooth(name, description);
            sendConfigAck(conn, "addBooth", booth);
            broadcastAllStats();
        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, "新增展位失败: " + e.getMessage(), null);
        }
    }

    private void handleUpdateBooth(WebSocket conn, JsonElement payload) {
        try {
            if (payload == null || !payload.isJsonObject()) {
                sendError(conn, "updateBooth需要payload", null);
                return;
            }
            JsonObject pl = payload.getAsJsonObject();
            String id = pl.has("id") ? pl.get("id").getAsString() : null;
            String name = pl.has("name") ? pl.get("name").getAsString() : null;
            String description = pl.has("description") ? pl.get("description").getAsString() : null;
            Boolean disabled = pl.has("disabled") ? pl.get("disabled").getAsBoolean() : null;
            Booth booth = expoService.updateBooth(id, name, description, disabled);
            sendConfigAck(conn, "updateBooth", booth);
            broadcastAllStats();
        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, "编辑展位失败: " + e.getMessage(), null);
        }
    }

    private void handleDeleteBooth(WebSocket conn, JsonElement payload) {
        try {
            if (payload == null || !payload.isJsonObject()) {
                sendError(conn, "deleteBooth需要payload", null);
                return;
            }
            JsonObject pl = payload.getAsJsonObject();
            String id = pl.has("id") ? pl.get("id").getAsString() : null;
            expoService.deleteBooth(id);
            sendConfigAck(conn, "deleteBooth", Collections.singletonMap("id", id));
            broadcastAllStats();
        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, "删除展位失败: " + e.getMessage(), null);
        }
    }

    private void handleAddProject(WebSocket conn, JsonElement payload) {
        try {
            if (payload == null || !payload.isJsonObject()) {
                sendError(conn, "addProject需要payload", null);
                return;
            }
            JsonObject pl = payload.getAsJsonObject();
            String name = pl.has("name") ? pl.get("name").getAsString() : null;
            expoService.addProject(name);
            sendConfigAck(conn, "addProject", Collections.singletonMap("name", name));
            broadcastAllStats();
        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, "新增项目失败: " + e.getMessage(), null);
        }
    }

    private void handleUpdateProject(WebSocket conn, JsonElement payload) {
        try {
            if (payload == null || !payload.isJsonObject()) {
                sendError(conn, "updateProject需要payload", null);
                return;
            }
            JsonObject pl = payload.getAsJsonObject();
            String oldName = pl.has("oldName") ? pl.get("oldName").getAsString() : null;
            String newName = pl.has("newName") ? pl.get("newName").getAsString() : null;
            expoService.updateProject(oldName, newName);
            Map<String, String> res = new HashMap<>();
            res.put("oldName", oldName);
            res.put("newName", newName);
            sendConfigAck(conn, "updateProject", res);
            broadcastAllStats();
        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, "编辑项目失败: " + e.getMessage(), null);
        }
    }

    private void handleDeleteProject(WebSocket conn, JsonElement payload) {
        try {
            if (payload == null || !payload.isJsonObject()) {
                sendError(conn, "deleteProject需要payload", null);
                return;
            }
            JsonObject pl = payload.getAsJsonObject();
            String name = pl.has("name") ? pl.get("name").getAsString() : null;
            expoService.deleteProject(name);
            sendConfigAck(conn, "deleteProject", Collections.singletonMap("name", name));
            broadcastAllStats();
        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, "删除项目失败: " + e.getMessage(), null);
        }
    }

    private void sendConfigAck(WebSocket conn, String action, Object data) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", action);
        payload.put("data", data);
        payload.put("allBooths", expoService.getAllBooths());
        payload.put("projects", expoService.getProjects());
        payload.put("booths", expoService.getBooths());

        Map<String, Object> message = new HashMap<>();
        message.put("type", "configAck");
        message.put("payload", payload);
        conn.send(gson.toJson(message));
    }

    private void handleGetRecordsByRange(WebSocket conn, JsonElement payload) {
        if (payload == null || !payload.isJsonObject()) {
            sendError(conn, "getRecordsByRange需要payload", null);
            return;
        }
        try {
            JsonObject pl = payload.getAsJsonObject();
            String range = pl.has("range") ? pl.get("range").getAsString() : "all";
            long startTime = 0;
            long endTime = System.currentTimeMillis();

            if ("10min".equals(range)) {
                startTime = endTime - 10L * 60L * 1000L;
            } else if ("today".equals(range)) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                startTime = cal.getTimeInMillis();
            } else {
                startTime = 0;
            }

            List<CheckInRecord> records = expoService.getRecordsByTimeRange(startTime, endTime);
            Map<String, Long> boothStats = new HashMap<>();
            Map<String, Long> projectStats = new HashMap<>();
            for (CheckInRecord record : records) {
                String bid = record.getBoothId();
                boothStats.put(bid, boothStats.containsKey(bid) ? boothStats.get(bid) + 1 : 1L);
                List<String> ps = record.getInterestedProjects();
                if (ps != null) {
                    for (String p : ps) {
                        projectStats.put(p, projectStats.containsKey(p) ? projectStats.get(p) + 1 : 1L);
                    }
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("records", records);
            result.put("boothStats", boothStats);
            result.put("projectStats", projectStats);
            result.put("range", range);

            Map<String, Object> message = new HashMap<>();
            message.put("type", "rangeStats");
            message.put("payload", result);
            conn.send(gson.toJson(message));
        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, "查询失败: " + e.getMessage(), null);
        }
    }

    private void broadcastCheckInSuccess(CheckInRecord record) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("record", record);
        payload.put("boothStats", expoService.getBoothStats());
        payload.put("projectStats", expoService.getProjectStats());
        payload.put("todayBoothStats", expoService.getTodayBoothStats());
        payload.put("todayProjectStats", expoService.getTodayProjectStats());
        payload.put("todayTotal", expoService.getTodayTotal());
        payload.put("peakBooths", expoService.getPeakBooths());
        payload.put("recentRecords", expoService.getRecentRecords(SNAPSHOT_RECORD_LIMIT));

        Map<String, Object> message = new HashMap<>();
        message.put("type", "checkIn");
        message.put("payload", payload);

        broadcastAll(gson.toJson(message));
    }

    private void broadcastAllStats() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("booths", expoService.getBooths());
        payload.put("allBooths", expoService.getAllBooths());
        payload.put("projects", expoService.getProjects());
        payload.put("records", expoService.getRecentRecords(SNAPSHOT_RECORD_LIMIT));
        payload.put("boothStats", expoService.getBoothStats());
        payload.put("projectStats", expoService.getProjectStats());
        payload.put("todayBoothStats", expoService.getTodayBoothStats());
        payload.put("todayProjectStats", expoService.getTodayProjectStats());
        payload.put("todayTotal", expoService.getTodayTotal());
        payload.put("peakBooths", expoService.getPeakBooths());
        payload.put("availableProjects", expoService.getProjects());

        Map<String, Object> message = new HashMap<>();
        message.put("type", "init");
        message.put("payload", payload);

        broadcastAll(gson.toJson(message));
    }

    private void sendSnapshot(WebSocket conn) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("booths", expoService.getBooths());
        payload.put("allBooths", expoService.getAllBooths());
        payload.put("projects", expoService.getProjects());
        payload.put("records", expoService.getRecentRecords(SNAPSHOT_RECORD_LIMIT));
        payload.put("boothStats", expoService.getBoothStats());
        payload.put("projectStats", expoService.getProjectStats());
        payload.put("todayBoothStats", expoService.getTodayBoothStats());
        payload.put("todayProjectStats", expoService.getTodayProjectStats());
        payload.put("todayTotal", expoService.getTodayTotal());
        payload.put("peakBooths", expoService.getPeakBooths());
        payload.put("availableProjects", expoService.getProjects());

        Map<String, Object> message = new HashMap<>();
        message.put("type", "init");
        message.put("payload", payload);

        conn.send(gson.toJson(message));
    }

    private void sendError(WebSocket conn, String errorMsg, String requestId) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "error");
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", errorMsg);
        if (requestId != null) {
            payload.put("requestId", requestId);
        }
        message.put("payload", payload);
        conn.send(gson.toJson(message));
    }

    private void broadcastAll(String message) {
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
