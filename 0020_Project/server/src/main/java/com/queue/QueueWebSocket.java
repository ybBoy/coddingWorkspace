/**
 * QueueWebSocket WebSocket 处理类
 * 职责：处理 WebSocket 连接、消息接收、状态广播
 * 使用 org.java-websocket 库实现 WebSocket 服务器
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
import java.util.Collection;

public class QueueWebSocket extends WebSocketServer {

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
            e.printStackTrace();
        }
    }

    private void handleMessage(WebSocket conn, WsMessage message) {
        String action = message.getAction();
        JsonNode payload = objectMapper.valueToTree(message.getPayload());

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

    private void handleTakeTicket(JsonNode payload) {
        String businessType = payload.get("businessType").asText();
        queueService.takeTicket(businessType);
        broadcastState();
    }

    private void handleCallNext(JsonNode payload) {
        String counterId = payload.get("counterId").asText();
        queueService.callNext(counterId);
        broadcastState();
    }

    private void handleComplete(JsonNode payload) {
        String counterId = payload.get("counterId").asText();
        String ticketId = payload.has("ticketId") ? payload.get("ticketId").asText() : null;
        queueService.completeTicket(counterId, ticketId);
        broadcastState();
    }

    private void handleMiss(JsonNode payload) {
        String counterId = payload.get("counterId").asText();
        String ticketId = payload.has("ticketId") ? payload.get("ticketId").asText() : null;
        queueService.missTicket(counterId, ticketId);
        broadcastState();
    }

    private void handleRecall(JsonNode payload) {
        String counterId = payload.get("counterId").asText();
        String ticketId = payload.has("ticketId") ? payload.get("ticketId").asText() : null;
        queueService.recallTicket(counterId, ticketId);
        broadcastState();
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
