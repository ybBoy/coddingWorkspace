package domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CareType {
    WATERING("浇水", "💧"),
    FERTILIZING("施肥", "🌱"),
    PRUNING("修剪", "✂️");

    private final String label;
    private final String icon;

    CareType(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    @JsonValue
    public String getValue() {
        return super.name();
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    @JsonCreator
    public static CareType fromName(String name) {
        for (CareType type : values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return WATERING;
    }
}
