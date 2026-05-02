package com.ev.dashboard.scheduler;

import com.ev.dashboard.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BatteryScheduler {

    @Autowired
    private VehicleService vehicleService;

    @Scheduled(fixedRate = 3000)
    public void simulateBatteryChange() {
        vehicleService.simulateBatteryChange();
    }
}
