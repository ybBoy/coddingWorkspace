package com.monitoring.model;

import java.time.LocalDateTime;

public class AlertRecord {

    private String id;
    private String serverId;
    private String serverName;
    private String ipAddress;
    private String monitorType;
    private String monitorName;
    private double value;
    private double threshold;
    private String unit;
    private String message;
    private LocalDateTime alertTime;
    private boolean acknowledged;
    private LocalDateTime acknowledgeTime;
    private String acknowledgeUser;

    public AlertRecord() {
        this.acknowledged = false;
    }

    public static AlertRecord create(ServerStatus.MonitorStatus status, ServerConfig config) {
        AlertRecord record = new AlertRecord();
        record.setServerId(config.getId());
        record.setServerName(config.getName());
        record.setIpAddress(config.getIpAddress());
        record.setMonitorType(status.getType());
        record.setMonitorName(status.getName());
        record.setValue(status.getValue());
        record.setThreshold(status.getThreshold());
        record.setUnit(status.getUnit());
        record.setMessage(status.getMessage());
        record.setAlertTime(LocalDateTime.now());
        return record;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getMonitorType() {
        return monitorType;
    }

    public void setMonitorType(String monitorType) {
        this.monitorType = monitorType;
    }

    public String getMonitorName() {
        return monitorName;
    }

    public void setMonitorName(String monitorName) {
        this.monitorName = monitorName;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getAlertTime() {
        return alertTime;
    }

    public void setAlertTime(LocalDateTime alertTime) {
        this.alertTime = alertTime;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public LocalDateTime getAcknowledgeTime() {
        return acknowledgeTime;
    }

    public void setAcknowledgeTime(LocalDateTime acknowledgeTime) {
        this.acknowledgeTime = acknowledgeTime;
    }

    public String getAcknowledgeUser() {
        return acknowledgeUser;
    }

    public void setAcknowledgeUser(String acknowledgeUser) {
        this.acknowledgeUser = acknowledgeUser;
    }
}
