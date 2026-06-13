package domain;

import java.util.HashSet;
import java.util.Set;

public class Reply {
    private String id;
    private String noteId;
    private String parentReplyId;
    private String author;
    private String content;
    private Set<String> likes;
    private long createdAt;

    public Reply() {
        this.likes = new HashSet<>();
        this.createdAt = System.currentTimeMillis();
    }

    public Reply(String id, String noteId, String parentReplyId, String author, String content) {
        this.id = id;
        this.noteId = noteId;
        this.parentReplyId = parentReplyId;
        this.author = author;
        this.content = content;
        this.likes = new HashSet<>();
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNoteId() { return noteId; }
    public void setNoteId(String noteId) { this.noteId = noteId; }

    public String getParentReplyId() { return parentReplyId; }
    public void setParentReplyId(String parentReplyId) { this.parentReplyId = parentReplyId; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Set<String> getLikes() { return likes; }
    public void setLikes(Set<String> likes) { this.likes = likes; }

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
