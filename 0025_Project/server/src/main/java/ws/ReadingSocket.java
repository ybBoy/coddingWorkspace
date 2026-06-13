package ws;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import domain.Note;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import service.ReadingService;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReadingSocket extends WebSocketServer {
    private final ReadingService service;
    private final Gson gson;
    private final Set<WebSocket> connections;
    private final Map<WebSocket, String> userNames;

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
        this.gson = new GsonBuilder().create();
        this.connections = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.userNames = new ConcurrentHashMap<>();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        System.out.println("[WS] New connection: " + conn.getRemoteSocketAddress());
        sendTo(conn, new WsMessage("INIT", buildInitData(), null));
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        String name = userNames.remove(conn);
        System.out.println("[WS] Connection closed: " + conn.getRemoteSocketAddress()
                + (name != null ? " (" + name + ")" : ""));
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            WsMessage msg = gson.fromJson(message, WsMessage.class);
            if (msg == null || msg.type == null) return;
            handleMessage(conn, msg);
        } catch (Exception e) {
            System.err.println("[WS] Message parse error: " + e.getMessage());
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
            case "ADD_NOTE": {
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                String paragraphId = (String) p.get("paragraphId");
                String author = userNames.get(conn);
                if (author == null) author = p.get("author") != null ? p.get("author").toString() : "匿名";
                String content = (String) p.get("content");
                Note.NoteType type = Note.NoteType.valueOf((String) p.getOrDefault("type", "THOUGHT"));
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
                            .map(n -> new HashMap<String, Object>() {{
                                put("count", n.getLikeCount());
                                put("users", n.getLikes());
                            }}).orElse(null));
                    payload.put("user", user);
                    broadcast(new WsMessage("LIKE_UPDATED", payload, user));
                }
                break;
            }
            case "TOGGLE_HIGHLIGHT": {
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
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                String pid = (String) p.get("paragraphId");
                if (service.switchParagraph(pid)) {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("paragraphId", service.getArticle().getCurrentParagraphId());
                    payload.put("index", service.getArticle().getCurrentParagraphIndex());
                    broadcast(new WsMessage("PARAGRAPH_SWITCHED", payload, userNames.get(conn)));
                }
                break;
            }
            case "MOVE_NEXT": {
                if (service.moveNext()) {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("paragraphId", service.getArticle().getCurrentParagraphId());
                    payload.put("index", service.getArticle().getCurrentParagraphIndex());
                    broadcast(new WsMessage("PARAGRAPH_SWITCHED", payload, userNames.get(conn)));
                }
                break;
            }
            case "MOVE_PREV": {
                if (service.movePrev()) {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("paragraphId", service.getArticle().getCurrentParagraphId());
                    payload.put("index", service.getArticle().getCurrentParagraphIndex());
                    broadcast(new WsMessage("PARAGRAPH_SWITCHED", payload, userNames.get(conn)));
                }
                break;
            }
            case "REQUEST_STATE": {
                sendTo(conn, new WsMessage("STATE_SYNC", buildInitData(), null));
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

    private Map<String, Object> buildInitData() {
        Map<String, Object> data = new HashMap<>();
        data.put("article", service.getArticle());
        data.put("notes", service.getAllNotes());
        data.put("noteCounts", service.getNoteCountByParagraph());
        data.put("onlineCount", connections.size());
        return data;
    }

    private void broadcast(WsMessage msg) {
        String json = gson.toJson(msg);
        for (WebSocket c : connections) {
            if (c != null && c.isOpen()) {
                c.send(json);
            }
        }
    }

    private void sendTo(WebSocket conn, WsMessage msg) {
        if (conn != null && conn.isOpen()) {
            conn.send(gson.toJson(msg));
        }
    }
}
