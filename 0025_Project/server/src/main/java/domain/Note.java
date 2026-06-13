package domain;

import java.util.HashSet;
import java.util.Set;

public class Note {
    private String id;
    private String paragraphId;
    private String author;
    private String content;
    private NoteType type;
    private Set<String> likes;
    private boolean highlighted;
    private long createdAt;

    public enum NoteType {
        THOUGHT, QUESTION, SUPPLEMENT
    }

    public Note() {
        this.likes = new HashSet<>();
        this.createdAt = System.currentTimeMillis();
    }

    public Note(String id, String paragraphId, String author, String content, NoteType type) {
        this.id = id;
        this.paragraphId = paragraphId;
        this.author = author;
        this.content = content;
        this.type = type;
        this.likes = new HashSet<>();
        this.highlighted = false;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getParagraphId() { return paragraphId; }
    public void setParagraphId(String paragraphId) { this.paragraphId = paragraphId; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public NoteType getType() { return type; }
    public void setType(NoteType type) { this.type = type; }

    public Set<String> getLikes() { return likes; }
    public void setLikes(Set<String> likes) { this.likes = likes; }

    public boolean isHighlighted() { return highlighted; }
    public void setHighlighted(boolean highlighted) { this.highlighted = highlighted; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public void addLike(String user) {
        if (this.likes == null) this.likes = new HashSet<>();
        this.likes.add(user);
    }

    public void removeLike(String user) {
        if (this.likes != null) this.likes.remove(user);
    }

    public int getLikeCount() {
        return likes == null ? 0 : likes.size();
    }
}
