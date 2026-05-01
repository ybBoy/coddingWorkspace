package com.monitoring.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServerConfig {

    private String id;
    private String name;
    private String ipAddress;
    private String description;
    private List<MonitorItem> monitorItems;
    private boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public ServerConfig() {
        this.monitorItems = new ArrayList<>();
        this.enabled = true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<MonitorItem> getMonitorItems() {
        return monitorItems;
    }

    public void setMonitorItems(List<MonitorItem> monitorItems) {
        this.monitorItems = monitorItems;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
