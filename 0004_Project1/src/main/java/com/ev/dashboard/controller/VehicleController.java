package com.ev.dashboard.controller;

import com.ev.dashboard.model.VehicleStatus;
import com.ev.dashboard.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
}
