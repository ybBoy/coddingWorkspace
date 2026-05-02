package com.ev.dashboard.service;

import com.ev.dashboard.model.VehicleStatus;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class VehicleService {
    private final VehicleStatus vehicleStatus;
    private final Random random;

    public VehicleService() {
        this.vehicleStatus = new VehicleStatus();
        this.random = new Random();
    }

    public VehicleStatus getStatus() {
        return vehicleStatus;
    }

    public void accelerate(double amount) {
        double currentSpeed = vehicleStatus.getSpeed();
        vehicleStatus.setSpeed(currentSpeed + amount);
    }

    public void brake(double amount) {
        double currentSpeed = vehicleStatus.getSpeed();
        vehicleStatus.setSpeed(currentSpeed - amount);
    }

    public void toggleAc() {
        boolean currentAcStatus = vehicleStatus.isAcOn();
        vehicleStatus.setAcOn(!currentAcStatus);
    }

    public void simulateBatteryChange() {
        double currentBattery = vehicleStatus.getBatteryLevel();
        if (currentBattery <= 0) {
            return;
        }

        double change = random.nextDouble() * 0.5 - 0.25;
        if (vehicleStatus.getSpeed() > 0) {
            change -= random.nextDouble() * 0.3;
        }

        if (vehicleStatus.isAcOn()) {
            change -= random.nextDouble() * 0.1;
        }

        double newBattery = currentBattery + change;
        vehicleStatus.setBatteryLevel(newBattery);
    }
}
