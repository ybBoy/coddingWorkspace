package com.company.ot.model;

public enum TimeoffType {
    HALF_DAY("半天", 4),
    FULL_DAY("全天", 8);

    private final String label;
    private final int hours;

    TimeoffType(String label, int hours) {
        this.label = label;
        this.hours = hours;
    }

    public String getLabel() {
        return label;
    }

    public int getHours() {
        return hours;
    }
}
