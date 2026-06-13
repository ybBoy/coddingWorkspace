package domain;

public class Paragraph {
    private String id;
    private int index;
    private String content;

    public Paragraph() {}

    public Paragraph(String id, int index, String content) {
        this.id = id;
        this.index = index;
        this.content = content;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
