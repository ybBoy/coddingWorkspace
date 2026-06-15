package com.monitoring.controller;

import com.monitoring.model.MonitorDataPoint;
import com.monitoring.service.HistoryDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "*")
public class HistoryDataController {

    @Autowired
    private HistoryDataService historyDataService;

    @GetMapping("/server/{serverId}/hour")
    public Map<String, List<MonitorDataPoint>> getLastHourData(@PathVariable String serverId) {
        return historyDataService.getLastHourDataForServer(serverId);
    }

    @GetMapping("/server/{serverId}/day")
    public Map<String, List<MonitorDataPoint>> getLastDayData(@PathVariable String serverId) {
        return historyDataService.getLastDayDataForServer(serverId);
    }

    @GetMapping("/server/{serverId}/monitor/{monitorType}/hour")
    public List<MonitorDataPoint> getLastHourDataForMonitor(
            @PathVariable String serverId,
            @PathVariable String monitorType) {
        return historyDataService.getLastHourData(serverId, monitorType);
    }

    @GetMapping("/server/{serverId}/monitor/{monitorType}/day")
    public List<MonitorDataPoint> getLastDayDataForMonitor(
            @PathVariable String serverId,
            @PathVariable String monitorType) {
        return historyDataService.getLastDayData(serverId, monitorType);
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDataPoints", historyDataService.getDataPointCount());
        stats.put("monitoredKeys", historyDataService.getMonitoredKeys());
        return stats;
    }
}
