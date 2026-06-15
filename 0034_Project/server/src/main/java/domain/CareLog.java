package domain;

import java.time.LocalDateTime;

public class CareLog {
    private String id;
    private CareType type;
    private String note;
    private LocalDateTime timestamp;

    public CareLog() {
        this.timestamp = LocalDateTime.now();
    }

    public CareLog(String id, CareType type, String note) {
        this.id = id;
        this.type = type;
        this.note = note;
        this.timestamp = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CareType getType() {
        return type;
    }

    public void setType(CareType type) {
        this.type = type;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
