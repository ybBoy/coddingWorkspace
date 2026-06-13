package service;

import domain.*;
import store.JsonFileStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RoomService {
    private final Map<String, Room> rooms;
    private final JsonFileStore store;

    public RoomService(JsonFileStore store) {
        this.store = store;
        this.rooms = new ConcurrentHashMap<>();

        Optional<JsonFileStore.StoreData> loaded = store.load();
        if (loaded.isPresent()) {
            JsonFileStore.StoreData data = loaded.get();
            if (data.getRooms() != null) {
                for (Room r : data.getRooms()) {
                    if (r != null && r.getId() != null) {
                        if (r.getPresences() != null) {
                            r.getPresences().clear();
                        }
                        rooms.put(r.getId(), r);
                    }
                }
            }
        }

        if (rooms.isEmpty()) {
            Room defaultRoom = createDefaultRoom();
            rooms.put(defaultRoom.getId(), defaultRoom);
        }
    }

    private Room createDefaultRoom() {
        List<Paragraph> paragraphs = new ArrayList<>();
        String[] contents = {
            "读书之法，在循序而渐进，熟读而精思。先须熟读，使其言皆若出于吾之口。继以精思，使其意皆若出于吾之心。然后可以有得尔。",
            "凡读书，须要读得字字响亮，不可误一字，不可少一字，不可多一字，不可倒一字，不可牵强暗记，只是要多诵数遍，自然上口，久远不忘。",
            "古人云，读书百遍，其义自见。谓读得熟，则不待解说，自晓其义也。余尝谓，读书有三到，谓心到，眼到，口到。",
            "心不在此，则眼不看仔细，心眼既不专一，却只漫浪诵读，决不能记，记亦不能久也。三到之中，心到最急。心既到矣，眼口岂不到乎？",
            "读书无疑者，须教有疑，有疑者，却要无疑，到这里方是长进。读书有始有终，不可半途而废，须是今日格一件，明日又格一件，积习既久，自然脱然有贯通处。"
        };
        for (int i = 0; i < contents.length; i++) {
            paragraphs.add(new Paragraph("p_" + i, i, contents[i]));
        }
        Article article = new Article("art_default", "读书要略", "朱熹（节选）", paragraphs);
        Room room = new Room("default", "公共共读室", "", article);
        room.setOwnerName("system");
        return room;
    }

    public synchronized Room createRoom(String name, String passcode, String ownerName, Article customArticle) {
        String id = "room_" + UUID.randomUUID().toString().substring(0, 8);
        Article article = customArticle != null ? customArticle : createDefaultArticle(id);
        Room room = new Room(id, name, passcode, article);
        room.setOwnerName(ownerName);
        Presence ownerPresence = new Presence(ownerName, id, article.getCurrentParagraphId());
        ownerPresence.setOwner(true);
        ownerPresence.setModerator(true);
        room.setPresence(ownerName, ownerPresence);
        rooms.put(id, room);
        addTimelineEvent(room, TimelineEvent.EventType.JOIN, ownerName, null);
        persist();
        return room;
    }

    private Article createDefaultArticle(String roomId) {
        List<Paragraph> paragraphs = new ArrayList<>();
        String[] contents = {
            "读书之法，在循序而渐进，熟读而精思。先须熟读，使其言皆若出于吾之口。继以精思，使其意皆若出于吾之心。然后可以有得尔。",
            "凡读书，须要读得字字响亮，不可误一字，不可少一字，不可多一字，不可倒一字，不可牵强暗记，只是要多诵数遍，自然上口，久远不忘。",
            "古人云，读书百遍，其义自见。谓读得熟，则不待解说，自晓其义也。余尝谓，读书有三到，谓心到，眼到，口到。",
            "心不在此，则眼不看仔细，心眼既不专一，却只漫浪诵读，决不能记，记亦不能久也。三到之中，心到最急。心既到矣，眼口岂不到乎？",
            "读书无疑者，须教有疑，有疑者，却要无疑，到这里方是长进。读书有始有终，不可半途而废，须是今日格一件，明日又格一件，积习既久，自然脱然有贯通处。"
        };
        for (int i = 0; i < contents.length; i++) {
            paragraphs.add(new Paragraph(roomId + "_p_" + i, i, contents[i]));
        }
        return new Article(roomId + "_art", "读书要略", "朱熹（节选）", paragraphs);
    }

    public Room getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public List<Room> listRooms() {
        return new ArrayList<>(rooms.values());
    }

    public boolean joinRoom(String roomId, String userName, String passcode) {
        Room room = rooms.get(roomId);
        if (room == null) return false;
        if (!room.validatePasscode(passcode)) return false;
        Presence p = room.getPresence(userName);
        if (p == null) {
            p = new Presence(userName, roomId, room.getArticle().getCurrentParagraphId());
            room.setPresence(userName, p);
            addTimelineEvent(room, TimelineEvent.EventType.JOIN, userName, null);
        }
        p.setLastActiveAt(System.currentTimeMillis());
        persist();
        return true;
    }

    public void leaveRoom(String roomId, String userName) {
        Room room = rooms.get(roomId);
        if (room == null) return;
        room.removePresence(userName);
        addTimelineEvent(room, TimelineEvent.EventType.LEAVE, userName, null);
        persist();
    }

    public Note addNote(String roomId, String paragraphId, String author, String content, Note.NoteType type) {
        Room room = rooms.get(roomId);
        if (room == null) return null;
        if (author == null || author.trim().isEmpty()) return null;
        if (content == null || content.trim().isEmpty()) return null;
        if (paragraphId == null) return null;

        String id = "note_" + UUID.randomUUID().toString().substring(0, 8);
        Note note = new Note(id, paragraphId, author.trim(), content.trim(), type);
        room.getNotes().add(note);

        Map<String, Object> data = new HashMap<>();
        data.put("noteId", note.getId());
        data.put("paragraphId", paragraphId);
        data.put("content", content);
        data.put("type", type.name());
        addTimelineEvent(room, TimelineEvent.EventType.NOTE_ADDED, author, data);
        persist();
        return note;
    }

    public Reply addReply(String roomId, String noteId, String parentReplyId, String author, String content) {
        Room room = rooms.get(roomId);
        if (room == null) return null;
        if (author == null || author.trim().isEmpty()) return null;
        if (content == null || content.trim().isEmpty()) return null;
        if (noteId == null) return null;

        String id = "reply_" + UUID.randomUUID().toString().substring(0, 8);
        Reply reply = new Reply(id, noteId, parentReplyId, author.trim(), content.trim());
        room.getReplies().add(reply);

        Map<String, Object> data = new HashMap<>();
        data.put("replyId", id);
        data.put("noteId", noteId);
        data.put("parentReplyId", parentReplyId);
        data.put("content", content);
        data.put("author", author);
        data.put("createdAt", reply.getCreatedAt());
        addTimelineEvent(room, TimelineEvent.EventType.REPLY_ADDED, author, data);
        persist();
        return reply;
    }

    public synchronized boolean toggleLike(String roomId, String noteId, String user) {
        Room room = rooms.get(roomId);
        if (room == null || user == null || user.trim().isEmpty()) return false;
        for (Note note : room.getNotes()) {
            if (noteId.equals(note.getId())) {
                Set<String> likes = note.getLikes();
                if (likes.contains(user)) {
                    note.removeLike(user);
                } else {
                    note.addLike(user);
                }
                Map<String, Object> data = new HashMap<>();
                data.put("noteId", noteId);
                data.put("liked", likes.contains(user));
                addTimelineEvent(room, TimelineEvent.EventType.LIKE, user, data);
                persist();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean toggleLikeReply(String roomId, String replyId, String user) {
        Room room = rooms.get(roomId);
        if (room == null || user == null || user.trim().isEmpty()) return false;
        for (Reply reply : room.getReplies()) {
            if (replyId.equals(reply.getId())) {
                Set<String> likes = reply.getLikes();
                if (likes.contains(user)) {
                    reply.removeLike(user);
                } else {
                    reply.addLike(user);
                }
                Map<String, Object> data = new HashMap<>();
                data.put("replyId", replyId);
                data.put("liked", likes.contains(user));
                addTimelineEvent(room, TimelineEvent.EventType.LIKE, user, data);
                persist();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean toggleHighlight(String roomId, String noteId, String userName) {
        Room room = rooms.get(roomId);
        if (room == null) return false;
        Presence p = room.getPresence(userName);
        if (p == null || !p.isModerator()) return false;
        for (Note note : room.getNotes()) {
            if (noteId.equals(note.getId())) {
                note.setHighlighted(!note.isHighlighted());
                Map<String, Object> data = new HashMap<>();
                data.put("noteId", noteId);
                data.put("highlighted", note.isHighlighted());
                addTimelineEvent(room, TimelineEvent.EventType.HIGHLIGHT, userName, data);
                persist();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean switchParagraph(String roomId, String paragraphId, String userName) {
        Room room = rooms.get(roomId);
        if (room == null) return false;
        Presence p = room.getPresence(userName);
        if (p == null || !p.isModerator()) return false;
        if (paragraphId == null) return false;
        for (Paragraph para : room.getArticle().getParagraphs()) {
            if (paragraphId.equals(para.getId())) {
                room.getArticle().setCurrentParagraphId(paragraphId);
                Map<String, Object> data = new HashMap<>();
                data.put("paragraphId", paragraphId);
                data.put("index", para.getIndex());
                addTimelineEvent(room, TimelineEvent.EventType.PARAGRAPH_SWITCH, userName, data);
                persist();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean moveNext(String roomId, String userName) {
        Room room = rooms.get(roomId);
        if (room == null) return false;
        Presence p = room.getPresence(userName);
        if (p == null || !p.isModerator()) return false;
        Article article = room.getArticle();
        if (article.canMoveNext()) {
            article.moveToNextParagraph();
            Map<String, Object> data = new HashMap<>();
            data.put("paragraphId", article.getCurrentParagraphId());
            data.put("index", article.getCurrentParagraphIndex());
            addTimelineEvent(room, TimelineEvent.EventType.PARAGRAPH_SWITCH, userName, data);
            persist();
            return true;
        }
        return false;
    }

    public synchronized boolean movePrev(String roomId, String userName) {
        Room room = rooms.get(roomId);
        if (room == null) return false;
        Presence p = room.getPresence(userName);
        if (p == null || !p.isModerator()) return false;
        Article article = room.getArticle();
        if (article.canMovePrev()) {
            article.moveToPrevParagraph();
            Map<String, Object> data = new HashMap<>();
            data.put("paragraphId", article.getCurrentParagraphId());
            data.put("index", article.getCurrentParagraphIndex());
            addTimelineEvent(room, TimelineEvent.EventType.PARAGRAPH_SWITCH, userName, data);
            persist();
            return true;
        }
        return false;
    }

    public boolean addToDiscussionQueue(String roomId, String noteId, String userName) {
        Room room = rooms.get(roomId);
        if (room == null) return false;
        Presence p = room.getPresence(userName);
        if (p == null || !p.isModerator()) return false;
        if (!room.getDiscussionQueue().contains(noteId)) {
            room.getDiscussionQueue().add(noteId);
            broadcastDiscussionQueueUpdated(room, userName);
            persist();
            return true;
        }
        return false;
    }

    public boolean removeFromDiscussionQueue(String roomId, String noteId, String userName) {
        Room room = rooms.get(roomId);
        if (room == null) return false;
        Presence p = room.getPresence(userName);
        if (p == null || !p.isModerator()) return false;
        boolean removed = room.getDiscussionQueue().remove(noteId);
        if (removed) {
            broadcastDiscussionQueueUpdated(room, userName);
            persist();
        }
        return removed;
    }

    public boolean reorderDiscussionQueue(String roomId, List<String> orderedNoteIds, String userName) {
        Room room = rooms.get(roomId);
        if (room == null) return false;
        Presence p = room.getPresence(userName);
        if (p == null || !p.isModerator()) return false;
        room.setDiscussionQueue(new ArrayList<>(orderedNoteIds));
        broadcastDiscussionQueueUpdated(room, userName);
        persist();
        return true;
    }

    private void broadcastDiscussionQueueUpdated(Room room, String userName) {
        Map<String, Object> data = new HashMap<>();
        data.put("discussionQueue", new ArrayList<>(room.getDiscussionQueue()));
        addTimelineEvent(room, TimelineEvent.EventType.DISCUSSION_QUEUE_UPDATED, userName, data);
    }

    public void updatePresence(String roomId, String userName, String paragraphId, Boolean typing) {
        Room room = rooms.get(roomId);
        if (room == null) return;
        Presence p = room.getPresence(userName);
        if (p == null) return;
        if (paragraphId != null) {
            p.setParagraphId(paragraphId);
        }
        if (typing != null) {
            p.setTyping(typing);
            p.setTypingSince(typing ? System.currentTimeMillis() : 0);
            if (typing) {
                Map<String, Object> data = new HashMap<>();
                data.put("paragraphId", p.getParagraphId());
                addTimelineEvent(room, TimelineEvent.EventType.TYPING_START, userName, data);
            } else {
                addTimelineEvent(room, TimelineEvent.EventType.TYPING_END, userName, null);
            }
        }
        p.setLastActiveAt(System.currentTimeMillis());
    }

    public boolean setModerator(String roomId, String targetUserName, boolean moderator, String requesterName) {
        Room room = rooms.get(roomId);
        if (room == null) return false;
        Presence requester = room.getPresence(requesterName);
        if (requester == null) return false;
        if (!requester.isOwner() && !requester.isModerator()) return false;
        Presence target = room.getPresence(targetUserName);
        if (target == null) return false;
        target.setModerator(moderator);
        persist();
        return true;
    }

    public synchronized boolean renameUser(String roomId, String oldName, String newName) {
        Room room = rooms.get(roomId);
        if (room == null) return false;
        if (oldName == null || newName == null || oldName.equals(newName)) return false;
        if (room.getPresence(newName) != null) return false;

        Presence p = room.getPresence(oldName);
        if (p != null) {
            room.removePresence(oldName);
            p.setUserName(newName);
            room.setPresence(newName, p);
        }

        if (oldName.equals(room.getOwnerName())) {
            room.setOwnerName(newName);
        }

        for (Note note : room.getNotes()) {
            if (oldName.equals(note.getAuthor())) {
                note.setAuthor(newName);
            }
            if (note.getLikes() != null) {
                if (note.getLikes().remove(oldName)) {
                    note.getLikes().add(newName);
                }
            }
        }

        for (Reply reply : room.getReplies()) {
            if (oldName.equals(reply.getAuthor())) {
                reply.setAuthor(newName);
            }
            if (reply.getLikes() != null) {
                if (reply.getLikes().remove(oldName)) {
                    reply.getLikes().add(newName);
                }
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("oldName", oldName);
        data.put("newName", newName);
        addTimelineEvent(room, TimelineEvent.EventType.USER_RENAMED, newName, data);
        persist();
        return true;
    }

    public Article importArticle(String roomId, String title, String author, String rawText, String userName) {
        Room room = rooms.get(roomId);
        if (room == null) return null;
        Presence p = room.getPresence(userName);
        if (p == null || !p.isModerator() && !p.isOwner()) return null;

        String[] parts = rawText.split("\\n\\s*\\n");
        List<Paragraph> paragraphs = new ArrayList<>();
        int idx = 0;
        long ts = System.currentTimeMillis();
        String roomPrefix = roomId + "_" + ts + "_";
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                paragraphs.add(new Paragraph(roomPrefix + "p_" + idx, idx, trimmed));
                idx++;
            }
        }
        if (paragraphs.isEmpty()) return null;

        Article newArticle = new Article(roomPrefix + "art",
                title != null ? title : "导入文章",
                author != null ? author : "",
                paragraphs);
        newArticle.setCurrentParagraphId(paragraphs.get(0).getId());
        room.setArticle(newArticle);

        room.getNotes().clear();
        room.getReplies().clear();
        room.getDiscussionQueue().clear();

        Map<String, Object> data = new HashMap<>();
        data.put("articleId", newArticle.getId());
        data.put("title", newArticle.getTitle());
        data.put("paragraphCount", paragraphs.size());
        data.put("clearedOldData", true);
        addTimelineEvent(room, TimelineEvent.EventType.ARTICLE_UPDATED, userName, data);
        persist();
        return newArticle;
    }

    public String exportMarkdown(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) return null;
        StringBuilder sb = new StringBuilder();
        Article article = room.getArticle();

        sb.append("# ").append(article.getTitle()).append("\n");
        if (article.getAuthor() != null && !article.getAuthor().isEmpty()) {
            sb.append("> 作者：").append(article.getAuthor()).append("\n");
        }
        sb.append("> 房间：").append(room.getName()).append("  \n");
        sb.append("> 导出时间：").append(new Date().toString()).append("\n\n");
        sb.append("---\n\n");

        for (Paragraph para : article.getParagraphs()) {
            sb.append("## 第 ").append(para.getIndex() + 1).append(" 段\n\n");
            sb.append(para.getContent()).append("\n\n");

            List<Note> paraNotes = room.getNotes().stream()
                    .filter(n -> para.getId().equals(n.getParagraphId()))
                    .sorted(Comparator.comparingLong(Note::getCreatedAt))
                    .collect(Collectors.toList());

            int noteNum = 1;
            for (Note note : paraNotes) {
                String typeLabel;
                switch (note.getType()) {
                    case QUESTION: typeLabel = "❓ 问题"; break;
                    case SUPPLEMENT: typeLabel = "📝 补充"; break;
                    default: typeLabel = "💭 想法";
                }
                sb.append("### ").append(typeLabel).append(" #").append(noteNum)
                        .append(" - ").append(note.getAuthor());
                if (note.isHighlighted()) sb.append(" ⭐");
                sb.append("\n\n");
                sb.append(note.getContent()).append("\n\n");
                if (note.getLikeCount() > 0) {
                    sb.append("> 👍 ").append(note.getLikeCount()).append(" 赞  \n");
                    sb.append("> 点赞：").append(String.join("、", note.getLikes())).append("\n\n");
                }

                List<Reply> replies = room.getRepliesForNote(note.getId());
                if (!replies.isEmpty()) {
                    sb.append("**回复：**\n\n");
                    for (Reply reply : replies) {
                        sb.append("- **").append(reply.getAuthor()).append("**：")
                                .append(reply.getContent());
                        if (reply.getLikeCount() > 0) {
                            sb.append(" (👍 ").append(reply.getLikeCount()).append(")");
                        }
                        sb.append("\n");
                    }
                    sb.append("\n");
                }
                noteNum++;
            }
            sb.append("---\n\n");
        }

        if (!room.getDiscussionQueue().isEmpty()) {
            sb.append("## 📋 讨论队列\n\n");
            int order = 1;
            for (String noteId : room.getDiscussionQueue()) {
                Note note = room.getNotes().stream()
                        .filter(n -> noteId.equals(n.getId())).findFirst().orElse(null);
                if (note != null) {
                    sb.append(order).append(". **")
                            .append(note.getAuthor()).append("**：")
                            .append(truncate(note.getContent(), 60)).append("\n");
                    order++;
                }
            }
            sb.append("\n");
        }

        sb.append("## 📊 统计\n\n");
        sb.append("- 段落数：").append(article.getParagraphs().size()).append("\n");
        sb.append("- 批注数：").append(room.getNotes().size()).append("\n");
        sb.append("- 回复数：").append(room.getReplies().size()).append("\n");
        sb.append("- 讨论队列：").append(room.getDiscussionQueue().size()).append(" 条\n");

        return sb.toString();
    }

    public Map<String, Object> exportJson(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) return null;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roomId", room.getId());
        result.put("roomName", room.getName());
        result.put("ownerName", room.getOwnerName());
        result.put("createdAt", room.getCreatedAt());
        result.put("exportedAt", System.currentTimeMillis());
        result.put("article", room.getArticle());
        result.put("notes", room.getNotes());
        result.put("replies", room.getReplies());
        result.put("discussionQueue", room.getDiscussionQueue());
        List<Map<String, Object>> presenceList = new ArrayList<>();
        for (Presence p : room.getPresences().values()) {
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("userName", p.getUserName());
            pm.put("paragraphId", p.getParagraphId());
            pm.put("isOwner", p.isOwner());
            pm.put("isModerator", p.isModerator());
            pm.put("joinedAt", p.getJoinedAt());
            pm.put("lastActiveAt", p.getLastActiveAt());
            presenceList.add(pm);
        }
        result.put("participants", presenceList);
        result.put("timeline", room.getTimeline());
        return result;
    }

    private void addTimelineEvent(Room room, TimelineEvent.EventType type, String userName, Map<String, Object> data) {
        TimelineEvent event = new TimelineEvent(type, userName, data);
        room.addTimelineEvent(event);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private void persist() {
        try {
            store.saveRooms(new ArrayList<>(rooms.values()));
        } catch (Exception e) {
            System.err.println("[RoomService] Persist error: " + e.getMessage());
        }
    }
}
