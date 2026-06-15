package entity;

public enum RepairStatus {
    PENDING("待处理"),
    IN_PROGRESS("维修中"),
    COMPLETED("已完成");

    private String displayName;

    RepairStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static RepairStatus fromDisplayName(String displayName) {
        for (RepairStatus status : values()) {
            if (status.displayName.equals(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + displayName);
    }
}
