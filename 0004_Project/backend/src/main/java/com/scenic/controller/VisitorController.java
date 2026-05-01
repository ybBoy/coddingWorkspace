package com.scenic.controller;

import com.scenic.entity.*;
import com.scenic.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class VisitorController {

    @Autowired
    private DataStoreService dataStoreService;

    @Autowired
    private VisitorSimulatorService simulatorService;

    @Autowired
    private GuidanceService guidanceService;

    @Autowired
    private SuggestionService suggestionService;

    @GetMapping("/areas")
    public ResponseEntity<Collection<Area>> getAllAreas() {
        return ResponseEntity.ok(dataStoreService.getAllAreas());
    }

    @GetMapping("/areas/{id}")
    public ResponseEntity<Area> getAreaById(@PathVariable Long id) {
        Area area = dataStoreService.getAreaById(id);
        if (area == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(area);
    }

    @GetMapping("/spots")
    public ResponseEntity<Collection<ScenicSpot>> getAllSpots() {
        return ResponseEntity.ok(dataStoreService.getAllSpots());
    }

    @GetMapping("/spots/{id}")
    public ResponseEntity<ScenicSpot> getSpotById(@PathVariable Long id) {
        ScenicSpot spot = dataStoreService.getSpotById(id);
        if (spot == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(spot);
    }

    @GetMapping("/areas/{areaId}/spots")
    public ResponseEntity<List<ScenicSpot>> getSpotsByAreaId(@PathVariable Long areaId) {
        return ResponseEntity.ok(dataStoreService.getSpotsByAreaId(areaId));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<CrowdSuggestion>> getSuggestions(
            @RequestParam(required = false, defaultValue = "true") boolean activeOnly) {
        if (activeOnly) {
            return ResponseEntity.ok(suggestionService.getActiveSuggestions());
        }
        return ResponseEntity.ok(suggestionService.getAllSuggestions());
    }

    @GetMapping("/guidance")
    public ResponseEntity<List<GuidanceMessage>> getGuidanceMessages(
            @RequestParam(required = false, defaultValue = "true") boolean activeOnly) {
        if (activeOnly) {
            return ResponseEntity.ok(guidanceService.getActiveGuidanceMessages());
        }
        return ResponseEntity.ok(guidanceService.getAllGuidanceMessages());
    }

    @PostMapping("/guidance")
    public ResponseEntity<GuidanceMessage> publishGuidance(@RequestBody Map<String, Object> request) {
        String title = (String) request.get("title");
        String content = (String) request.get("content");
        String typeStr = (String) request.get("type");
        String targetDisplay = (String) request.getOrDefault("targetDisplay", "入口大屏");
        int durationMinutes = (Integer) request.getOrDefault("durationMinutes", 30);

        GuidanceMessage.MessageType type = GuidanceMessage.MessageType.INFO;
        if (typeStr != null) {
            try {
                type = GuidanceMessage.MessageType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                // use default
            }
        }

        GuidanceMessage message = guidanceService.publishManualGuidance(
                title, content, type, targetDisplay, durationMinutes);
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/guidance/{id}")
    public ResponseEntity<Void> deactivateGuidance(@PathVariable Long id) {
        boolean success = guidanceService.deactivateGuidanceMessage(id);
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/simulator/status")
    public ResponseEntity<Map<String, Object>> getSimulatorStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("running", simulatorService.isRunning());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/simulator/start")
    public ResponseEntity<Map<String, Object>> startSimulator() {
        simulatorService.setRunning(true);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "模拟已启动");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/simulator/stop")
    public ResponseEntity<Map<String, Object>> stopSimulator() {
        simulatorService.setRunning(false);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "模拟已停止");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/simulator/reset")
    public ResponseEntity<Map<String, Object>> resetSimulator() {
        simulatorService.resetAllData();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "数据已重置");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/auto-publish/status")
    public ResponseEntity<Map<String, Object>> getAutoPublishStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", guidanceService.isAutoPublishEnabled());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/auto-publish/enable")
    public ResponseEntity<Map<String, Object>> enableAutoPublish() {
        guidanceService.setAutoPublishEnabled(true);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "自动发布已启用");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/auto-publish/disable")
    public ResponseEntity<Map<String, Object>> disableAutoPublish() {
        guidanceService.setAutoPublishEnabled(false);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "自动发布已禁用");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        Map<String, Object> result = new HashMap<>();
        
        Collection<Area> areas = dataStoreService.getAllAreas();
        int totalVisitors = 0;
        int totalCapacity = 0;
        int busyCount = 0;
        int emptyCount = 0;
        int moderateCount = 0;

        for (Area area : areas) {
            totalVisitors += area.getCurrentVisitors();
            totalCapacity += area.getMaxCapacity();
            
            CrowdLevel level = area.getCrowdLevel();
            if (level == CrowdLevel.BUSY || level == CrowdLevel.OVERCROWDED) {
                busyCount++;
            } else if (level == CrowdLevel.EMPTY || level == CrowdLevel.COMFORTABLE) {
                emptyCount++;
            } else {
                moderateCount++;
            }
        }

        result.put("totalVisitors", totalVisitors);
        result.put("totalCapacity", totalCapacity);
        result.put("overallRatio", totalCapacity > 0 ? (double) totalVisitors / totalCapacity : 0);
        result.put("areaCount", areas.size());
        result.put("busyAreas", busyCount);
        result.put("emptyAreas", emptyCount);
        result.put("moderateAreas", moderateCount);
        result.put("activeSuggestions", suggestionService.getActiveSuggestions().size());
        result.put("activeGuidance", guidanceService.getActiveGuidanceMessages().size());

        return ResponseEntity.ok(result);
    }
}