package ws;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import domain.*;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import service.RoomService;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReadingSocket extends WebSocketServer {
    private final RoomService service;
    private final Gson gson;
    private final Set<WebSocket> connections;
    private final Map<WebSocket, String> userNames;
    private final Map<WebSocket, String> connRoomMap;

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

    public ReadingSocket(int port, RoomService service) {
        super(new InetSocketAddress(port));
        this.service = service;
        this.gson = new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .create();
        this.connections = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.userNames = new ConcurrentHashMap<>();
        this.connRoomMap = new ConcurrentHashMap<>();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        String nameFromQuery = extractParamFromQuery(handshake, "name");
        String roomFromQuery = extractParamFromQuery(handshake, "room");
        if (nameFromQuery != null && !nameFromQuery.isEmpty()) {
            userNames.put(conn, nameFromQuery);
        }
        if (roomFromQuery != null && !roomFromQuery.isEmpty()) {
            connRoomMap.put(conn, roomFromQuery);
        }
        System.out.println("[WS] New connection: " + conn.getRemoteSocketAddress()
                + (nameFromQuery != null ? " (" + nameFromQuery + ")" : "")
                + (roomFromQuery != null ? " room=" + roomFromQuery : ""));
    }

    private String extractParamFromQuery(ClientHandshake handshake, String key) {
        try {
            String res = handshake.getResourceDescriptor();
            if (res == null || !res.contains("?")) return null;
            String q = res.substring(res.indexOf('?') + 1);
            for (String pair : q.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && key.equals(kv[0])) {
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
        String roomId = connRoomMap.remove(conn);
        if (roomId != null && name != null) {
            service.leaveRoom(roomId, name);
            broadcastRoomPresence(roomId);
        }
        System.out.println("[WS] Connection closed: " + conn.getRemoteSocketAddress()
                + (name != null ? " (" + name + ")" : ""));
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
            e.printStackTrace();
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
        String userName = userNames.get(conn);
        if (userName == null || userName.isEmpty()) {
            userName = "匿名";
            userNames.put(conn, userName);
        }

        switch (msg.type) {
            case "LIST_ROOMS": {
                List<Map<String, Object>> roomList = new ArrayList<>();
                for (Room r : service.listRooms()) {
                    Map<String, Object> rm = new LinkedHashMap<>();
                    rm.put("id", r.getId());
                    rm.put("name", r.getName());
                    rm.put("hasPasscode", r.getPasscode() != null && !r.getPasscode().isEmpty());
                    rm.put("onlineCount", r.getOnlineCount());
                    rm.put("articleTitle", r.getArticle().getTitle());
                    rm.put("ownerName", r.getOwnerName());
                    rm.put("createdAt", r.getCreatedAt());
                    roomList.add(rm);
                }
                sendTo(conn, new WsMessage("ROOM_LIST", roomList, null));
                break;
            }
            case "CREATE_ROOM": {
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                String name = p.get("name") != null ? p.get("name").toString() : "新共读室";
                String passcode = p.get("passcode") != null ? p.get("passcode").toString() : "";
                Room room = service.createRoom(name, passcode, userName, null);
                connRoomMap.put(conn, room.getId());
                sendTo(conn, new WsMessage("ROOM_CREATED", buildRoomState(room, userName), userName));
                broadcastRoomPresence(room.getId());
                break;
            }
            case "LEAVE_ROOM": {
                String roomId = connRoomMap.remove(conn);
                if (roomId != null && userName != null) {
                    service.leaveRoom(roomId, userName);
                    broadcastRoomPresence(roomId);
                }
                break;
            }
            case "JOIN_ROOM": {
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                String roomId = p.get("roomId") != null ? p.get("roomId").toString() : "default";
                String passcode = p.get("passcode") != null ? p.get("passcode").toString() : "";
                String prevRoom = connRoomMap.get(conn);
                if (prevRoom != null && !prevRoom.equals(roomId)) {
                    service.leaveRoom(prevRoom, userName);
                    broadcastRoomPresence(prevRoom);
                }
                boolean ok = service.joinRoom(roomId, userName, passcode);
                if (ok) {
                    connRoomMap.put(conn, roomId);
                    Room room = service.getRoom(roomId);
                    sendTo(conn, new WsMessage("ROOM_JOINED", buildRoomState(room, userName), userName));
                    broadcastRoomPresence(roomId);
                } else {
                    sendError(conn, "JOIN_ROOM", "房间不存在或口令错误");
                }
                break;
            }
            case "SET_NAME": {
                if (msg.payload != null) {
                    String name = msg.payload.toString().trim();
                    if (!name.isEmpty()) {
                        String oldName = userNames.get(conn);
                        String roomId = connRoomMap.get(conn);
                        if (roomId != null && oldName != null && !oldName.equals(name)) {
                            service.renameUser(roomId, oldName, name);
                            broadcastRoomState(roomId);
                        }
                        userNames.put(conn, name);
                    }
                }
                break;
            }
            case "SET_MODERATOR": {
                String roomId = connRoomMap.get(conn);
                if (roomId == null) { sendError(conn, "SET_MODERATOR", "未加入房间"); break; }
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                String target = p.get("target") != null ? p.get("target").toString() : userName;
                boolean wantMod = Boolean.TRUE.equals(p.get("moderator"));
                if (service.setModerator(roomId, target, wantMod, userName)) {
                    broadcastRoomState(roomId);
                } else {
                    sendError(conn, "SET_MODERATOR", "权限不足");
                }
                break;
            }
            case "PRESENCE_UPDATE": {
                String roomId = connRoomMap.get(conn);
                if (roomId == null) break;
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                String paragraphId = p.get("paragraphId") != null ? p.get("paragraphId").toString() : null;
                Boolean typing = p.get("typing") != null ? (Boolean) p.get("typing") : null;
                service.updatePresence(roomId, userName, paragraphId, typing);
                broadcastRoomPresence(roomId);
                break;
            }
            case "ADD_NOTE": {
                String roomId = connRoomMap.get(conn);
                if (roomId == null) { sendError(conn, "ADD_NOTE", "未加入房间"); break; }
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                String paragraphId = (String) p.get("paragraphId");
                String content = (String) p.get("content");
                Note.NoteType type;
                try {
                    type = Note.NoteType.valueOf((String) p.getOrDefault("type", "THOUGHT"));
                } catch (Exception e) {
                    type = Note.NoteType.THOUGHT;
                }
                Note note = service.addNote(roomId, paragraphId, userName, content, type);
                if (note != null) {
                    broadcastRoom(roomId, new WsMessage("NOTE_ADDED", note, userName));
                }
                break;
            }
            case "ADD_REPLY": {
                String roomId = connRoomMap.get(conn);
                if (roomId == null) { sendError(conn, "ADD_REPLY", "未加入房间"); break; }
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                String noteId = (String) p.get("noteId");
                String parentReplyId = p.get("parentReplyId") != null ? p.get("parentReplyId").toString() : null;
                String content = (String) p.get("content");
                Reply reply = service.addReply(roomId, noteId, parentReplyId, userName, content);
                if (reply != null) {
                    broadcastRoom(roomId, new WsMessage("REPLY_ADDED", reply, userName));
                }
                break;
            }
            case "TOGGLE_LIKE": {
                String roomId = connRoomMap.get(conn);
                if (roomId == null) break;
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                String noteId = (String) p.get("noteId");
                if (service.toggleLike(roomId, noteId, userName)) {
                    Room room = service.getRoom(roomId);
                    Note note = room.getNotes().stream()
                            .filter(n -> noteId.equals(n.getId())).findFirst().orElse(null);
                    if (note != null) {
                        Map<String, Object> payload = new HashMap<>();
                        payload.put("noteId", noteId);
                        Map<String, Object> likes = new HashMap<>();
                        likes.put("count", note.getLikeCount());
                        likes.put("users", note.getLikes());
                        payload.put("likes", likes);
                        payload.put("user", userName);
                        broadcastRoom(roomId, new WsMessage("LIKE_UPDATED", payload, userName));
                    }
                }
                break;
            }
            case "TOGGLE_LIKE_REPLY": {
                String roomId = connRoomMap.get(conn);
                if (roomId == null) break;
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                String replyId = (String) p.get("replyId");
                if (service.toggleLikeReply(roomId, replyId, userName)) {
                    Room room = service.getRoom(roomId);
                    Reply reply = room.getReplies().stream()
                            .filter(r -> replyId.equals(r.getId())).findFirst().orElse(null);
                    if (reply != null) {
                        Map<String, Object> payload = new HashMap<>();
                        payload.put("replyId", replyId);
                        Map<String, Object> likes = new HashMap<>();
                        likes.put("count", reply.getLikeCount());
                        likes.put("users", reply.getLikes());
                        payload.put("likes", likes);
                        payload.put("user", userName);
                        broadcastRoom(roomId, new WsMessage("REPLY_LIKE_UPDATED", payload, userName));
                    }
                }
                break;
            }
            case "TOGGLE_HIGHLIGHT": {
                String roomId = connRoomMap.get(conn);
                if (roomId == null) { sendError(conn, "TOGGLE_HIGHLIGHT", "未加入房间"); break; }
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                String noteId = (String) p.get("noteId");
                if (service.toggleHighlight(roomId, noteId, userName)) {
                    Room room = service.getRoom(roomId);
                    Note note = room.getNotes().stream()
                            .filter(n -> noteId.equals(n.getId())).findFirst().orElse(null);
                    if (note != null) {
                        Map<String, Object> payload = new HashMap<>();
                        payload.put("noteId", noteId);
                        payload.put("highlighted", note.isHighlighted());
                        broadcastRoom(roomId, new WsMessage("HIGHLIGHT_UPDATED", payload, userName));
                    }
                } else {
                    sendError(conn, "TOGGLE_HIGHLIGHT", "仅主持人可标记重点");
                }
                break;
            }
            case "SWITCH_PARAGRAPH": {
                String roomId = connRoomMap.get(conn);
                if (roomId == null) { sendError(conn, "SWITCH_PARAGRAPH", "未加入房间"); break; }
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                String pid = (String) p.get("paragraphId");
                if (service.switchParagraph(roomId, pid, userName)) {
                    broadcastParagraphSwitched(roomId, userName);
                } else {
                    sendError(conn, "SWITCH_PARAGRAPH", "仅主持人可切换段落");
                }
                break;
            }
            case "MOVE_NEXT": {
                String roomId = connRoomMap.get(conn);
                if (roomId == null) { sendError(conn, "MOVE_NEXT", "未加入房间"); break; }
                if (service.moveNext(roomId, userName)) {
                    broadcastParagraphSwitched(roomId, userName);
                } else {
                    sendError(conn, "MOVE_NEXT", "仅主持人可切换段落");
                }
                break;
            }
            case "MOVE_PREV": {
                String roomId = connRoomMap.get(conn);
                if (roomId == null) { sendError(conn, "MOVE_PREV", "未加入房间"); break; }
                if (service.movePrev(roomId, userName)) {
                    broadcastParagraphSwitched(roomId, userName);
                } else {
                    sendError(conn, "MOVE_PREV", "仅主持人可切换段落");
                }
                break;
            }
            case "ADD_TO_QUEUE": {
                String roomId = connRoomMap.get(conn);
                if (roomId == null) { sendError(conn, "ADD_TO_QUEUE", "未加入房间"); break; }
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                String noteId = (String) p.get("noteId");
                if (service.addToDiscussionQueue(roomId, noteId, userName)) {
                    broadcastDiscussionQueue(roomId, userName);
                } else {
                    sendError(conn, "ADD_TO_QUEUE", "仅主持人可操作");
                }
                break;
            }
            case "REMOVE_FROM_QUEUE": {
                String roomId = connRoomMap.get(conn);
                if (roomId == null) break;
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                String noteId = (String) p.get("noteId");
                if (service.removeFromDiscussionQueue(roomId, noteId, userName)) {
                    broadcastDiscussionQueue(roomId, userName);
                } else {
                    sendError(conn, "REMOVE_FROM_QUEUE", "仅主持人可操作");
                }
                break;
            }
            case "REORDER_QUEUE": {
                String roomId = connRoomMap.get(conn);
                if (roomId == null) break;
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                List<String> order = (List<String>) p.get("order");
                if (service.reorderDiscussionQueue(roomId, order, userName)) {
                    broadcastDiscussionQueue(roomId, userName);
                } else {
                    sendError(conn, "REORDER_QUEUE", "仅主持人可操作");
                }
                break;
            }
            case "CLEAR_NOTES_PARAGRAPH": {
                String roomId = connRoomMap.get(conn);
                if (roomId == null) { sendError(conn, "CLEAR_NOTES_PARAGRAPH", "未加入房间"); break; }
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                String paragraphId = p.get("paragraphId") != null ? p.get("paragraphId").toString() : null;
                if (service.clearNotesByParagraph(roomId, paragraphId, userName)) {
                    broadcastRoomState(roomId);
                } else {
                    sendError(conn, "CLEAR_NOTES_PARAGRAPH", "仅主持人或房主可清空");
                }
                break;
            }
            case "IMPORT_ARTICLE": {
                String roomId = connRoomMap.get(conn);
                if (roomId == null) { sendError(conn, "IMPORT_ARTICLE", "未加入房间"); break; }
                Map<String, Object> p = (Map<String, Object>) msg.payload;
                String title = p.get("title") != null ? p.get("title").toString() : null;
                String author = p.get("author") != null ? p.get("author").toString() : null;
                String text = p.get("text") != null ? p.get("text").toString() : "";
                Article a = service.importArticle(roomId, title, author, text, userName);
                if (a != null) {
                    broadcastRoomState(roomId);
                    broadcastRoom(roomId, new WsMessage("ARTICLE_UPDATED", a, userName));
                } else {
                    sendError(conn, "IMPORT_ARTICLE", "仅主持人或房主可导入");
                }
                break;
            }
            case "EXPORT_MARKDOWN": {
                String roomId = connRoomMap.get(conn);
                if (roomId == null) break;
                String md = service.exportMarkdown(roomId);
                Map<String, Object> res = new HashMap<>();
                res.put("format", "markdown");
                res.put("content", md);
                res.put("filename", (service.getRoom(roomId).getName() + ".md").replaceAll("[\\\\/:*?\"<>|]", "_"));
                sendTo(conn, new WsMessage("EXPORT_RESULT", res, null));
                break;
            }
            case "EXPORT_JSON": {
                String roomId = connRoomMap.get(conn);
                if (roomId == null) break;
                Map<String, Object> json = service.exportJson(roomId);
                Map<String, Object> res = new HashMap<>();
                res.put("format", "json");
                res.put("content", json);
                res.put("filename", (service.getRoom(roomId).getName() + ".json").replaceAll("[\\\\/:*?\"<>|]", "_"));
                sendTo(conn, new WsMessage("EXPORT_RESULT", res, null));
                break;
            }
            case "GET_TIMELINE": {
                String roomId = connRoomMap.get(conn);
                if (roomId == null) break;
                Room room = service.getRoom(roomId);
                List<TimelineEvent> timeline = room != null ? room.getTimeline() : new ArrayList<>();
                sendTo(conn, new WsMessage("TIMELINE_DATA", timeline, null));
                break;
            }
            case "REQUEST_STATE": {
                String roomId = connRoomMap.get(conn);
                if (roomId != null) {
                    Room room = service.getRoom(roomId);
                    if (room != null) {
                        sendTo(conn, new WsMessage("STATE_SYNC", buildRoomState(room, userName), null));
                    }
                }
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

    private Map<String, Object> buildRoomState(Room room, String userName) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("roomId", room.getId());
        data.put("roomName", room.getName());
        data.put("ownerName", room.getOwnerName());
        data.put("article", room.getArticle());
        data.put("notes", room.getNotes());
        data.put("replies", room.getReplies());
        data.put("discussionQueue", room.getDiscussionQueue());

        Map<String, Integer> noteCounts = new HashMap<>();
        for (Note n : room.getNotes()) {
            noteCounts.merge(n.getParagraphId(), 1, Integer::sum);
        }
        data.put("noteCounts", noteCounts);

        List<Map<String, Object>> presences = new ArrayList<>();
        for (Presence p : room.getPresences().values()) {
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("userName", p.getUserName());
            pm.put("roomId", p.getRoomId());
            pm.put("paragraphId", p.getParagraphId());
            pm.put("typing", p.isTyping() && (System.currentTimeMillis() - p.getTypingSince()) < 15000);
            pm.put("typingSince", p.getTypingSince());
            pm.put("joinedAt", p.getJoinedAt());
            pm.put("lastActiveAt", p.getLastActiveAt());
            pm.put("isOwner", p.isOwner());
            pm.put("isModerator", p.isModerator());
            presences.add(pm);
        }
        data.put("presences", presences);

        Presence self = room.getPresence(userName);
        data.put("isOwner", self != null && self.isOwner());
        data.put("isModerator", self != null && self.isModerator());

        List<String> typingUsers = room.getTypingUsers();
        data.put("typingUsers", typingUsers);

        data.put("onlineCount", room.getOnlineCount());
        List<String> onlineNames = new ArrayList<>();
        for (Presence p : room.getPresences().values()) {
            onlineNames.add(p.getUserName());
        }
        data.put("onlineNames", onlineNames);

        List<String> mods = new ArrayList<>();
        for (Presence p : room.getPresences().values()) {
            if (p.isModerator()) mods.add(p.getUserName());
        }
        data.put("moderators", mods);

        return data;
    }

    private void broadcastParagraphSwitched(String roomId, String userName) {
        Room room = service.getRoom(roomId);
        if (room == null) return;
        Article article = room.getArticle();
        Map<String, Object> payload = new HashMap<>();
        payload.put("paragraphId", article.getCurrentParagraphId());
        payload.put("index", article.getCurrentParagraphIndex());
        broadcastRoom(roomId, new WsMessage("PARAGRAPH_SWITCHED", payload, userName));
    }

    private void broadcastDiscussionQueue(String roomId, String userName) {
        Room room = service.getRoom(roomId);
        if (room == null) return;
        Map<String, Object> payload = new HashMap<>();
        payload.put("discussionQueue", new ArrayList<>(room.getDiscussionQueue()));
        broadcastRoom(roomId, new WsMessage("DISCUSSION_QUEUE_UPDATED", payload, userName));
    }

    private void broadcastRoomPresence(String roomId) {
        Room room = service.getRoom(roomId);
        if (room == null) return;
        Map<String, Object> payload = new HashMap<>();
        payload.put("onlineCount", room.getOnlineCount());
        List<String> names = new ArrayList<>();
        List<Map<String, Object>> details = new ArrayList<>();
        for (Presence p : room.getPresences().values()) {
            names.add(p.getUserName());
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("userName", p.getUserName());
            pm.put("paragraphId", p.getParagraphId());
            pm.put("typing", p.isTyping() && (System.currentTimeMillis() - p.getTypingSince()) < 15000);
            pm.put("isOwner", p.isOwner());
            pm.put("isModerator", p.isModerator());
            pm.put("joinedAt", p.getJoinedAt());
            pm.put("lastActiveAt", p.getLastActiveAt());
            details.add(pm);
        }
        payload.put("names", names);
        payload.put("presences", details);
        payload.put("typingUsers", room.getTypingUsers());
        List<String> mods = new ArrayList<>();
        for (Presence p : room.getPresences().values()) {
            if (p.isModerator()) mods.add(p.getUserName());
        }
        payload.put("moderators", mods);
        broadcastRoom(roomId, new WsMessage("PRESENCE_UPDATED", payload, null));
    }

    private void broadcastRoomState(String roomId) {
        Room room = service.getRoom(roomId);
        if (room == null) return;
        broadcastRoom(roomId, new WsMessage("ROOM_STATE", buildMinimalRoomState(room), null));
    }

    private Map<String, Object> buildMinimalRoomState(Room room) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("discussionQueue", room.getDiscussionQueue());
        List<String> mods = new ArrayList<>();
        for (Presence p : room.getPresences().values()) {
            if (p.isModerator()) mods.add(p.getUserName());
        }
        data.put("moderators", mods);
        return data;
    }

    private void sendError(WebSocket conn, String action, String reason) {
        Map<String, Object> err = new HashMap<>();
        err.put("action", action);
        err.put("reason", reason);
        sendTo(conn, new WsMessage("ERROR", err, null));
    }

    private void broadcastRoom(String roomId, WsMessage msg) {
        String json = gson.toJson(msg);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        String utf8Json = new String(bytes, StandardCharsets.UTF_8);
        for (WebSocket c : connections) {
            if (c != null && c.isOpen() && roomId.equals(connRoomMap.get(c))) {
                c.send(utf8Json);
            }
        }
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
