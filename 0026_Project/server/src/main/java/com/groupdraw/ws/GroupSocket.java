package com.groupdraw.ws;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.groupdraw.model.ActionLog;
import com.groupdraw.model.Group;
import com.groupdraw.model.Participant;
import com.groupdraw.service.GroupService;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.*;

public class GroupSocket extends WebSocketServer {
    private GroupService groupService;
    private Gson gson;
    private Set<WebSocket> connections;
    private Set<WebSocket> hosts;
    private String hostToken;

    public GroupSocket(int port, GroupService groupService) {
        super(new InetSocketAddress(port));
        this.groupService = groupService;
        this.gson = new Gson();
        this.connections = new HashSet<WebSocket>();
        this.hosts = new HashSet<WebSocket>();
        this.hostToken = generateHostToken();
        System.out.println("Host token: " + hostToken);
    }

    private String generateHostToken() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        System.out.println("New connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        hosts.remove(conn);
        System.out.println("Connection closed: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject msg = gson.fromJson(message, JsonObject.class);
            String type = msg.get("type").getAsString();

            if ("init".equals(type)) {
                handleInit(conn, msg);
            } else if ("claim-host".equals(type)) {
                handleClaimHost(conn, msg);
            } else if (hosts.contains(conn)) {
                handleHostMessage(conn, type, msg);
            } else {
                sendError(conn, "无权限操作，请先登录为主持人");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, "消息格式错误");
        }
    }

    private void handleInit(WebSocket conn, JsonObject msg) {
        JsonObject data = new JsonObject();
        data.addProperty("type", "state");
        data.addProperty("activityName", groupService.getActivityName());
        data.addProperty("groupCount", groupService.getGroupCount());
        data.addProperty("isHost", hosts.contains(conn));
        data.add("participants", gson.toJsonTree(groupService.getParticipants()));
        data.add("groups", gson.toJsonTree(groupService.getGroups()));
        data.add("logs", gson.toJsonTree(groupService.getActionLogs()));
        conn.send(gson.toJson(data));
    }

    private void handleClaimHost(WebSocket conn, JsonObject msg) {
        String token = msg.has("token") ? msg.get("token").getAsString() : "";
        if (hostToken.equals(token)) {
            hosts.add(conn);
            JsonObject data = new JsonObject();
            data.addProperty("type", "host-granted");
            conn.send(gson.toJson(data));
            broadcastState();
        } else {
            sendError(conn, "主持人令牌错误");
        }
    }

    private void handleHostMessage(WebSocket conn, String type, JsonObject msg) {
        if ("set-activity-name".equals(type)) {
            String name = msg.has("name") ? msg.get("name").getAsString() : "";
            groupService.setActivityName(name);
            broadcastState();
        } else if ("add-participant".equals(type)) {
            String name = msg.has("name") ? msg.get("name").getAsString() : "";
            groupService.addParticipant(name);
            broadcastState();
        } else if ("add-participants".equals(type)) {
            List<String> names = new ArrayList<String>();
            if (msg.has("names")) {
                for (int i = 0; i < msg.getAsJsonArray("names").size(); i++) {
                    names.add(msg.getAsJsonArray("names").get(i).getAsString());
                }
            }
            groupService.addParticipants(names);
            broadcastState();
        } else if ("remove-participant".equals(type)) {
            String id = msg.has("id") ? msg.get("id").getAsString() : "";
            groupService.removeParticipant(id);
            broadcastState();
        } else if ("clear-participants".equals(type)) {
            groupService.clearParticipants();
            broadcastState();
        } else if ("set-group-count".equals(type)) {
            int count = msg.has("count") ? msg.get("count").getAsInt() : 4;
            groupService.setGroupCount(count);
            broadcastState();
        } else if ("random-group".equals(type)) {
            groupService.randomGroup();
            broadcastState();
        } else if ("toggle-lock".equals(type)) {
            String groupId = msg.has("groupId") ? msg.get("groupId").getAsString() : "";
            groupService.toggleGroupLock(groupId);
            broadcastState();
        } else if ("move-participant".equals(type)) {
            String participantId = msg.has("participantId") ? msg.get("participantId").getAsString() : "";
            String targetGroupId = msg.has("targetGroupId") ? msg.get("targetGroupId").getAsString() : "";
            groupService.moveParticipant(participantId, targetGroupId);
            broadcastState();
        } else if ("undo".equals(type)) {
            groupService.undo();
            broadcastState();
        } else if ("save".equals(type)) {
            groupService.save();
            sendSuccess(conn, "已保存");
        }
    }

    private void broadcastState() {
        JsonObject data = new JsonObject();
        data.addProperty("type", "state");
        data.addProperty("activityName", groupService.getActivityName());
        data.addProperty("groupCount", groupService.getGroupCount());
        data.add("participants", gson.toJsonTree(groupService.getParticipants()));
        data.add("groups", gson.toJsonTree(groupService.getGroups()));
        data.add("logs", gson.toJsonTree(groupService.getActionLogs()));

        String json = gson.toJson(data);
        for (WebSocket conn : connections) {
            try {
                JsonObject msg = gson.fromJson(json, JsonObject.class);
                msg.addProperty("isHost", hosts.contains(conn));
                conn.send(gson.toJson(msg));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

    public String getHostToken() {
        return hostToken;
    }
}
