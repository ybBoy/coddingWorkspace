package ws;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import domain.Note;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import service.ReadingService;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReadingSocket extends WebSocketServer {
    private final ReadingService service;
    private final Gson gson;
    private final Set<WebSocket> connections;
    private final Map<WebSocket, String> userNames;
    private final Map<WebSocket, Boolean> moderatorMap;
    private static final String MODERATOR_TOKEN = "reading-moderator-2025";

    public static class WsMessage {
        String type;
        Object payload;
        String sender;

        public WsMessage() {}
        public WsMessage(String type, Object payload, String sender) {
            this.type = type;
            this.payload = payload;
            this.sender = sender;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public Object getPayload() { return payload; }
        public void setPayload(Object payload) { this.payload = payload; }

        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }
    }

    public ReadingSocket(int port, ReadingService service) {
        super(new InetSocketAddress(port));
        this.service = service;
        this.gson = new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .create();
        this.connections = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.userNames = new ConcurrentHashMap<>();
        this.moderatorMap = new ConcurrentHashMap<>();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        String nameFromQuery = extractNameFromQuery(handshake);
        if (nameFromQuery != null && !nameFromQuery.isEmpty()) {
            userNames.put(conn, nameFromQuery);
        }
        System.out.println("[WS] New connection: " + conn.getRemoteSocketAddress()
                + (nameFromQuery != null ? " (" + nameFromQuery + ")" : ""));
        sendTo(conn, new WsMessage("INIT", buildInitData(conn), null));
        broadcastOnlineCount();
    }

    private String extractNameFromQuery(ClientHandshake handshake) {
        try {
            String res = handshake.getResourceDescriptor();
            if (res == null || !res.contains("?")) return null;
            String q = res.substring(res.indexOf('?') + 1);
            for (String pair : q.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && "name".equals(kv[0])) {
                    return URLDecoder.decode(kv[1], "UTF-8");
                }
            }
        } catch (UnsupportedEncodingException ignored) {}
        return null;
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        String name = userNames.remove(conn);
        moderatorMap.remove(conn);
        System.out.println("[WS] Connection closed: " + conn.getRemoteSocketAddress()
                + (name != null ? " (" + name + ")" : ""));
        broadcastOnlineCount();
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            String decoded = ensureUtf8(message);
            WsMessage msg = gson.fromJson(decoded, WsMessage.class);
            if (msg == null || msg.type == null) return;
            handleMessage(conn, msg);
        } catch (Exception e) {
            System.err.println("[WS] Message parse error: " + e.getMessage());
        }
    }

    private String ensureUtf8(String s) {
        if (s == null) return null;
        try {
            byte[] raw = s.getBytes(StandardCharsets.ISO_8859_1);
            if (looksLikeUtf8(raw)) {
                return new String(raw, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}
        return s;
    }

    private boolean looksLikeUtf8(byte[] bytes) {
        int i = 0;
        while (i < bytes.length) {
            byte b = bytes[i];
            if ((b & 0x80) == 0) { i++; continue; }
            int n;
            if ((b & 0xE0) == 0xC0) n = 1;
            else if ((b & 0xF0) == 0xE0) n = 2;
            else if ((b & 0xF8) == 0xF0) n = 3;
            else return false;
            for (int j = 0; j < n; j++) {
                if (i + j + 1 >= bytes.length) return false;
                byte c = bytes[i + j + 1];
                if ((c & 0xC0) != 0x80) return false;
            }
            i += n + 1;
        }
        return true;
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[WS] Error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("[WS] Server started on port " + getPort());
    }

    @SuppressWarnings("unchecked")
    private void handleMessage(WebSocket conn, WsMessage msg) {
        String sender = msg.sender;
        if (sender != null && !sender.trim().isEmpty()) {
            userNames.put(conn, sender.trim());
        }
        switch (msg.type) {
            case "SET_NAME": {
                if (msg.payload != null) {
                    String name = msg.payload.toString().trim();
                    if (!name.isEmpty()) userNames.put(conn, name);
                }
                break;
            }
            case "SET_MODERATOR": {
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                boolean wantMod = Boolean.TRUE.equals(p.get("moderator"));
                String token = p.get("token") != null ? p.get("token").toString() : "";
                boolean granted = MODERATOR_TOKEN.equals(token);
                if (wantMod) {
                    if (granted || moderatorMap.isEmpty()) {
                        moderatorMap.put(conn, true);
                        sendTo(conn, new WsMessage("MODERATOR_GRANTED", true, null));
                        broadcastModeratorList();
                    } else {
                        sendTo(conn, new WsMessage("MODERATOR_DENIED", "已存在主持人", null));
                    }
                } else {
                    moderatorMap.remove(conn);
                    broadcastModeratorList();
                }
                break;
            }
            case "ADD_NOTE": {
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                String paragraphId = (String) p.get("paragraphId");
                String author = userNames.get(conn);
                if (author == null) author = p.get("author") != null ? p.get("author").toString() : "匿名";
                String content = (String) p.get("content");
                Note.NoteType type;
                try {
                    type = Note.NoteType.valueOf((String) p.getOrDefault("type", "THOUGHT"));
                } catch (Exception e) {
                    type = Note.NoteType.THOUGHT;
                }
                Note note = service.addNote(paragraphId, author, content, type);
                if (note != null) {
                    broadcast(new WsMessage("NOTE_ADDED", note, author));
                }
                break;
            }
            case "TOGGLE_LIKE": {
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                String noteId = (String) p.get("noteId");
                String user = userNames.get(conn);
                if (user == null) user = "匿名";
                if (service.toggleLike(noteId, user)) {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("noteId", noteId);
                    payload.put("likes", service.getAllNotes().stream()
                            .filter(n -> noteId.equals(n.getId()))
                            .findFirst()
                            .map(n -> {
                                Map<String, Object> m = new HashMap<>();
                                m.put("count", n.getLikeCount());
                                m.put("users", n.getLikes());
                                return m;
                            }).orElse(null));
                    payload.put("user", user);
                    broadcast(new WsMessage("LIKE_UPDATED", payload, user));
                }
                break;
            }
            case "TOGGLE_HIGHLIGHT": {
                if (!isModerator(conn)) {
                    sendError(conn, "TOGGLE_HIGHLIGHT", "仅主持人可标记重点");
                    break;
                }
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                String noteId = (String) p.get("noteId");
                if (service.toggleHighlight(noteId)) {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("noteId", noteId);
                    payload.put("highlighted", service.getAllNotes().stream()
                            .filter(n -> noteId.equals(n.getId()))
                            .findFirst()
                            .map(Note::isHighlighted).orElse(false));
                    broadcast(new WsMessage("HIGHLIGHT_UPDATED", payload, userNames.get(conn)));
                }
                break;
            }
            case "SWITCH_PARAGRAPH": {
                if (!isModerator(conn)) {
                    sendError(conn, "SWITCH_PARAGRAPH", "仅主持人可切换段落");
                    break;
                }
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                String pid = (String) p.get("paragraphId");
                if (service.switchParagraph(pid)) {
                    broadcastParagraphSwitched(conn);
                }
                break;
            }
            case "MOVE_NEXT": {
                if (!isModerator(conn)) {
                    sendError(conn, "MOVE_NEXT", "仅主持人可切换段落");
                    break;
                }
                if (service.moveNext()) {
                    broadcastParagraphSwitched(conn);
                }
                break;
            }
            case "MOVE_PREV": {
                if (!isModerator(conn)) {
                    sendError(conn, "MOVE_PREV", "仅主持人可切换段落");
                    break;
                }
                if (service.movePrev()) {
                    broadcastParagraphSwitched(conn);
                }
                break;
            }
            case "REQUEST_STATE": {
                sendTo(conn, new WsMessage("STATE_SYNC", buildInitData(conn), null));
                break;
            }
            case "HEARTBEAT": {
                sendTo(conn, new WsMessage("HEARTBEAT_ACK", System.currentTimeMillis(), null));
                break;
            }
            default:
                break;
        }
    }

    private boolean isModerator(WebSocket conn) {
        return Boolean.TRUE.equals(moderatorMap.get(conn));
    }

    private void sendError(WebSocket conn, String action, String reason) {
        Map<String, Object> err = new HashMap<>();
        err.put("action", action);
        err.put("reason", reason);
        sendTo(conn, new WsMessage("ERROR", err, null));
    }

    private void broadcastParagraphSwitched(WebSocket conn) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paragraphId", service.getArticle().getCurrentParagraphId());
        payload.put("index", service.getArticle().getCurrentParagraphIndex());
        broadcast(new WsMessage("PARAGRAPH_SWITCHED", payload, userNames.get(conn)));
    }

    private void broadcastOnlineCount() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("onlineCount", connections.size());
        List<String> names = new ArrayList<>();
        for (WebSocket c : connections) {
            String n = userNames.get(c);
            if (n != null) names.add(n);
        }
        payload.put("names", names);
        broadcast(new WsMessage("ONLINE_COUNT", payload, null));
    }

    private void broadcastModeratorList() {
        List<String> mods = new ArrayList<>();
        for (Map.Entry<WebSocket, Boolean> e : moderatorMap.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) {
                String n = userNames.get(e.getKey());
                if (n != null) mods.add(n);
            }
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("moderators", mods);
        broadcast(new WsMessage("MODERATOR_LIST", payload, null));
    }

    private Map<String, Object> buildInitData(WebSocket conn) {
        Map<String, Object> data = new HashMap<>();
        data.put("article", service.getArticle());
        data.put("notes", service.getAllNotes());
        data.put("noteCounts", service.getNoteCountByParagraph());
        data.put("onlineCount", connections.size());
        List<String> names = new ArrayList<>();
        for (WebSocket c : connections) {
            String n = userNames.get(c);
            if (n != null) names.add(n);
        }
        data.put("onlineNames", names);
        List<String> mods = new ArrayList<>();
        for (Map.Entry<WebSocket, Boolean> e : moderatorMap.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) {
                String n = userNames.get(e.getKey());
                if (n != null) mods.add(n);
            }
        }
        data.put("moderators", mods);
        data.put("isModerator", isModerator(conn));
        return data;
    }

    private void broadcast(WsMessage msg) {
        String json = gson.toJson(msg);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        String utf8Json = new String(bytes, StandardCharsets.UTF_8);
        for (WebSocket c : connections) {
            if (c != null && c.isOpen()) {
                c.send(utf8Json);
            }
        }
    }

    private void sendTo(WebSocket conn, WsMessage msg) {
        if (conn != null && conn.isOpen()) {
            String json = gson.toJson(msg);
            String utf8Json = new String(json.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            conn.send(utf8Json);
        }
    }
}
