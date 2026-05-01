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
    private double warningThreshold;
    private double criticalThreshold;
    private String unit;
    private AlertLevel alertLevel;
    private String message;
    private LocalDateTime alertTime;
    private boolean acknowledged;
    private LocalDateTime acknowledgeTime;
    private String acknowledgeUser;

    public AlertRecord() {
        this.acknowledged = false;
        this.alertLevel = AlertLevel.NORMAL;
    }

    public static AlertRecord create(ServerStatus.MonitorStatus status, ServerConfig config) {
        AlertRecord record = new AlertRecord();
        record.setServerId(config.getId());
        record.setServerName(config.getName());
        record.setIpAddress(config.getIpAddress());
        record.setMonitorType(status.getType());
        record.setMonitorName(status.getName());
        record.setValue(status.getValue());
        record.setWarningThreshold(status.getWarningThreshold());
        record.setCriticalThreshold(status.getCriticalThreshold());
        record.setThreshold(status.getCriticalThreshold());
        record.setUnit(status.getUnit());
        record.setAlertLevel(status.getAlertLevel());
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

    public AlertLevel getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(AlertLevel alertLevel) {
        this.alertLevel = alertLevel;
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
