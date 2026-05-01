package com.monitoring.controller;

import com.monitoring.model.AlertRecord;
import com.monitoring.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertController {

    @Autowired
    private AlertService alertService;

    @GetMapping
    public List<AlertRecord> getAllAlerts() {
        return alertService.getAllAlerts();
    }

    @GetMapping("/active")
    public List<AlertRecord> getActiveAlerts() {
        return alertService.getActiveAlerts();
    }

    @GetMapping("/history")
    public List<AlertRecord> getHistoricalAlerts() {
        return alertService.getHistoricalAlerts();
    }

    @PostMapping("/{id}/acknowledge")
    public String acknowledgeAlert(
            @PathVariable String id,
            @RequestParam(required = false) String user) {
        boolean acknowledged = alertService.acknowledgeAlert(id, user);
        if (acknowledged) {
            return "告警已确认";
        }
        return "告警不存在或已确认";
    }

    @PostMapping("/acknowledge-all")
    public String acknowledgeAllAlerts(
            @RequestParam(required = false) String user) {
        alertService.acknowledgeAllAlerts(user);
        return "所有活跃告警已确认";
    }
}
