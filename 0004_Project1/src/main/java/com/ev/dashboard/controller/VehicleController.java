package com.ev.dashboard.controller;

import com.ev.dashboard.model.VehicleStatus;
import com.ev.dashboard.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @GetMapping("/status")
    public VehicleStatus getStatus() {
        return vehicleService.getStatus();
    }

    @PostMapping("/accelerate")
    public VehicleStatus accelerate(@RequestParam(defaultValue = "10") double amount) {
        vehicleService.accelerate(amount);
        return vehicleService.getStatus();
    }

    @PostMapping("/brake")
    public VehicleStatus brake(@RequestParam(defaultValue = "10") double amount) {
        vehicleService.brake(amount);
        return vehicleService.getStatus();
    }

    @PostMapping("/toggle-ac")
    public VehicleStatus toggleAc() {
        vehicleService.toggleAc();
        return vehicleService.getStatus();
    }

    @PostMapping("/toggle-charging")
    public Map<String, Object> toggleCharging() {
        Map<String, Object> result = new HashMap<>();
        VehicleStatus currentStatus = vehicleService.getStatus();
        
        if (currentStatus.isCharging()) {
            vehicleService.stopCharging();
            result.put("message", "已停止充电");
            result.put("charging", false);
        } else {
            if (currentStatus.getSpeed() > 0) {
                result.put("message", "车辆行驶中，无法开始充电");
                result.put("charging", false);
                result.put("error", true);
            } else {
                vehicleService.startCharging();
                result.put("message", "已开始充电");
                result.put("charging", true);
            }
        }
        result.put("status", vehicleService.getStatus());
        return result;
    }

    @PostMapping("/set-target-temp")
    public VehicleStatus setTargetTemp(@RequestParam double temp) {
        vehicleService.setTargetTemp(temp);
        return vehicleService.getStatus();
    }

    @PostMapping("/set-temp-mode")
    public VehicleStatus setTempMode(@RequestParam String mode) {
        vehicleService.setTempControlMode(mode);
        return vehicleService.getStatus();
    }

    @PostMapping("/adjust-temp-up")
    public VehicleStatus adjustTempUp() {
        vehicleService.adjustInteriorTemp(1.0);
        return vehicleService.getStatus();
    }

    @PostMapping("/adjust-temp-down")
    public VehicleStatus adjustTempDown() {
        vehicleService.adjustInteriorTemp(-1.0);
        return vehicleService.getStatus();
    }
}
