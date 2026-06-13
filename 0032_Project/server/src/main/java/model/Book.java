package model;

public class Book {
    private String id;
    private String title;
    private String author;
    private String status;
    private String remark;
    private long createdAt;

    public Book() {
    }

    public Book(String id, String title, String author, String status, String remark, long createdAt) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.status = status;
        this.remark = remark;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
