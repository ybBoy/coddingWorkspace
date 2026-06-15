package model;

public enum DisposePlan {
    KEEP("留下"),
    GIVE_AWAY("送人"),
    SELL("出售"),
    DISCARD("丢弃");

    private final String displayName;

    DisposePlan(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static DisposePlan fromDisplayName(String displayName) {
        for (DisposePlan plan : values()) {
            if (plan.displayName.equals(displayName)) {
                return plan;
            }
        }
        throw new IllegalArgumentException("Unknown dispose plan: " + displayName);
    }

    public static DisposePlan fromNameOrDisplayName(String value) {
        for (DisposePlan plan : values()) {
            if (plan.name().equalsIgnoreCase(value) || plan.displayName.equals(value)) {
                return plan;
            }
        }
        throw new IllegalArgumentException("Unknown dispose plan: " + value);
    }
}
