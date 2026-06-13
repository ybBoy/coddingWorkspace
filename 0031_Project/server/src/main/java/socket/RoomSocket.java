package socket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import core.RoomService;
import entity.Room;
import entity.RoomLog;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomSocket extends WebSocketServer {

    private final RoomService roomService;
    private final Gson gson;

    public RoomSocket(int port, RoomService roomService) {
        super(new InetSocketAddress(port));
        this.roomService = roomService;
        this.gson = new GsonBuilder().create();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("[WS] Client connected: " + conn.getRemoteSocketAddress());
        sendFullSync(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("[WS] Client disconnected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = gson.fromJson(message, Map.class);
            String type = (String) msg.get("type");
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) msg.get("payload");

            if (type == null || payload == null) {
                return;
            }

            String roomId = (String) payload.get("roomId");
            if (roomId == null) {
                return;
            }

            boolean handled = true;

            switch (type) {
                case "CHECK_IN":
                    String guestName = (String) payload.get("guestName");
                    Double expectedTime = (Double) payload.get("expectedCheckOutTime");
                    if (guestName != null && expectedTime != null) {
                        roomService.checkIn(roomId, guestName, expectedTime.longValue());
                    }
                    break;
                case "CHECK_OUT":
                    roomService.checkOut(roomId);
                    break;
                case "CLEAN_ROOM":
                    roomService.cleanRoom(roomId);
                    break;
                case "MARK_MAINTENANCE":
                    roomService.markMaintenance(roomId);
                    break;
                case "REPAIR_DONE":
                    roomService.repairDone(roomId);
                    break;
                default:
                    handled = false;
            }

            if (handled) {
                broadcastUpdate();
            }
        } catch (Exception e) {
            System.err.println("[WS] Message handling error: " + e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[WS] Error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("[WS] Server started on port " + getPort());
    }

    private void sendFullSync(WebSocket conn) {
        List<Room> rooms = roomService.getAllRooms();
        List<RoomLog> logs = roomService.getRecentLogs(10);

        Map<String, Object> payload = new HashMap<>();
        payload.put("rooms", rooms);
        payload.put("logs", logs);

        Map<String, Object> message = new HashMap<>();
        message.put("type", "FULL_SYNC");
        message.put("payload", payload);

        conn.send(gson.toJson(message));
    }

    private void broadcastUpdate() {
        List<Room> rooms = roomService.getAllRooms();
        List<RoomLog> logs = roomService.getRecentLogs(10);

        Map<String, Object> roomsPayload = new HashMap<>();
        roomsPayload.put("rooms", rooms);
        Map<String, Object> roomsMsg = new HashMap<>();
        roomsMsg.put("type", "ROOMS_UPDATE");
        roomsMsg.put("payload", roomsPayload);
        broadcast(gson.toJson(roomsMsg));

        Map<String, Object> logsPayload = new HashMap<>();
        logsPayload.put("logs", logs);
        Map<String, Object> logsMsg = new HashMap<>();
        logsMsg.put("type", "LOGS_UPDATE");
        logsMsg.put("payload", logsPayload);
        broadcast(gson.toJson(logsMsg));
    }
}
