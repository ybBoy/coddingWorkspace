package com.scenic.entity;

import java.time.LocalDateTime;
import java.util.Map;

public class VisitorStatistics {
    private LocalDateTime timestamp;
    private int totalVisitors;
    private int maxCapacity;
    private double overallCrowdRatio;
    private CrowdLevel overallCrowdLevel;
    private Map<Long, AreaStats> areaStats;
    private int busyAreasCount;
    private int emptyAreasCount;
    private int moderateAreasCount;

    public static class AreaStats {
        private Long areaId;
        private String areaName;
        private int currentVisitors;
        private int maxCapacity;
        private double crowdRatio;
        private CrowdLevel crowdLevel;

        public AreaStats() {
        }

        public AreaStats(Long areaId, String areaName, int currentVisitors, int maxCapacity,
                         double crowdRatio, CrowdLevel crowdLevel) {
            this.areaId = areaId;
            this.areaName = areaName;
            this.currentVisitors = currentVisitors;
            this.maxCapacity = maxCapacity;
            this.crowdRatio = crowdRatio;
            this.crowdLevel = crowdLevel;
        }

        public Long getAreaId() {
            return areaId;
        }

        public void setAreaId(Long areaId) {
            this.areaId = areaId;
        }

        public String getAreaName() {
            return areaName;
        }

        public void setAreaName(String areaName) {
            this.areaName = areaName;
        }

        public int getCurrentVisitors() {
            return currentVisitors;
        }

        public void setCurrentVisitors(int currentVisitors) {
            this.currentVisitors = currentVisitors;
        }

        public int getMaxCapacity() {
            return maxCapacity;
        }

        public void setMaxCapacity(int maxCapacity) {
            this.maxCapacity = maxCapacity;
        }

        public double getCrowdRatio() {
            return crowdRatio;
        }

        public void setCrowdRatio(double crowdRatio) {
            this.crowdRatio = crowdRatio;
        }

        public CrowdLevel getCrowdLevel() {
            return crowdLevel;
        }

        public void setCrowdLevel(CrowdLevel crowdLevel) {
            this.crowdLevel = crowdLevel;
        }
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getTotalVisitors() {
        return totalVisitors;
    }

    public void setTotalVisitors(int totalVisitors) {
        this.totalVisitors = totalVisitors;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public double getOverallCrowdRatio() {
        return overallCrowdRatio;
    }

    public void setOverallCrowdRatio(double overallCrowdRatio) {
        this.overallCrowdRatio = overallCrowdRatio;
    }

    public CrowdLevel getOverallCrowdLevel() {
        return overallCrowdLevel;
    }

    public void setOverallCrowdLevel(CrowdLevel overallCrowdLevel) {
        this.overallCrowdLevel = overallCrowdLevel;
    }

    public Map<Long, AreaStats> getAreaStats() {
        return areaStats;
    }

    public void setAreaStats(Map<Long, AreaStats> areaStats) {
        this.areaStats = areaStats;
    }

    public int getBusyAreasCount() {
        return busyAreasCount;
    }

    public void setBusyAreasCount(int busyAreasCount) {
        this.busyAreasCount = busyAreasCount;
    }

    public int getEmptyAreasCount() {
        return emptyAreasCount;
    }

    public void setEmptyAreasCount(int emptyAreasCount) {
        this.emptyAreasCount = emptyAreasCount;
    }

    public int getModerateAreasCount() {
        return moderateAreasCount;
    }

    public void setModerateAreasCount(int moderateAreasCount) {
        this.moderateAreasCount = moderateAreasCount;
    }
}