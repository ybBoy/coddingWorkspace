package com.monitoring.controller;

import com.monitoring.model.ServerStatus;
import com.monitoring.service.MonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
@CrossOrigin(origins = "*")
public class MonitoringController {

    @Autowired
    private MonitoringService monitoringService;

    @GetMapping("/status")
    public Map<String, ServerStatus> getAllStatuses() {
        return monitoringService.getAllServerStatuses();
    }

    @GetMapping("/status/{serverId}")
    public ServerStatus getServerStatus(@PathVariable String serverId) {
        return monitoringService.getServerStatus(serverId);
    }

    @PostMapping("/check-now")
    public String executeManualCheck() {
        monitoringService.executeMonitoring();
        return "监控检查已执行";
    }
}
