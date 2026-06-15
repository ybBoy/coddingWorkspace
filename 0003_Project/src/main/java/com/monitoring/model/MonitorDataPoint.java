package com.monitoring.model;

import java.time.LocalDateTime;

public class MonitorDataPoint {

    private String serverId;
    private String monitorType;
    private double value;
    private LocalDateTime timestamp;

    public MonitorDataPoint() {
    }

    public MonitorDataPoint(String serverId, String monitorType, double value, LocalDateTime timestamp) {
        this.serverId = serverId;
        this.monitorType = monitorType;
        this.value = value;
        this.timestamp = timestamp;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getMonitorType() {
        return monitorType;
    }

    public void setMonitorType(String monitorType) {
        this.monitorType = monitorType;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
