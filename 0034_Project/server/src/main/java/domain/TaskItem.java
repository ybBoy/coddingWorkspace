package domain;

public class TaskItem {
    private String plantId;
    private String plantName;
    private String taskType;
    private String reason;
    private String icon;

    public TaskItem() {}

    public TaskItem(String plantId, String plantName, String taskType, String reason, String icon) {
        this.plantId = plantId;
        this.plantName = plantName;
        this.taskType = taskType;
        this.reason = reason;
        this.icon = icon;
    }

    public String getPlantId() {
        return plantId;
    }

    public void setPlantId(String plantId) {
        this.plantId = plantId;
    }

    public String getPlantName() {
        return plantName;
    }

    public void setPlantName(String plantName) {
        this.plantName = plantName;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
