package domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PlantStatus {
    HEALTHY("健康", "#2d6a4f"),
    GROWING_WELL("生长良好", "#40916c"),
    NEEDS_ATTENTION("需要关注", "#f4a261"),
    SICK("生病", "#e76f51"),
    DORMANT("休眠", "#6c757d");

    private final String label;
    private final String color;

    PlantStatus(String label, String color) {
        this.label = label;
        this.color = color;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }

    @JsonCreator
    public static PlantStatus fromLabel(String label) {
        for (PlantStatus status : values()) {
            if (status.label.equals(label) || status.name().equals(label)) {
                return status;
            }
        }
        return HEALTHY;
    }
}
