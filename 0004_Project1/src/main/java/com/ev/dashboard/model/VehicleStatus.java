package com.ev.dashboard.model;

public class VehicleStatus {
    private double speed;
    private double batteryLevel;
    private double range;
    private boolean acOn;
    private double baseRange;
    private boolean charging;
    private double interiorTemp;
    private double targetTemp;
    private String tempControlMode;
    private double exteriorTemp;

    public VehicleStatus() {
        this.speed = 0.0;
        this.batteryLevel = 100.0;
        this.acOn = false;
        this.charging = false;
        this.interiorTemp = 22.0;
        this.targetTemp = 22.0;
        this.tempControlMode = "auto";
        this.exteriorTemp = 25.0;
        calculateRange();
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        if (this.charging) {
            return;
        }
        this.speed = Math.max(0, Math.min(180, speed));
    }

    public double getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(double batteryLevel) {
        this.batteryLevel = Math.max(0, Math.min(100, batteryLevel));
        calculateRange();
    }

    public double getRange() {
        return range;
    }

    public boolean isAcOn() {
        return acOn;
    }

    public void setAcOn(boolean acOn) {
        this.acOn = acOn;
        calculateRange();
    }

    public boolean isCharging() {
        return charging;
    }

    public void setCharging(boolean charging) {
        if (charging && this.speed > 0) {
            return;
        }
        this.charging = charging;
    }

    public double getInteriorTemp() {
        return interiorTemp;
    }

    public void setInteriorTemp(double interiorTemp) {
        this.interiorTemp = Math.max(-10, Math.min(50, interiorTemp));
    }

    public double getTargetTemp() {
        return targetTemp;
    }

    public void setTargetTemp(double targetTemp) {
        this.targetTemp = Math.max(16, Math.min(30, targetTemp));
    }

    public String getTempControlMode() {
        return tempControlMode;
    }

    public void setTempControlMode(String tempControlMode) {
        if ("auto".equals(tempControlMode) || "manual".equals(tempControlMode)) {
            this.tempControlMode = tempControlMode;
        }
    }

    public double getExteriorTemp() {
        return exteriorTemp;
    }

    public void setExteriorTemp(double exteriorTemp) {
        this.exteriorTemp = Math.max(-20, Math.min(45, exteriorTemp));
    }

    private void calculateRange() {
        this.baseRange = this.batteryLevel * 0.85;
        if (this.acOn) {
            this.range = Math.max(0, this.baseRange - 10);
        } else {
            this.range = this.baseRange;
        }
    }
}
