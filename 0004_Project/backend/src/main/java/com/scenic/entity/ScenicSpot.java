package com.scenic.entity;

import java.time.LocalDateTime;

public class ScenicSpot {
    private Long id;
    private String name;
    private String description;
    private Long areaId;
    private double x;
    private double y;
    private int maxCapacity;
    private int currentVisitors;
    private CrowdLevel crowdLevel;
    private LocalDateTime lastUpdate;

    public ScenicSpot() {
    }

    public ScenicSpot(Long id, String name, String description, Long areaId, double x, double y, int maxCapacity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.areaId = areaId;
        this.x = x;
        this.y = y;
        this.maxCapacity = maxCapacity;
        this.currentVisitors = 0;
        this.crowdLevel = CrowdLevel.EMPTY;
        this.lastUpdate = LocalDateTime.now();
    }

    public void updateCrowdLevel() {
        double ratio = (double) currentVisitors / maxCapacity;
        this.crowdLevel = CrowdLevel.fromRatio(ratio);
        this.lastUpdate = LocalDateTime.now();
    }

    public double getCrowdRatio() {
        return (double) currentVisitors / maxCapacity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public int getCurrentVisitors() {
        return currentVisitors;
    }

    public void setCurrentVisitors(int currentVisitors) {
        this.currentVisitors = currentVisitors;
        updateCrowdLevel();
    }

    public CrowdLevel getCrowdLevel() {
        return crowdLevel;
    }

    public void setCrowdLevel(CrowdLevel crowdLevel) {
        this.crowdLevel = crowdLevel;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}