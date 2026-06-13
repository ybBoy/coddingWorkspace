package socket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import core.RoomService;
import entity.AlertItem;
import entity.Operator;
import entity.Room;
import entity.RoomLog;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RoomSocket extends WebSocketServer {

    private final RoomService roomService;
    private final Gson gson;
    private final ScheduledExecutorService alertScheduler;

    public RoomSocket(int port, RoomService roomService) {
        super(new InetSocketAddress(port));
        this.roomService = roomService;
        this.gson = new GsonBuilder().create();
        this.alertScheduler = Executors.newSingleThreadScheduledExecutor();
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
    @SuppressWarnings("unchecked")
    public void onMessage(WebSocket conn, String message) {
        try {
            Map<String, Object> msg = gson.fromJson(message, Map.class);
            String type = (String) msg.get("type");
            Map<String, Object> payload = (Map<String, Object>) msg.get("payload");

            if (type == null) {
                return;
            }

            boolean needBroadcast = handleMessage(type, payload, conn);

            if (needBroadcast) {
                broadcastUpdate();
            }
        } catch (Exception e) {
            System.err.println("[WS] Message handling error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private boolean handleMessage(String type, Map<String, Object> payload, WebSocket conn) {
        switch (type) {
            case "CHECK_IN": {
                String roomId = (String) payload.get("roomId");
                String guestName = (String) payload.get("guestName");
                Double expectedTime = getAsLong(payload.get("expectedCheckOutTime"));
                Double price = getAsDouble(payload.get("price"));
                Double deposit = getAsDouble(payload.get("deposit"));
                if (roomId != null && guestName != null && expectedTime != null
                        && price != null && deposit != null) {
                    roomService.checkIn(roomId, guestName, expectedTime.longValue(),
                            price.doubleValue(), deposit.doubleValue());
                    return true;
                }
                return false;
            }
            case "CHECK_OUT": {
                String roomId = (String) payload.get("roomId");
                Boolean settle = (Boolean) payload.get("settle");
                if (roomId != null) {
                    roomService.checkOut(roomId, settle != null && settle);
                    return true;
                }
                return false;
            }
            case "CLEAN_ROOM": {
                String roomId = (String) payload.get("roomId");
                if (roomId != null) {
                    roomService.cleanRoom(roomId);
                    return true;
                }
                return false;
            }
            case "MARK_MAINTENANCE": {
                String roomId = (String) payload.get("roomId");
                String remark = (String) payload.get("remark");
                if (roomId != null) {
                    roomService.markMaintenance(roomId, remark);
                    return true;
                }
                return false;
            }
            case "REPAIR_DONE": {
                String roomId = (String) payload.get("roomId");
                if (roomId != null) {
                    roomService.repairDone(roomId);
                    return true;
                }
                return false;
            }
            case "BATCH_CLEAN_BY_FLOOR": {
                Double floor = getAsLong(payload.get("floor"));
                if (floor != null) {
                    int count = roomService.batchCleanByFloor(floor.intValue());
                    System.out.println("[WS] Batch cleaned " + count + " rooms on floor " + floor);
                    return count > 0;
                }
                return false;
            }
            case "BATCH_MARK_DIRTY_BY_FLOOR": {
                Double floor = getAsLong(payload.get("floor"));
                if (floor != null) {
                    int count = roomService.batchMarkDirtyByFloor(floor.intValue());
                    System.out.println("[WS] Batch marked " + count + " rooms dirty on floor " + floor);
                    return count > 0;
                }
                return false;
            }
            case "GET_ROOM_DETAIL": {
                String roomId = (String) payload.get("roomId");
                if (roomId != null) {
                    Map<String, Object> detail = roomService.getRoomDetail(roomId);
                    sendSingleMessage(conn, "ROOM_DETAIL", detail);
                }
                return false;
            }
            case "SET_OPERATOR": {
                String operatorName = (String) payload.get("operatorName");
                if (operatorName != null) {
                    roomService.setCurrentOperatorName(operatorName);
                    broadcastUpdate();
                }
                return false;
            }
            case "GET_OPERATORS": {
                List<Operator> operators = roomService.getOperators();
                Map<String, Object> data = new HashMap<>();
                data.put("operators", operators);
                data.put("currentOperator", roomService.getCurrentOperatorName());
                sendSingleMessage(conn, "OPERATORS_LIST", data);
                return false;
            }
            case "EXPORT_STAY_RECORDS": {
                String csv = roomService.exportStayRecordsCsv();
                Map<String, Object> data = new HashMap<>();
                data.put("filename", "入住记录_" + System.currentTimeMillis() + ".csv");
                data.put("content", csv);
                sendSingleMessage(conn, "EXPORT_DATA", data);
                return false;
            }
            case "EXPORT_LOGS": {
                String csv = roomService.exportLogsCsv();
                Map<String, Object> data = new HashMap<>();
                data.put("filename", "操作日志_" + System.currentTimeMillis() + ".csv");
                data.put("content", csv);
                sendSingleMessage(conn, "EXPORT_DATA", data);
                return false;
            }
            case "ADD_ROOM": {
                String roomNo = (String) payload.get("roomNo");
                Double floor = getAsLong(payload.get("floor"));
                String roomType = (String) payload.get("type");
                Double price = getAsDouble(payload.get("defaultPrice"));
                if (roomNo != null && floor != null && roomType != null && price != null) {
                    Room room = roomService.addRoom(roomNo, floor.intValue(), roomType, price.doubleValue());
                    if (room != null) {
                        return true;
                    }
                }
                return false;
            }
            case "UPDATE_ROOM": {
                String roomId = (String) payload.get("roomId");
                String roomNo = (String) payload.get("roomNo");
                Double floor = getAsLong(payload.get("floor"));
                String roomType = (String) payload.get("type");
                Double price = getAsDouble(payload.get("defaultPrice"));
                if (roomId != null && roomNo != null && floor != null
                        && roomType != null && price != null) {
                    Room room = roomService.updateRoom(roomId, roomNo, floor.intValue(),
                            roomType, price.doubleValue());
                    if (room != null) {
                        return true;
                    }
                }
                return false;
            }
            case "DELETE_ROOM": {
                String roomId = (String) payload.get("roomId");
                if (roomId != null) {
                    boolean deleted = roomService.deleteRoom(roomId);
                    if (deleted) {
                        return true;
                    }
                }
                return false;
            }
            case "DISABLE_ROOM": {
                String roomId = (String) payload.get("roomId");
                String remark = (String) payload.get("remark");
                if (roomId != null) {
                    Room room = roomService.disableRoom(roomId, remark);
                    if (room != null) {
                        return true;
                    }
                }
                return false;
            }
            case "ENABLE_ROOM": {
                String roomId = (String) payload.get("roomId");
                if (roomId != null) {
                    Room room = roomService.enableRoom(roomId);
                    if (room != null) {
                        return true;
                    }
                }
                return false;
            }
            default:
                return false;
        }
    }

    private Double getAsLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Double) return (Double) obj;
        if (obj instanceof Integer) return Double.valueOf((Integer) obj);
        if (obj instanceof Long) return Double.valueOf((Long) obj);
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Double getAsDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Double) return (Double) obj;
        if (obj instanceof Integer) return Double.valueOf((Integer) obj);
        if (obj instanceof Long) return Double.valueOf((Long) obj);
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[WS] Error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("[WS] Server started on port " + getPort());
        alertScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    broadcastAlerts();
                } catch (Exception e) {
                    System.err.println("[WS] Alert broadcast error: " + e.getMessage());
                }
            }
        }, 10, 30, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                alertScheduler.shutdown();
            }
        }));
    }

    private void sendFullSync(WebSocket conn) {
        List<Room> rooms = roomService.getAllRooms();
        List<RoomLog> logs = roomService.getRecentLogs(10);
        List<AlertItem> alerts = roomService.getAlerts();
        List<Operator> operators = roomService.getOperators();

        Map<String, Object> payload = new HashMap<>();
        payload.put("rooms", rooms);
        payload.put("logs", logs);
        payload.put("alerts", alerts);
        payload.put("operators", operators);
        payload.put("currentOperator", roomService.getCurrentOperatorName());

        Map<String, Object> message = new HashMap<>();
        message.put("type", "FULL_SYNC");
        message.put("payload", payload);

        conn.send(gson.toJson(message));
    }

    private void broadcastUpdate() {
        List<Room> rooms = roomService.getAllRooms();
        List<RoomLog> logs = roomService.getRecentLogs(10);
        List<AlertItem> alerts = roomService.getAlerts();

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

        Map<String, Object> alertsPayload = new HashMap<>();
        alertsPayload.put("alerts", alerts);
        Map<String, Object> alertsMsg = new HashMap<>();
        alertsMsg.put("type", "ALERTS_UPDATE");
        alertsMsg.put("payload", alertsPayload);
        broadcast(gson.toJson(alertsMsg));
    }

    private void broadcastAlerts() {
        List<AlertItem> alerts = roomService.getAlerts();
        Map<String, Object> payload = new HashMap<>();
        payload.put("alerts", alerts);
        Map<String, Object> message = new HashMap<>();
        message.put("type", "ALERTS_UPDATE");
        message.put("payload", payload);
        broadcast(gson.toJson(message));
    }

    private void sendSingleMessage(WebSocket conn, String type, Object payload) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.put("payload", payload);
        conn.send(gson.toJson(message));
    }
}
