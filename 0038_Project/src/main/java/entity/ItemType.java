package entity;

public enum ItemType {
    APPLIANCE("电器"),
    FURNITURE("家具"),
    PLUMBING("水电"),
    OTHER("其他");

    private String displayName;

    ItemType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ItemType fromDisplayName(String displayName) {
        for (ItemType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        return OTHER;
    }
}
