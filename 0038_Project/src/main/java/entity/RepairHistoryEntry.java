package entity;

import java.time.LocalDateTime;

public class RepairHistoryEntry {
    private LocalDateTime timestamp;
    private String action;
    private String oldValue;
    private String newValue;
    private String remark;

    public RepairHistoryEntry() {
        this.timestamp = LocalDateTime.now();
    }

    public RepairHistoryEntry(String action, String oldValue, String newValue, String remark) {
        this.timestamp = LocalDateTime.now();
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.remark = remark;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
