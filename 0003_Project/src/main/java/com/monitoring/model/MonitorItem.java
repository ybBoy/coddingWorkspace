package com.monitoring.model;

public class MonitorItem {

    private String type;
    private String name;
    private double threshold;
    private String unit;
    private boolean enabled;

    public static final String CPU = "CPU";
    public static final String MEMORY = "MEMORY";
    public static final String DISK = "DISK";

    public MonitorItem() {
    }

    public MonitorItem(String type, String name, double threshold, String unit) {
        this.type = type;
        this.name = name;
        this.threshold = threshold;
        this.unit = unit;
        this.enabled = true;
    }

    public static MonitorItem createCpuItem() {
        return new MonitorItem(CPU, "CPU使用率", 80.0, "%");
    }

    public static MonitorItem createMemoryItem() {
        return new MonitorItem(MEMORY, "内存使用率", 85.0, "%");
    }

    public static MonitorItem createDiskItem() {
        return new MonitorItem(DISK, "硬盘剩余空间", 10.0, "%");
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
