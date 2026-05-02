package com.ev.dashboard.model;

public class VehicleStatus {
    private double speed;
    private double batteryLevel;
    private double range;
    private boolean acOn;
    private double baseRange;

    public VehicleStatus() {
        this.speed = 0.0;
        this.batteryLevel = 100.0;
        this.acOn = false;
        calculateRange();
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
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

    private void calculateRange() {
        this.baseRange = this.batteryLevel * 0.85;
        if (this.acOn) {
            this.range = Math.max(0, this.baseRange - 10);
        } else {
            this.range = this.baseRange;
        }
    }
}
