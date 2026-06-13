package domain;

import java.util.ArrayList;
import java.util.List;

public class Article {
    private String id;
    private String title;
    private String author;
    private List<Paragraph> paragraphs;
    private String currentParagraphId;

    public Article() {
        this.paragraphs = new ArrayList<>();
    }

    public Article(String id, String title, String author, List<Paragraph> paragraphs) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.paragraphs = paragraphs;
        if (!paragraphs.isEmpty()) {
            this.currentParagraphId = paragraphs.get(0).getId();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public List<Paragraph> getParagraphs() { return paragraphs; }
    public void setParagraphs(List<Paragraph> paragraphs) { this.paragraphs = paragraphs; }

    public String getCurrentParagraphId() { return currentParagraphId; }
    public void setCurrentParagraphId(String currentParagraphId) { this.currentParagraphId = currentParagraphId; }

    public Paragraph getCurrentParagraph() {
        if (currentParagraphId == null) return null;
        for (Paragraph p : paragraphs) {
            if (currentParagraphId.equals(p.getId())) return p;
        }
        return null;
    }

    public int getCurrentParagraphIndex() {
        Paragraph current = getCurrentParagraph();
        return current == null ? 0 : current.getIndex();
    }

    public void moveToNextParagraph() {
        if (paragraphs == null || paragraphs.isEmpty()) return;
        int currentIdx = getCurrentParagraphIndex();
        if (currentIdx < paragraphs.size() - 1) {
            this.currentParagraphId = paragraphs.get(currentIdx + 1).getId();
        }
    }

    public void moveToPrevParagraph() {
        if (paragraphs == null || paragraphs.isEmpty()) return;
        int currentIdx = getCurrentParagraphIndex();
        if (currentIdx > 0) {
            this.currentParagraphId = paragraphs.get(currentIdx - 1).getId();
        }
    }

    public boolean canMoveNext() {
        return paragraphs != null && getCurrentParagraphIndex() < paragraphs.size() - 1;
    }

    public boolean canMovePrev() {
        return paragraphs != null && getCurrentParagraphIndex() > 0;
    }
}
