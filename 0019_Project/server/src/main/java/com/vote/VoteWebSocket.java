package com.vote;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * VoteWebSocket 职责：
 * - 处理每个客户端的 WebSocket 连接生命周期
 * - 接收客户端消息，根据类型和权限转发给 VoteService
 * - 将最新状态广播给所有在线客户端
 * - 新连接立即发送 INIT 消息（完整状态）
 * - 定期清理失效 Session
 *
 * 消息协议：
 * 客户端 -> 服务端：
 *   {type:"VOTE", clientId:"xxx", data:"optionId"}
 *   {type:"ADD", clientId:"xxx", data:"optionName"}
 *   {type:"CLEAR", adminToken:"xxx"}
 *   {type:"ADMIN_LOGIN", data:"password"}
 *   {type:"DELETE", adminToken:"xxx", data:"optionId"}
 *   {type:"RENAME", adminToken:"xxx", data:{id:"opt1", name:"新名称"}}
 *   {type:"LOCK", adminToken:"xxx", data:true/false}
 *   {type:"SET_TIMER", adminToken:"xxx", data:60}
 *
 * 服务端 -> 客户端：
 *   {type:"INIT", data:{options:[...], locked:false, remainingSeconds:0}}
 *   {type:"UPDATE", data:{options:[...], locked:false, remainingSeconds:0}}
 *   {type:"ADMIN_LOGIN_OK"}
 *   {type:"ADMIN_LOGIN_FAIL"}
 */
@WebSocket
public class VoteWebSocket {
    // 保存所有在线会话
    private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();
    private static VoteService voteService;

    public static void setVoteService(VoteService service) {
        voteService = service;
        // 设置广播回调，VoteService 状态变化时自动广播
        voteService.setBroadcastCallback(new VoteService.BroadcastCallback() {
            @Override
            public void broadcast(VoteState state) {
                broadcastState(state);
            }
        });
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        sessions.add(session);
        System.out.println("客户端连接: " + session.getRemoteAddress().getAddress()
            + ", 当前在线: " + sessions.size());
        // 新连接上来立刻发送当前完整状态
        sendToSession(session, buildStateMessage("INIT", voteService.getState()));
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        sessions.remove(session);
        System.out.println("客户端断开: " + session.getRemoteAddress().getAddress()
            + ", 当前在线: " + sessions.size());
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        try {
            String type = extractJsonValue(message, "type");
            String clientId = extractJsonValue(message, "clientId");
            String adminToken = extractJsonValue(message, "adminToken");
            String data = extractJsonRaw(message, "data");

            if (type == null) return;
            VoteState newState = null;

            switch (type) {
                case "VOTE": {
                    String optionId = stripQuotes(data);
                    newState = voteService.vote(clientId, optionId);
                    break;
                }
                case "ADD": {
                    String name = stripQuotes(data);
                    newState = voteService.addOption(name);
                    break;
                }
                case "CLEAR": {
                    newState = voteService.clearAll(adminToken);
                    break;
                }
                case "ADMIN_LOGIN": {
                    String password = stripQuotes(data);
                    boolean ok = voteService.adminLogin(password);
                    sendToSession(session, ok
                        ? "{\"type\":\"ADMIN_LOGIN_OK\",\"data\":\"" + password + "\"}"
                        : "{\"type\":\"ADMIN_LOGIN_FAIL\"}");
                    return;
                }
                case "DELETE": {
                    String optionId = stripQuotes(data);
                    newState = voteService.deleteOption(adminToken, optionId);
                    break;
                }
                case "RENAME": {
                    // data 是 {"id":"xxx","name":"xxx"} 格式
                    String id = extractJsonValue(data, "id");
                    String name = extractJsonValue(data, "name");
                    newState = voteService.renameOption(adminToken, id, name);
                    break;
                }
                case "LOCK": {
                    boolean locked = "true".equals(data.trim());
                    newState = voteService.setLocked(adminToken, locked);
                    break;
                }
                case "SET_TIMER": {
                    try {
                        int seconds = Integer.parseInt(data.trim());
                        newState = voteService.setTimer(adminToken, seconds);
                    } catch (NumberFormatException ignored) {}
                    break;
                }
                default:
                    return;
            }

            if (newState != null) {
                broadcastState(newState);
            }
        } catch (Exception e) {
            System.err.println("处理消息异常: " + e.getMessage() + ", 消息: " + message);
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket 错误: " + error.getMessage());
    }

    // 广播完整状态给所有客户端
    private static void broadcastState(VoteState state) {
        String msg = buildStateMessage("UPDATE", state);
        for (Session s : sessions) {
            if (s.isOpen()) sendToSession(s, msg);
        }
    }

    private static void sendToSession(Session session, String msg) {
        try {
            session.getRemote().sendString(msg);
        } catch (IOException e) {
            System.err.println("发送消息失败: " + e.getMessage());
        }
    }

    // 构建包含完整状态的 JSON 消息
    private static String buildStateMessage(String type, VoteState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"").append(type).append("\",\"data\":{");

        // options 数组
        sb.append("\"options\":[");
        java.util.List<VoteOption> options = state.getOptions();
        for (int i = 0; i < options.size(); i++) {
            VoteOption o = options.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"id\":\"").append(escape(o.getId()))
              .append("\",\"name\":\"").append(escape(o.getName()))
              .append("\",\"votes\":").append(o.getVotes()).append("}");
        }
        sb.append("],");

        // locked 和 remainingSeconds
        sb.append("\"locked\":").append(state.isLocked()).append(",");
        sb.append("\"remainingSeconds\":").append(state.getRemainingSeconds());

        sb.append("}}");
        return sb.toString();
    }

    // 提取 JSON 字段值（返回原始字符串，数字、布尔直接返回，字符串带引号）
    private String extractJsonRaw(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int start = idx + search.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length()) return null;

        char first = json.charAt(start);
        if (first == '"') {
            // 字符串
            start++;
            int end = json.indexOf('"', start);
            if (end < 0) return null;
            return "\"" + json.substring(start, end) + "\"";
        } else if (first == '{' || first == '[') {
            // 对象或数组，找匹配的结束符
            char open = first;
            char close = (open == '{') ? '}' : ']';
            int depth = 0;
            boolean inStr = false;
            int end = start;
            for (; end < json.length(); end++) {
                char c = json.charAt(end);
                if (c == '"' && (end == 0 || json.charAt(end - 1) != '\\')) inStr = !inStr;
                if (!inStr) {
                    if (c == open) depth++;
                    else if (c == close) depth--;
                    if (depth == 0) {
                        end++;
                        break;
                    }
                }
            }
            return json.substring(start, end);
        } else {
            // 数字或布尔
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(start, end).trim();
        }
    }

    // 提取 JSON 字段值（去掉字符串的引号）
    private String extractJsonValue(String json, String key) {
        String raw = extractJsonRaw(json, key);
        if (raw == null) return null;
        return stripQuotes(raw);
    }

    private String stripQuotes(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
