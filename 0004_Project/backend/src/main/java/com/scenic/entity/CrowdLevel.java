package com.scenic.entity;

public enum CrowdLevel {
    EMPTY(0, "空闲", "#4CAF50"),
    COMFORTABLE(1, "舒适", "#8BC34A"),
    MODERATE(2, "适中", "#FFC107"),
    BUSY(3, "拥挤", "#FF9800"),
    OVERCROWDED(4, "超负荷", "#F44336");

    private final int code;
    private final String description;
    private final String color;

    CrowdLevel(int code, String description, String color) {
        this.code = code;
        this.description = description;
        this.color = color;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getColor() {
        return color;
    }

    public static CrowdLevel fromRatio(double ratio) {
        if (ratio < 0.3) return EMPTY;
        if (ratio < 0.5) return COMFORTABLE;
        if (ratio < 0.7) return MODERATE;
        if (ratio < 0.9) return BUSY;
        return OVERCROWDED;
    }
}