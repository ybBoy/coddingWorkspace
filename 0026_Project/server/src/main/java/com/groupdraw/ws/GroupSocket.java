package com.groupdraw.ws;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.groupdraw.model.*;
import com.groupdraw.service.GroupService;
import com.groupdraw.store.JsonStore;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GroupSocket extends WebSocketServer {
    private JsonStore jsonStore;
    private Gson gson;
    private Map<String, Set<WebSocket>> roomConnections;
    private Map<WebSocket, String> connectionRoom;
    private Map<WebSocket, Boolean> connectionHost;
    private Map<String, GroupService> roomServices;

    public GroupSocket(int port, JsonStore jsonStore) {
        super(new InetSocketAddress(port));
        this.jsonStore = jsonStore;
        this.gson = new Gson();
        this.roomConnections = new ConcurrentHashMap<String, Set<WebSocket>>();
        this.connectionRoom = new ConcurrentHashMap<WebSocket, String>();
        this.connectionHost = new ConcurrentHashMap<WebSocket, Boolean>();
        this.roomServices = new ConcurrentHashMap<String, GroupService>();

        for (Room room : jsonStore.getAllRooms()) {
            GroupService service = new GroupService(jsonStore, room);
            roomServices.put(room.getCode(), service);
            roomConnections.put(room.getCode(), new HashSet<WebSocket>());
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String roomCode = connectionRoom.remove(conn);
        connectionHost.remove(conn);
        if (roomCode != null) {
            Set<WebSocket> conns = roomConnections.get(roomCode);
            if (conns != null) {
                conns.remove(conn);
            }
        }
        System.out.println("Connection closed: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject msg = gson.fromJson(message, JsonObject.class);
            String type = msg.get("type").getAsString();

            if ("create-room".equals(type)) {
                handleCreateRoom(conn, msg);
            } else if ("join-room".equals(type)) {
                handleJoinRoom(conn, msg);
            } else if ("self-register".equals(type)) {
                handleSelfRegister(conn, msg);
            } else if ("init".equals(type)) {
                handleInit(conn);
            } else if ("claim-host".equals(type)) {
                handleClaimHost(conn, msg);
            } else {
                String roomCode = connectionRoom.get(conn);
                Boolean isHost = connectionHost.get(conn);
                if (roomCode == null) {
                    sendError(conn, "请先加入房间");
                    return;
                }
                if (isHost == null || !isHost) {
                    sendError(conn, "无权限操作，请先登录为主持人");
                    return;
                }
                handleHostMessage(conn, roomCode, type, msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, "消息格式错误");
        }
    }

    private void handleCreateRoom(WebSocket conn, JsonObject msg) {
        String activityName = msg.has("activityName") ? msg.get("activityName").getAsString() : "活动抽签分组";
        String code = generateRoomCode();
        String hostToken = generateHostToken();

        Room room = new Room(code, activityName, hostToken);
        jsonStore.putRoom(room);

        GroupService service = new GroupService(jsonStore, room);
        roomServices.put(code, service);
        roomConnections.put(code, new HashSet<WebSocket>());

        connectionRoom.put(conn, code);
        connectionHost.put(conn, true);
        roomConnections.get(code).add(conn);

        JsonObject data = new JsonObject();
        data.addProperty("type", "room-created");
        data.addProperty("roomCode", code);
        data.addProperty("hostToken", hostToken);
        conn.send(gson.toJson(data));

        sendStateToConnection(conn, code, true);
    }

    private void handleJoinRoom(WebSocket conn, JsonObject msg) {
        String code = msg.has("roomCode") ? msg.get("roomCode").getAsString().toUpperCase() : "";

        if (!jsonStore.hasRoom(code)) {
            sendError(conn, "房间不存在，请检查房间码");
            return;
        }

        String oldRoom = connectionRoom.get(conn);
        if (oldRoom != null) {
            Set<WebSocket> oldConns = roomConnections.get(oldRoom);
            if (oldConns != null) {
                oldConns.remove(conn);
            }
        }

        connectionRoom.put(conn, code);
        connectionHost.put(conn, false);

        Set<WebSocket> conns = roomConnections.get(code);
        if (conns != null) {
            conns.add(conn);
        }

        sendStateToConnection(conn, code, false);
    }

    private void handleSelfRegister(WebSocket conn, JsonObject msg) {
        String roomCode = connectionRoom.get(conn);
        if (roomCode == null) {
            String code = msg.has("roomCode") ? msg.get("roomCode").getAsString().toUpperCase() : "";
            if (!jsonStore.hasRoom(code)) {
                sendError(conn, "房间不存在");
                return;
            }
            roomCode = code;
            connectionRoom.put(conn, roomCode);
            connectionHost.put(conn, false);
            Set<WebSocket> conns = roomConnections.get(roomCode);
            if (conns != null) {
                conns.add(conn);
            }
        }

        GroupService service = getOrCreateService(roomCode);
        if (service == null) {
            sendError(conn, "房间不存在");
            return;
        }

        String name = msg.has("name") ? msg.get("name").getAsString() : "";
        String gender = msg.has("gender") ? msg.get("gender").getAsString() : null;
        String department = msg.has("department") ? msg.get("department").getAsString() : null;

        Participant p = service.addParticipant(name, gender, department, 0, null, true);
        if (p != null) {
            JsonObject data = new JsonObject();
            data.addProperty("type", "self-registered");
            data.addProperty("participantId", p.getId());
            conn.send(gson.toJson(data));
        }

        broadcastRoomState(roomCode);
    }

    private void handleInit(WebSocket conn) {
        String roomCode = connectionRoom.get(conn);
        if (roomCode != null) {
            Boolean isHost = connectionHost.get(conn);
            sendStateToConnection(conn, roomCode, isHost != null && isHost);
        }
    }

    private void handleClaimHost(WebSocket conn, JsonObject msg) {
        String roomCode = connectionRoom.get(conn);
        if (roomCode == null) {
            sendError(conn, "请先加入房间");
            return;
        }

        GroupService service = getOrCreateService(roomCode);
        if (service == null) {
            sendError(conn, "房间不存在");
            return;
        }

        String token = msg.has("token") ? msg.get("token").getAsString() : "";
        if (service.verifyHost(token)) {
            connectionHost.put(conn, true);
            JsonObject data = new JsonObject();
            data.addProperty("type", "host-granted");
            conn.send(gson.toJson(data));
            broadcastRoomState(roomCode);
        } else {
            sendError(conn, "主持人令牌错误");
        }
    }

    private void handleHostMessage(WebSocket conn, String roomCode, String type, JsonObject msg) {
        GroupService service = getOrCreateService(roomCode);
        if (service == null) {
            sendError(conn, "房间不存在");
            return;
        }

        if ("set-activity-name".equals(type)) {
            String name = msg.has("name") ? msg.get("name").getAsString() : "";
            service.setActivityName(name);
            broadcastRoomState(roomCode);
        } else if ("add-participant".equals(type)) {
            String name = msg.has("name") ? msg.get("name").getAsString() : "";
            String gender = msg.has("gender") ? msg.get("gender").getAsString() : null;
            String department = msg.has("department") ? msg.get("department").getAsString() : null;
            int skill = msg.has("skill") ? msg.get("skill").getAsInt() : 0;
            String tag = msg.has("tag") ? msg.get("tag").getAsString() : null;
            service.addParticipant(name, gender, department, skill, tag, false);
            broadcastRoomState(roomCode);
        } else if ("add-participants".equals(type)) {
            List<String> names = new ArrayList<String>();
            if (msg.has("names")) {
                for (int i = 0; i < msg.getAsJsonArray("names").size(); i++) {
                    names.add(msg.getAsJsonArray("names").get(i).getAsString());
                }
            }
            service.addParticipants(names);
            broadcastRoomState(roomCode);
        } else if ("update-participant".equals(type)) {
            String id = msg.has("id") ? msg.get("id").getAsString() : "";
            String gender = msg.has("gender") ? msg.get("gender").getAsString() : null;
            String department = msg.has("department") ? msg.get("department").getAsString() : null;
            int skill = msg.has("skill") ? msg.get("skill").getAsInt() : 0;
            String tag = msg.has("tag") ? msg.get("tag").getAsString() : null;
            service.updateParticipant(id, gender, department, skill, tag);
            broadcastRoomState(roomCode);
        } else if ("remove-participant".equals(type)) {
            String id = msg.has("id") ? msg.get("id").getAsString() : "";
            service.removeParticipant(id);
            broadcastRoomState(roomCode);
        } else if ("clear-participants".equals(type)) {
            service.clearParticipants();
            broadcastRoomState(roomCode);
        } else if ("set-group-count".equals(type)) {
            int count = msg.has("count") ? msg.get("count").getAsInt() : 4;
            service.setGroupCount(count);
            broadcastRoomState(roomCode);
        } else if ("random-group".equals(type)) {
            service.randomGroup();
            broadcastRoomState(roomCode);
        } else if ("toggle-lock".equals(type)) {
            String groupId = msg.has("groupId") ? msg.get("groupId").getAsString() : "";
            service.toggleGroupLock(groupId);
            broadcastRoomState(roomCode);
        } else if ("move-participant".equals(type)) {
            String participantId = msg.has("participantId") ? msg.get("participantId").getAsString() : "";
            String targetGroupId = msg.has("targetGroupId") ? msg.get("targetGroupId").getAsString() : "";
            service.moveParticipant(participantId, targetGroupId);
            broadcastRoomState(roomCode);
        } else if ("undo".equals(type)) {
            service.undo();
            broadcastRoomState(roomCode);
        } else if ("restore-version".equals(type)) {
            int versionIndex = msg.has("versionIndex") ? msg.get("versionIndex").getAsInt() : -1;
            service.restoreVersion(versionIndex);
            broadcastRoomState(roomCode);
        } else if ("set-rules".equals(type)) {
            List<GroupRule> rules = new ArrayList<GroupRule>();
            if (msg.has("rules")) {
                JsonArray rulesArr = msg.getAsJsonArray("rules");
                for (int i = 0; i < rulesArr.size(); i++) {
                    JsonObject r = rulesArr.get(i).getAsJsonObject();
                    rules.add(new GroupRule(
                        r.has("type") ? r.get("type").getAsString() : "",
                        r.has("value") ? r.get("value").getAsString() : ""
                    ));
                }
            }
            service.setRules(rules);
            broadcastRoomState(roomCode);
        } else if ("save".equals(type)) {
            service.save();
            sendSuccess(conn, "已保存");
        } else if ("export-csv".equals(type)) {
            String csv = buildExportCsv(service);
            JsonObject data = new JsonObject();
            data.addProperty("type", "export-data");
            data.addProperty("format", "csv");
            data.addProperty("content", csv);
            conn.send(gson.toJson(data));
        }
    }

    private String buildExportCsv(GroupService service) {
        StringBuilder sb = new StringBuilder();
        sb.append("分组,姓名,性别,部门,能力值\n");

        for (Group g : service.getGroups()) {
            for (String pid : g.getParticipantIds()) {
                Participant p = null;
                for (Participant pp : service.getParticipants()) {
                    if (pp.getId().equals(pid)) {
                        p = pp;
                        break;
                    }
                }
                if (p != null) {
                    sb.append(g.getName()).append(",");
                    sb.append(p.getName()).append(",");
                    sb.append(p.getGender() != null ? p.getGender() : "").append(",");
                    sb.append(p.getDepartment() != null ? p.getDepartment() : "").append(",");
                    sb.append(p.getSkill()).append("\n");
                }
            }
        }

        for (Participant p : service.getParticipants()) {
            if (p.getGroupId() == null) {
                sb.append("未分组,").append(p.getName()).append(",");
                sb.append(p.getGender() != null ? p.getGender() : "").append(",");
                sb.append(p.getDepartment() != null ? p.getDepartment() : "").append(",");
                sb.append(p.getSkill()).append("\n");
            }
        }

        return sb.toString();
    }

    private GroupService getOrCreateService(String roomCode) {
        GroupService service = roomServices.get(roomCode);
        if (service == null) {
            Room room = jsonStore.getRoom(roomCode);
            if (room != null) {
                service = new GroupService(jsonStore, room);
                roomServices.put(roomCode, service);
            }
        }
        return service;
    }

    private void sendStateToConnection(WebSocket conn, String roomCode, boolean isHost) {
        GroupService service = getOrCreateService(roomCode);
        if (service == null) return;

        JsonObject data = new JsonObject();
        data.addProperty("type", "state");
        data.addProperty("activityName", service.getActivityName());
        data.addProperty("groupCount", service.getGroupCount());
        data.addProperty("roomCode", roomCode);
        data.addProperty("isHost", isHost);
        data.add("participants", gson.toJsonTree(service.getParticipants()));
        data.add("groups", gson.toJsonTree(service.getGroups()));
        data.add("logs", gson.toJsonTree(service.getActionLogs()));
        data.add("rules", gson.toJsonTree(service.getRules()));
        conn.send(gson.toJson(data));
    }

    private void broadcastRoomState(String roomCode) {
        GroupService service = getOrCreateService(roomCode);
        if (service == null) return;

        Set<WebSocket> conns = roomConnections.get(roomCode);
        if (conns == null || conns.isEmpty()) return;

        JsonObject data = new JsonObject();
        data.addProperty("type", "state");
        data.addProperty("activityName", service.getActivityName());
        data.addProperty("groupCount", service.getGroupCount());
        data.addProperty("roomCode", roomCode);
        data.add("participants", gson.toJsonTree(service.getParticipants()));
        data.add("groups", gson.toJsonTree(service.getGroups()));
        data.add("logs", gson.toJsonTree(service.getActionLogs()));
        data.add("rules", gson.toJsonTree(service.getRules()));

        String json = gson.toJson(data);
        for (WebSocket conn : new HashSet<WebSocket>(conns)) {
            try {
                JsonObject msg = gson.fromJson(json, JsonObject.class);
                Boolean isHost = connectionHost.get(conn);
                msg.addProperty("isHost", isHost != null && isHost);
                conn.send(gson.toJson(msg));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        String code = sb.toString();
        if (jsonStore.hasRoom(code)) {
            return generateRoomCode();
        }
        return code;
    }

    private String generateHostToken() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private void sendError(WebSocket conn, String error) {
        JsonObject data = new JsonObject();
        data.addProperty("type", "error");
        data.addProperty("message", error);
        conn.send(gson.toJson(data));
    }

    private void sendSuccess(WebSocket conn, String message) {
        JsonObject data = new JsonObject();
        data.addProperty("type", "success");
        data.addProperty("message", message);
        conn.send(gson.toJson(data));
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server started on port " + getPort());
    }
}
