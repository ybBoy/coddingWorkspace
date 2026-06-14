package com.demo.dto;

import java.util.List;

public class StatisticsResponse {
    private int totalStockGrams;
    private int totalBeanKinds;
    private int approachingCount;
    private int lowStockCount;
    private int emptyCount;
    private List<?> lowStockBeans;
    private List<WeeklyConsumeBean> weeklyTopConsumed;

    public int getTotalStockGrams() {
        return totalStockGrams;
    }

    public void setTotalStockGrams(int totalStockGrams) {
        this.totalStockGrams = totalStockGrams;
    }

    public int getTotalBeanKinds() {
        return totalBeanKinds;
    }

    public void setTotalBeanKinds(int totalBeanKinds) {
        this.totalBeanKinds = totalBeanKinds;
    }

    public int getApproachingCount() {
        return approachingCount;
    }

    public void setApproachingCount(int approachingCount) {
        this.approachingCount = approachingCount;
    }

    public int getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(int lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public int getEmptyCount() {
        return emptyCount;
    }

    public void setEmptyCount(int emptyCount) {
        this.emptyCount = emptyCount;
    }

    public List<?> getLowStockBeans() {
        return lowStockBeans;
    }

    public void setLowStockBeans(List<?> lowStockBeans) {
        this.lowStockBeans = lowStockBeans;
    }

    public List<WeeklyConsumeBean> getWeeklyTopConsumed() {
        return weeklyTopConsumed;
    }

    public void setWeeklyTopConsumed(List<WeeklyConsumeBean> weeklyTopConsumed) {
        this.weeklyTopConsumed = weeklyTopConsumed;
    }
}
