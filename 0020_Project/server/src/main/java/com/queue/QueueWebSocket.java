/**
 * QueueWebSocket WebSocket 处理类
 * 职责：处理 WebSocket 连接、消息接收、状态广播
 * 使用 org.java-websocket 库实现 WebSocket 服务器
 *
 * 安全增强：
 * 1. payload 非空校验
 * 2. 必填字段（businessType/counterId/ticketId）校验
 * 3. 非法消息记录日志但不抛出异常，不影响服务运行
 *
 * 数据流：
 * 前端发送消息 -> onMessage 解析 -> 调用 QueueService 处理
 *   -> 获取 QueueState -> broadcastState 发送给所有连接的客户端
 */
package com.queue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
            case "GET_STATE":
                sendState(conn);
                break;

            case "TAKE_TICKET":
                handleTakeTicket(payload);
                break;

            case "CALL_NEXT":
                handleCallNext(payload);
                break;

            case "COMPLETE":
                handleComplete(payload);
                break;

            case "MISS":
                handleMiss(payload);
                break;

            case "RECALL":
                handleRecall(payload);
                break;

            default:
                System.out.println("未知动作: " + action);
        }
    }

    /**
     * 校验并处理取号请求
     * 需要 payload 非空，且 businessType 合法
     */
    private void handleTakeTicket(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            System.err.println("TAKE_TICKET 失败：payload 为空");
            return;
        }
        JsonNode btNode = payload.get("businessType");
        if (btNode == null || btNode.isNull() || !btNode.isTextual()) {
            System.err.println("TAKE_TICKET 失败：businessType 字段缺失或类型错误");
            return;
        }
        String businessType = btNode.asText();
        if (businessType == null || businessType.trim().isEmpty()) {
            System.err.println("TAKE_TICKET 失败：businessType 为空");
            return;
        }
        if (!VALID_BUSINESS_TYPES.contains(businessType)) {
            System.err.println("TAKE_TICKET 失败：非法 businessType=" + businessType);
            return;
        }
        queueService.takeTicket(businessType);
        broadcastState();
    }

    /**
     * 校验并处理叫号请求
     * 需要 counterId 非空
     */
    private void handleCallNext(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            System.err.println("CALL_NEXT 失败：payload 为空");
            return;
        }
        String counterId = getTextValue(payload, "counterId");
        if (counterId == null) {
            System.err.println("CALL_NEXT 失败：counterId 为空");
            return;
        }
        queueService.callNext(counterId);
        broadcastState();
    }

    /**
     * 校验并处理完成请求
     * 需要 counterId 非空，ticketId 可选
     */
    private void handleComplete(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            System.err.println("COMPLETE 失败：payload 为空");
            return;
        }
        String counterId = getTextValue(payload, "counterId");
        if (counterId == null) {
            System.err.println("COMPLETE 失败：counterId 为空");
            return;
        }
        String ticketId = getOptionalTextValue(payload, "ticketId");
        queueService.completeTicket(counterId, ticketId);
        broadcastState();
    }

    /**
     * 校验并处理过号请求
     * 需要 counterId 非空，ticketId 可选
     */
    private void handleMiss(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            System.err.println("MISS 失败：payload 为空");
            return;
        }
        String counterId = getTextValue(payload, "counterId");
        if (counterId == null) {
            System.err.println("MISS 失败：counterId 为空");
            return;
        }
        String ticketId = getOptionalTextValue(payload, "ticketId");
        queueService.missTicket(counterId, ticketId);
        broadcastState();
    }

    /**
     * 校验并处理重新叫号请求
     * 需要 counterId 非空，ticketId 可选
     */
    private void handleRecall(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            System.err.println("RECALL 失败：payload 为空");
            return;
        }
        String counterId = getTextValue(payload, "counterId");
        if (counterId == null) {
            System.err.println("RECALL 失败：counterId 为空");
            return;
        }
        String ticketId = getOptionalTextValue(payload, "ticketId");
        queueService.recallTicket(counterId, ticketId);
        broadcastState();
    }

    /**
     * 从 JsonNode 中安全读取必填文本字段
     * @return 字段值，若为空/缺失/null 返回 null
     */
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

    /**
     * 从 JsonNode 中安全读取可选文本字段（允许为空字符串）
     * @return 字段值，若缺失/null 返回 null
     */
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

    private void sendState(WebSocket conn) {
        try {
            QueueState state = queueService.getQueueState();
            WsMessage response = new WsMessage("STATE_UPDATE", state);
            conn.send(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            System.err.println("发送状态失败: " + e.getMessage());
        }
    }

    public void broadcastState() {
        try {
            QueueState state = queueService.getQueueState();
            WsMessage response = new WsMessage("STATE_UPDATE", state);
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
