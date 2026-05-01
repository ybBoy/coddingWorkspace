package com.monitoring.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServerStatus {

    private String serverId;
    private String serverName;
    private String ipAddress;
    private boolean online;
    private List<MonitorStatus> monitorStatuses;
    private LocalDateTime checkTime;
    private boolean inMaintenance;
    private LocalDateTime maintenanceEndTime;

    public ServerStatus() {
        this.monitorStatuses = new ArrayList<>();
        this.inMaintenance = false;
    }

    public static class MonitorStatus {

        private String type;
        private String name;
        private double value;
        private double warningThreshold;
        private double criticalThreshold;
        private String unit;
        private boolean alarming;
        private AlertLevel alertLevel;
        private String message;

        public MonitorStatus() {
            this.alertLevel = AlertLevel.NORMAL;
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

        public boolean isAlarming() {
            return alarming;
        }

        public void setAlarming(boolean alarming) {
            this.alarming = alarming;
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
    }

    public boolean hasAlarms() {
        return monitorStatuses != null &&
                monitorStatuses.stream().anyMatch(MonitorStatus::isAlarming);
    }

    public String getStatus() {
        if (inMaintenance) {
            return "MAINTENANCE";
        }
        if (!online) {
            return "OFFLINE";
        }
        if (hasAlarms()) {
            return "ALARMING";
        }
        return "NORMAL";
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

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public List<MonitorStatus> getMonitorStatuses() {
        return monitorStatuses;
    }

    public void setMonitorStatuses(List<MonitorStatus> monitorStatuses) {
        this.monitorStatuses = monitorStatuses;
    }

    public LocalDateTime getCheckTime() {
        return checkTime;
    }

    public void setCheckTime(LocalDateTime checkTime) {
        this.checkTime = checkTime;
    }

    public boolean isInMaintenance() {
        return inMaintenance;
    }

    public void setInMaintenance(boolean inMaintenance) {
        this.inMaintenance = inMaintenance;
    }

    public LocalDateTime getMaintenanceEndTime() {
        return maintenanceEndTime;
    }

    public void setMaintenanceEndTime(LocalDateTime maintenanceEndTime) {
        this.maintenanceEndTime = maintenanceEndTime;
    }
}
