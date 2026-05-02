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
        if (vehicleStatus.isCharging()) {
            return;
        }
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

    public void startCharging() {
        if (vehicleStatus.getSpeed() > 0) {
            return;
        }
        vehicleStatus.setCharging(true);
    }

    public void stopCharging() {
        vehicleStatus.setCharging(false);
    }

    public void toggleCharging() {
        if (vehicleStatus.isCharging()) {
            stopCharging();
        } else {
            startCharging();
        }
    }

    public void setTargetTemp(double targetTemp) {
        vehicleStatus.setTargetTemp(targetTemp);
    }

    public void setTempControlMode(String mode) {
        vehicleStatus.setTempControlMode(mode);
    }

    public void adjustInteriorTemp(double delta) {
        if ("manual".equals(vehicleStatus.getTempControlMode())) {
            double currentTemp = vehicleStatus.getInteriorTemp();
            vehicleStatus.setInteriorTemp(currentTemp + delta);
        }
    }

    public void simulateBatteryChange() {
        double currentBattery = vehicleStatus.getBatteryLevel();
        
        if (vehicleStatus.isCharging()) {
            if (currentBattery >= 100) {
                vehicleStatus.setCharging(false);
                return;
            }
            double chargeRate = 0.5 + random.nextDouble() * 0.5;
            if (vehicleStatus.isAcOn()) {
                chargeRate -= random.nextDouble() * 0.2;
            }
            double newBattery = currentBattery + Math.max(0.1, chargeRate);
            vehicleStatus.setBatteryLevel(newBattery);
            if (vehicleStatus.getBatteryLevel() >= 100) {
                vehicleStatus.setBatteryLevel(100);
                vehicleStatus.setCharging(false);
            }
            return;
        }

        if (currentBattery <= 0) {
            return;
        }

        double dischargeRate = 0.05 + random.nextDouble() * 0.1;

        if (vehicleStatus.getSpeed() > 0) {
            dischargeRate += 0.1 + random.nextDouble() * 0.2;
        }

        if (vehicleStatus.isAcOn()) {
            dischargeRate += 0.05 + random.nextDouble() * 0.1;
        }

        double newBattery = currentBattery - dischargeRate;
        vehicleStatus.setBatteryLevel(newBattery);
    }

    public void simulateTemperatureChange() {
        String mode = vehicleStatus.getTempControlMode();
        double interiorTemp = vehicleStatus.getInteriorTemp();
        double targetTemp = vehicleStatus.getTargetTemp();
        double exteriorTemp = vehicleStatus.getExteriorTemp();

        double ambientEffect = (exteriorTemp - interiorTemp) * 0.01;

        if ("auto".equals(mode) && vehicleStatus.isAcOn()) {
            double tempDiff = targetTemp - interiorTemp;
            double adjustRate = 0.15 + random.nextDouble() * 0.1;
            
            if (Math.abs(tempDiff) > 0.1) {
                interiorTemp += tempDiff * adjustRate + ambientEffect;
            } else {
                interiorTemp += ambientEffect;
            }
        } else {
            interiorTemp += ambientEffect;
        }

        vehicleStatus.setInteriorTemp(interiorTemp);

        double exteriorChange = (random.nextDouble() - 0.5) * 0.2;
        vehicleStatus.setExteriorTemp(exteriorTemp + exteriorChange);
    }
}
