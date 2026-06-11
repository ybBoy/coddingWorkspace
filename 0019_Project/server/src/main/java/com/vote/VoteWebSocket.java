package com.vote;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * VoteWebSocket 职责：
 * - 处理每个客户端的 WebSocket 连接生命周期
 * - 接收客户端消息（投票、新增、清空），转发给 VoteService
 * - 将最新结果广播给所有在线客户端
 */
@WebSocket
public class VoteWebSocket {
    // 保存所有在线会话
    private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();
    private static VoteService voteService;

    public static void setVoteService(VoteService service) {
        voteService = service;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        sessions.add(session);
        System.out.println("客户端连接: " + session.getRemoteAddress().getAddress());
        // 新连接上来立刻发送当前数据
        sendToSession(session, buildMessage("INIT", voteService.getAll()));
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        sessions.remove(session);
        System.out.println("客户端断开: " + session.getRemoteAddress().getAddress());
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        try {
            // 解析前端发来的消息：{"type":"VOTE","data":"xxx"}
            String type = extractJsonValue(message, "type");
            String data = extractJsonValue(message, "data");
            if (type == null) return;

            java.util.List<VoteOption> newState = null;
            switch (type) {
                case "VOTE":
                    newState = voteService.vote(data);
                    break;
                case "ADD":
                    newState = voteService.addOption(data);
                    break;
                case "CLEAR":
                    newState = voteService.clearAll();
                    break;
                default:
                    return;
            }
            if (newState != null) {
                broadcast(buildMessage("UPDATE", newState));
            }
        } catch (Exception e) {
            System.err.println("处理消息异常: " + e.getMessage());
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket 错误: " + error.getMessage());
    }

    // 广播给所有客户端
    private void broadcast(String msg) {
        for (Session s : sessions) {
            if (s.isOpen()) sendToSession(s, msg);
        }
    }

    private void sendToSession(Session session, String msg) {
        try {
            session.getRemote().sendString(msg);
        } catch (IOException e) {
            System.err.println("发送消息失败: " + e.getMessage());
        }
    }

    // 将选项列表序列化为 JSON 消息
    private String buildMessage(String type, java.util.List<VoteOption> options) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"").append(type).append("\",\"data\":[");
        for (int i = 0; i < options.size(); i++) {
            VoteOption o = options.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"id\":\"").append(escape(o.getId()))
              .append("\",\"name\":\"").append(escape(o.getName()))
              .append("\",\"votes\":").append(o.getVotes()).append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    // 简易 JSON 字段提取（够用即可）
    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length()) return null;
        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf('"', start);
            if (end < 0) return null;
            return json.substring(start, end);
        } else {
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(start, end).trim();
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
