/**
 * QueueWebSocket WebSocket 处理类
 * 职责：处理 WebSocket 连接、消息接收、状态广播
 * 使用 org.java-websocket 库实现 WebSocket 服务器
 *
 * 迭代新增：
 * 1. 新增消息类型：CALL_NEXT_BY_TYPE、REQUEUE_MISSED、FINISH_MISSED、ADD_COUNTER、UPDATE_COUNTER、TOGGLE_COUNTER
 * 2. 所有操作返回 OPERATION_RESULT，供前端显示 Toast 提示
 *
 * 安全增强：
 * 1. payload 非空校验
 * 2. 必填字段（businessType/counterId/ticketId）校验
 * 3. 非法消息记录日志但不抛出异常，不影响服务运行
 *
 * 数据流：
 * 前端发送消息 -> onMessage 解析 -> 调用 QueueService 处理
 *   -> 发送 OPERATION_RESULT 反馈 -> 获取 QueueState -> broadcastState 发送给所有连接的客户端
 */
package com.queue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.*;

public class QueueWebSocket extends WebSocketServer {

    private static final Set<String> VALID_BUSINESS_TYPES = new HashSet<>(
            Arrays.asList("咨询", "办理", "售后")
    );

    private final QueueService queueService;
    private final ObjectMapper objectMapper;

    public QueueWebSocket(int port, QueueService queueService) {
        super(new InetSocketAddress(port));
        this.queueService = queueService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("新客户端连接: " + conn.getRemoteSocketAddress());
        sendState(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("客户端断开: " + conn.getRemoteSocketAddress() + ", 原因: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            WsMessage wsMessage = objectMapper.readValue(message, WsMessage.class);
            handleMessage(conn, wsMessage);
        } catch (Exception e) {
            System.err.println("处理消息失败: " + e.getMessage());
        }
    }

    private void handleMessage(WebSocket conn, WsMessage message) {
        if (message == null || message.getAction() == null || message.getAction().trim().isEmpty()) {
            System.err.println("收到无效消息：action 为空");
            return;
        }

        String action = message.getAction();
        JsonNode payload = null;
        try {
            payload = objectMapper.valueToTree(message.getPayload());
        } catch (IllegalArgumentException e) {
            payload = objectMapper.createObjectNode();
        }
        if (payload == null) {
            payload = objectMapper.createObjectNode();
        }

        switch (action) {
            case WsMessage.GET_STATE:
                sendState(conn);
                break;

            case WsMessage.TAKE_TICKET:
                handleTakeTicket(payload);
                break;

            case WsMessage.CALL_NEXT:
                handleCallNext(payload);
                break;

            case WsMessage.CALL_NEXT_BY_TYPE:
                handleCallNextByType(payload);
                break;

            case WsMessage.COMPLETE:
                handleComplete(payload);
                break;

            case WsMessage.MISS:
                handleMiss(payload);
                break;

            case WsMessage.RECALL:
                handleRecall(payload);
                break;

            case WsMessage.REQUEUE_MISSED:
                handleRequeueMissed(payload);
                break;

            case WsMessage.FINISH_MISSED:
                handleFinishMissed(payload);
                break;

            case WsMessage.ADD_COUNTER:
                handleAddCounter(payload);
                break;

            case WsMessage.UPDATE_COUNTER:
                handleUpdateCounter(payload);
                break;

            case WsMessage.TOGGLE_COUNTER:
                handleToggleCounter(payload);
                break;

            default:
                System.out.println("未知动作: " + action);
                sendOperationResult(action, false, "未知操作类型");
        }
    }

    private void sendOperationResult(String action, boolean success, String message) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("action", action);
            result.put("message", message);
            WsMessage wsMsg = new WsMessage(WsMessage.OPERATION_RESULT, result);
            String json = objectMapper.writeValueAsString(wsMsg);
            Collection<WebSocket> connections = getConnections();
            for (WebSocket conn : connections) {
                if (conn.isOpen()) {
                    conn.send(json);
                }
            }
        } catch (Exception e) {
            System.err.println("发送操作结果失败: " + e.getMessage());
        }
    }

    private void handleTakeTicket(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            sendOperationResult(WsMessage.TAKE_TICKET, false, "请求数据为空");
            return;
        }
        JsonNode btNode = payload.get("businessType");
        if (btNode == null || btNode.isNull() || !btNode.isTextual()) {
            sendOperationResult(WsMessage.TAKE_TICKET, false, "业务类型字段缺失");
            return;
        }
        String businessType = btNode.asText();
        if (businessType == null || businessType.trim().isEmpty()) {
            sendOperationResult(WsMessage.TAKE_TICKET, false, "业务类型为空");
            return;
        }
        if (!VALID_BUSINESS_TYPES.contains(businessType)) {
            sendOperationResult(WsMessage.TAKE_TICKET, false, "非法业务类型: " + businessType);
            return;
        }
        Ticket ticket = queueService.takeTicket(businessType);
        if (ticket != null) {
            sendOperationResult(WsMessage.TAKE_TICKET, true, "取号成功：" + String.format("%03d", ticket.getNumber()));
            broadcastState();
        } else {
            sendOperationResult(WsMessage.TAKE_TICKET, false, "取号失败");
        }
    }

    private void handleCallNext(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            sendOperationResult(WsMessage.CALL_NEXT, false, "请求数据为空");
            return;
        }
        String counterId = getTextValue(payload, "counterId");
        if (counterId == null) {
            sendOperationResult(WsMessage.CALL_NEXT, false, "未选择窗口");
            return;
        }
        Ticket ticket = queueService.callNext(counterId);
        if (ticket != null) {
            sendOperationResult(WsMessage.CALL_NEXT, true, "叫号成功：" + String.format("%03d", ticket.getNumber()));
            broadcastState();
        } else {
            sendOperationResult(WsMessage.CALL_NEXT, false, "没有等待的号票或窗口忙");
        }
    }

    private void handleCallNextByType(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            sendOperationResult(WsMessage.CALL_NEXT_BY_TYPE, false, "请求数据为空");
            return;
        }
        String counterId = getTextValue(payload, "counterId");
        if (counterId == null) {
            sendOperationResult(WsMessage.CALL_NEXT_BY_TYPE, false, "未选择窗口");
            return;
        }
        String businessType = getTextValue(payload, "businessType");
        if (businessType == null) {
            sendOperationResult(WsMessage.CALL_NEXT_BY_TYPE, false, "未选择业务类型");
            return;
        }
        if (!"all".equals(businessType) && !VALID_BUSINESS_TYPES.contains(businessType)) {
            sendOperationResult(WsMessage.CALL_NEXT_BY_TYPE, false, "非法业务类型");
            return;
        }
        Ticket ticket = queueService.callNextByType(counterId, businessType);
        if (ticket != null) {
            sendOperationResult(WsMessage.CALL_NEXT_BY_TYPE, true,
                    "叫号成功：" + String.format("%03d", ticket.getNumber()) + "（" + ticket.getBusinessType() + "）");
            broadcastState();
        } else {
            sendOperationResult(WsMessage.CALL_NEXT_BY_TYPE, false,
                    "该业务类型下暂无等待号票");
        }
    }

    private void handleComplete(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            sendOperationResult(WsMessage.COMPLETE, false, "请求数据为空");
            return;
        }
        String counterId = getTextValue(payload, "counterId");
        if (counterId == null) {
            sendOperationResult(WsMessage.COMPLETE, false, "未选择窗口");
            return;
        }
        String ticketId = getOptionalTextValue(payload, "ticketId");
        boolean success = queueService.completeTicket(counterId, ticketId);
        sendOperationResult(WsMessage.COMPLETE, success, success ? "办理完成" : "操作失败，当前没有办理业务");
        if (success) broadcastState();
    }

    private void handleMiss(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            sendOperationResult(WsMessage.MISS, false, "请求数据为空");
            return;
        }
        String counterId = getTextValue(payload, "counterId");
        if (counterId == null) {
            sendOperationResult(WsMessage.MISS, false, "未选择窗口");
            return;
        }
        String ticketId = getOptionalTextValue(payload, "ticketId");
        boolean success = queueService.missTicket(counterId, ticketId);
        sendOperationResult(WsMessage.MISS, success, success ? "已标记过号" : "操作失败，当前没有办理业务");
        if (success) broadcastState();
    }

    private void handleRecall(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            sendOperationResult(WsMessage.RECALL, false, "请求数据为空");
            return;
        }
        String counterId = getTextValue(payload, "counterId");
        String ticketId = getOptionalTextValue(payload, "ticketId");
        if (counterId == null) {
            sendOperationResult(WsMessage.RECALL, false, "未选择窗口");
            return;
        }
        boolean success = queueService.recallTicket(counterId, ticketId);
        sendOperationResult(WsMessage.RECALL, success, success ? "重新叫号成功" : "重新叫号失败");
        if (success) broadcastState();
    }

    private void handleRequeueMissed(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            sendOperationResult(WsMessage.REQUEUE_MISSED, false, "请求数据为空");
            return;
        }
        String ticketId = getTextValue(payload, "ticketId");
        if (ticketId == null) {
            sendOperationResult(WsMessage.REQUEUE_MISSED, false, "ticketId 为空");
            return;
        }
        boolean success = queueService.requeueMissed(ticketId);
        sendOperationResult(WsMessage.REQUEUE_MISSED, success, success ? "已重新加入等待队列" : "操作失败");
        if (success) broadcastState();
    }

    private void handleFinishMissed(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            sendOperationResult(WsMessage.FINISH_MISSED, false, "请求数据为空");
            return;
        }
        String ticketId = getTextValue(payload, "ticketId");
        if (ticketId == null) {
            sendOperationResult(WsMessage.FINISH_MISSED, false, "ticketId 为空");
            return;
        }
        boolean success = queueService.finishMissed(ticketId);
        sendOperationResult(WsMessage.FINISH_MISSED, success, success ? "已标记结束" : "操作失败");
        if (success) broadcastState();
    }

    private void handleAddCounter(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            sendOperationResult(WsMessage.ADD_COUNTER, false, "请求数据为空");
            return;
        }
        String name = getTextValue(payload, "name");
        if (name == null) {
            sendOperationResult(WsMessage.ADD_COUNTER, false, "窗口名称为空");
            return;
        }
        List<String> types = getStringList(payload, "supportedBusinessTypes");
        if (types != null && !types.isEmpty()) {
            for (String t : types) {
                if (!VALID_BUSINESS_TYPES.contains(t)) {
                    sendOperationResult(WsMessage.ADD_COUNTER, false, "非法业务类型: " + t);
                    return;
                }
            }
        }
        Counter counter = queueService.addCounter(name, types);
        if (counter != null) {
            sendOperationResult(WsMessage.ADD_COUNTER, true, "已新增窗口：" + counter.getName());
            broadcastState();
        } else {
            sendOperationResult(WsMessage.ADD_COUNTER, false, "新增窗口失败");
        }
    }

    private void handleUpdateCounter(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            sendOperationResult(WsMessage.UPDATE_COUNTER, false, "请求数据为空");
            return;
        }
        String counterId = getTextValue(payload, "counterId");
        if (counterId == null) {
            sendOperationResult(WsMessage.UPDATE_COUNTER, false, "counterId 为空");
            return;
        }
        String name = getOptionalTextValue(payload, "name");
        List<String> types = getStringList(payload, "supportedBusinessTypes");
        if (types != null && !types.isEmpty()) {
            for (String t : types) {
                if (!VALID_BUSINESS_TYPES.contains(t)) {
                    sendOperationResult(WsMessage.UPDATE_COUNTER, false, "非法业务类型: " + t);
                    return;
                }
            }
        }
        boolean success = queueService.updateCounter(counterId, name, types);
        sendOperationResult(WsMessage.UPDATE_COUNTER, success, success ? "窗口已更新" : "更新失败，窗口不存在");
        if (success) broadcastState();
    }

    private void handleToggleCounter(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            sendOperationResult(WsMessage.TOGGLE_COUNTER, false, "请求数据为空");
            return;
        }
        String counterId = getTextValue(payload, "counterId");
        if (counterId == null) {
            sendOperationResult(WsMessage.TOGGLE_COUNTER, false, "counterId 为空");
            return;
        }
        JsonNode enabledNode = payload.get("enabled");
        if (enabledNode == null || !enabledNode.isBoolean()) {
            sendOperationResult(WsMessage.TOGGLE_COUNTER, false, "enabled 字段缺失或类型错误");
            return;
        }
        boolean enabled = enabledNode.asBoolean();
        boolean success = queueService.toggleCounter(counterId, enabled);
        sendOperationResult(WsMessage.TOGGLE_COUNTER, success,
                success ? "窗口已" + (enabled ? "启用" : "停用")
                        : "操作失败，窗口可能正在办理业务");
        if (success) broadcastState();
    }

    private String getTextValue(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return null;
        }
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull() || !fieldNode.isTextual()) {
            return null;
        }
        String value = fieldNode.asText();
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value;
    }

    private String getOptionalTextValue(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.has(fieldName)) {
            return null;
        }
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        return fieldNode.asText();
    }

    private List<String> getStringList(JsonNode node, String fieldName) {
        if (node == null || node.isNull() || !node.has(fieldName)) {
            return null;
        }
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull() || !fieldNode.isArray()) {
            return null;
        }
        List<String> list = new ArrayList<>();
        for (JsonNode item : (ArrayNode) fieldNode) {
            if (item.isTextual()) {
                list.add(item.asText());
            }
        }
        return list;
    }

    private void sendState(WebSocket conn) {
        try {
            QueueState state = queueService.getQueueState();
            WsMessage response = new WsMessage(WsMessage.STATE_UPDATE, state);
            conn.send(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            System.err.println("发送状态失败: " + e.getMessage());
        }
    }

    public void broadcastState() {
        try {
            QueueState state = queueService.getQueueState();
            WsMessage response = new WsMessage(WsMessage.STATE_UPDATE, state);
            String json = objectMapper.writeValueAsString(response);
            Collection<WebSocket> connections = getConnections();
            for (WebSocket conn : connections) {
                if (conn.isOpen()) {
                    conn.send(json);
                }
            }
        } catch (Exception e) {
            System.err.println("广播状态失败: " + e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("WebSocket 错误: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket 服务器已启动，端口: " + getPort());
    }
}
