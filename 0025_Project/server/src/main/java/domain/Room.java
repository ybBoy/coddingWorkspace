package domain;

import java.util.*;

public class Room {
    private String id;
    private String name;
    private String passcode;
    private Article article;
    private List<Note> notes;
    private List<Reply> replies;
    private List<String> discussionQueue;
    private Map<String, Presence> presences;
    private List<TimelineEvent> timeline;
    private String ownerName;
    private long createdAt;

    public Room() {
        this.notes = new ArrayList<>();
        this.replies = new ArrayList<>();
        this.discussionQueue = new ArrayList<>();
        this.presences = new LinkedHashMap<>();
        this.timeline = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public Room(String id, String name, String passcode, Article article) {
        this.id = id;
        this.name = name;
        this.passcode = passcode;
        this.article = article;
        this.notes = new ArrayList<>();
        this.replies = new ArrayList<>();
        this.discussionQueue = new ArrayList<>();
        this.presences = new LinkedHashMap<>();
        this.timeline = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPasscode() { return passcode; }
    public void setPasscode(String passcode) { this.passcode = passcode; }

    public Article getArticle() { return article; }
    public void setArticle(Article article) { this.article = article; }

    public List<Note> getNotes() { return notes; }
    public void setNotes(List<Note> notes) { this.notes = notes; }

    public List<Reply> getReplies() { return replies; }
    public void setReplies(List<Reply> replies) { this.replies = replies; }

    public List<String> getDiscussionQueue() { return discussionQueue; }
    public void setDiscussionQueue(List<String> discussionQueue) { this.discussionQueue = discussionQueue; }

    public Map<String, Presence> getPresences() { return presences; }
    public void setPresences(Map<String, Presence> presences) { this.presences = presences; }

    public List<TimelineEvent> getTimeline() { return timeline; }
    public void setTimeline(List<TimelineEvent> timeline) { this.timeline = timeline; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public void addTimelineEvent(TimelineEvent event) {
        if (this.timeline == null) this.timeline = new ArrayList<>();
        this.timeline.add(event);
    }

    public List<Reply> getRepliesForNote(String noteId) {
        List<Reply> result = new ArrayList<>();
        for (Reply r : replies) {
            if (noteId.equals(r.getNoteId())) result.add(r);
        }
        result.sort(Comparator.comparingLong(Reply::getCreatedAt));
        return result;
    }

    public Presence getPresence(String userName) {
        return presences == null ? null : presences.get(userName);
    }

    public void setPresence(String userName, Presence presence) {
        if (presences == null) presences = new LinkedHashMap<>();
        presences.put(userName, presence);
    }

    public void removePresence(String userName) {
        if (presences != null) presences.remove(userName);
    }

    public int getOnlineCount() {
        return presences == null ? 0 : presences.size();
    }

    public List<String> getTypingUsers() {
        List<String> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        if (presences != null) {
            for (Presence p : presences.values()) {
                if (p.isTyping() && (now - p.getTypingSince()) < 15000) {
                    result.add(p.getUserName());
                }
            }
        }
        return result;
    }

    public boolean validatePasscode(String input) {
        if (passcode == null || passcode.isEmpty()) return true;
        return passcode.equals(input);
    }
}
