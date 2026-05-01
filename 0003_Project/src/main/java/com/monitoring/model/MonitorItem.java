package com.monitoring.model;

public class MonitorItem {

    private String type;
    private String name;
    private double warningThreshold;
    private double criticalThreshold;
    private String unit;
    private boolean enabled;

    public static final String CPU = "CPU";
    public static final String MEMORY = "MEMORY";
    public static final String DISK = "DISK";

    public MonitorItem() {
    }

    public MonitorItem(String type, String name, double warningThreshold, double criticalThreshold, String unit) {
        this.type = type;
        this.name = name;
        this.warningThreshold = warningThreshold;
        this.criticalThreshold = criticalThreshold;
        this.unit = unit;
        this.enabled = true;
    }

    public static MonitorItem createCpuItem() {
        return new MonitorItem(CPU, "CPU使用率", 70.0, 85.0, "%");
    }

    public static MonitorItem createMemoryItem() {
        return new MonitorItem(MEMORY, "内存使用率", 75.0, 90.0, "%");
    }

    public static MonitorItem createDiskItem() {
        return new MonitorItem(DISK, "硬盘剩余空间", 20.0, 10.0, "%");
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

    public double getWarningThreshold() {
        return warningThreshold;
    }

    public void setWarningThreshold(double warningThreshold) {
        this.warningThreshold = warningThreshold;
    }

    public double getCriticalThreshold() {
        return criticalThreshold;
    }

    public void setCriticalThreshold(double criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
    }

    @Deprecated
    public double getThreshold() {
        return criticalThreshold;
    }

    @Deprecated
    public void setThreshold(double threshold) {
        this.criticalThreshold = threshold;
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
