package model;

public enum ItemStatus {
    PENDING("待处理"),
    SELLING("出售中"),
    SOLD("已出售"),
    GIVEN_AWAY("已送出"),
    DISCARDED("已丢弃"),
    KEPT("已保留");

    private final String displayName;

    ItemStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ItemStatus fromDisplayName(String displayName) {
        for (ItemStatus status : values()) {
            if (status.displayName.equals(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + displayName);
    }

    public static ItemStatus fromNameOrDisplayName(String value) {
        for (ItemStatus status : values()) {
            if (status.name().equalsIgnoreCase(value) || status.displayName.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }

    public static ItemStatus getDefaultForPlan(DisposePlan plan) {
        if (plan == null) return PENDING;
        switch (plan) {
            case KEEP: return KEPT;
            case SELL: return SELLING;
            case GIVE_AWAY:
            case DISCARD:
            default: return PENDING;
        }
    }

    public boolean isCompleted() {
        return this == SOLD || this == GIVEN_AWAY || this == DISCARDED || this == KEPT;
    }
}
