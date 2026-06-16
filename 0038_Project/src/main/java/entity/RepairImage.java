package entity;

import java.time.LocalDateTime;

public class RepairImage {
    private String id;
    private String fileName;
    private String filePath;
    private LocalDateTime uploadTime;
    private String description;

    public RepairImage() {
    }

    public RepairImage(String id, String fileName, String filePath, String description) {
        this.id = id;
        this.fileName = fileName;
        this.filePath = filePath;
        this.uploadTime = LocalDateTime.now();
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
