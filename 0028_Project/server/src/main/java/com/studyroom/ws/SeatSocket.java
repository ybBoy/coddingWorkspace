package com.studyroom.ws;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.studyroom.model.Seat;
import com.studyroom.model.SeatAction;
import com.studyroom.service.SeatService;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.List;

public class SeatSocket extends WebSocketServer {
    private final SeatService seatService;
    private final Gson gson = new Gson();

    public SeatSocket(InetSocketAddress address, SeatService seatService) {
        super(address);
        this.seatService = seatService;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        sendInitialState(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {}

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject msg = JsonParser.parseString(message).getAsJsonObject();
            String type = msg.get("type").getAsString();
            switch (type) {
                case "sit": {
                    int seatId = msg.get("seatId").getAsInt();
                    String nickname = msg.get("nickname").getAsString();
                    Seat result = seatService.sit(seatId, nickname);
                    if (result != null) {
                        broadcastUpdate();
                    } else {
                        sendError(conn, "无法入座，座位可能已被占用");
                    }
                    break;
                }
                case "away": {
                    int seatId = msg.get("seatId").getAsInt();
                    String nickname = msg.get("nickname").getAsString();
                    Seat result = seatService.away(seatId, nickname);
                    if (result != null) {
                        broadcastUpdate();
                    } else {
                        sendError(conn, "无法暂离，请确认座位归属");
                    }
                    break;
                }
                case "leave": {
                    int seatId = msg.get("seatId").getAsInt();
                    String nickname = msg.get("nickname").getAsString();
                    Seat result = seatService.leave(seatId, nickname);
                    if (result != null) {
                        broadcastUpdate();
                    } else {
                        sendError(conn, "无法离开，请确认座位归属");
                    }
                    break;
                }
                case "forceRelease": {
                    int seatId = msg.get("seatId").getAsInt();
                    Seat result = seatService.forceRelease(seatId);
                    if (result != null) {
                        broadcastUpdate();
                    } else {
                        sendError(conn, "无法释放座位");
                    }
                    break;
                }
                default:
                    sendError(conn, "未知操作: " + type);
            }
        } catch (Exception e) {
            sendError(conn, "消息格式错误: " + e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server started on " + getAddress());
    }

    private void sendInitialState(WebSocket conn) {
        JsonObject state = new JsonObject();
        state.addProperty("type", "init");
        state.add("seats", gson.toJsonTree(seatService.getAllSeats()));
        state.add("actions", gson.toJsonTree(seatService.getRecentActions(10)));
        conn.send(gson.toJson(state));
    }

    public void broadcastUpdate() {
        JsonObject state = new JsonObject();
        state.addProperty("type", "update");
        state.add("seats", gson.toJsonTree(seatService.getAllSeats()));
        state.add("actions", gson.toJsonTree(seatService.getRecentActions(10)));
        String json = gson.toJson(state);
        broadcast(json);
    }

    private void sendError(WebSocket conn, String message) {
        JsonObject err = new JsonObject();
        err.addProperty("type", "error");
        err.addProperty("message", message);
        conn.send(gson.toJson(err));
    }
}
