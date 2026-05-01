package com.scenic.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Area {
    private Long id;
    private String name;
    private String description;
    private double x;
    private double y;
    private int maxCapacity;
    private int currentVisitors;
    private CrowdLevel crowdLevel;
    private LocalDateTime lastUpdate;
    private List<ScenicSpot> spots;
    private List<Long> adjacentAreaIds;

    public Area() {
        this.spots = new ArrayList<>();
        this.adjacentAreaIds = new ArrayList<>();
    }

    public Area(Long id, String name, String description, double x, double y, int maxCapacity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.x = x;
        this.y = y;
        this.maxCapacity = maxCapacity;
        this.currentVisitors = 0;
        this.crowdLevel = CrowdLevel.EMPTY;
        this.lastUpdate = LocalDateTime.now();
        this.spots = new ArrayList<>();
        this.adjacentAreaIds = new ArrayList<>();
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

    public List<ScenicSpot> getSpots() {
        return spots;
    }

    public void setSpots(List<ScenicSpot> spots) {
        this.spots = spots;
    }

    public List<Long> getAdjacentAreaIds() {
        return adjacentAreaIds;
    }

    public void setAdjacentAreaIds(List<Long> adjacentAreaIds) {
        this.adjacentAreaIds = adjacentAreaIds;
    }
}