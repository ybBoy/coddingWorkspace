package com.demo.dto;

public class WeeklyConsumeBean {
    private String beanId;
    private String beanName;
    private int totalConsumed;

    public WeeklyConsumeBean() {
    }

    public WeeklyConsumeBean(String beanId, String beanName, int totalConsumed) {
        this.beanId = beanId;
        this.beanName = beanName;
        this.totalConsumed = totalConsumed;
    }

    public String getBeanId() {
        return beanId;
    }

    public void setBeanId(String beanId) {
        this.beanId = beanId;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public int getTotalConsumed() {
        return totalConsumed;
    }

    public void setTotalConsumed(int totalConsumed) {
        this.totalConsumed = totalConsumed;
    }
}
