package com.scenic.service;

import com.alibaba.fastjson.JSON;
import com.scenic.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class WebSocketService {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private DataStoreService dataStoreService;

    @Autowired
    private SuggestionService suggestionService;

    public void broadcastUpdate() {
        try {
            Map<String, Object> data = new HashMap<>();
            
            List<Area> areas = new ArrayList<>(dataStoreService.getAllAreas());
            List<ScenicSpot> spots = new ArrayList<>(dataStoreService.getAllSpots());
            
            data.put("timestamp", System.currentTimeMillis());
            data.put("areas", areas);
            data.put("spots", spots);
            data.put("statistics", generateStatistics());
            data.put("suggestions", suggestionService.getActiveSuggestions());
            data.put("guidanceMessages", dataStoreService.getActiveGuidanceMessages());

            messagingTemplate.convertAndSend("/topic/visitor-data", JSON.toJSONString(data));
            logger.debug("WebSocket广播数据成功");
        } catch (Exception e) {
            logger.error("WebSocket广播数据失败", e);
        }
    }

    private VisitorStatistics generateStatistics() {
        VisitorStatistics stats = new VisitorStatistics();
        stats.setTimestamp(LocalDateTime.now());

        int totalVisitors = 0;
        int totalCapacity = 0;
        int busyCount = 0;
        int emptyCount = 0;
        int moderateCount = 0;

        Map<Long, VisitorStatistics.AreaStats> areaStatsMap = new LinkedHashMap<>();

        for (Area area : dataStoreService.getAllAreas()) {
            totalVisitors += area.getCurrentVisitors();
            totalCapacity += area.getMaxCapacity();

            VisitorStatistics.AreaStats areaStats = new VisitorStatistics.AreaStats(
                    area.getId(),
                    area.getName(),
                    area.getCurrentVisitors(),
                    area.getMaxCapacity(),
                    area.getCrowdRatio(),
                    area.getCrowdLevel()
            );
            areaStatsMap.put(area.getId(), areaStats);

            CrowdLevel level = area.getCrowdLevel();
            if (level == CrowdLevel.BUSY || level == CrowdLevel.OVERCROWDED) {
                busyCount++;
            } else if (level == CrowdLevel.EMPTY || level == CrowdLevel.COMFORTABLE) {
                emptyCount++;
            } else {
                moderateCount++;
            }
        }

        stats.setTotalVisitors(totalVisitors);
        stats.setMaxCapacity(totalCapacity);
        stats.setOverallCrowdRatio(totalCapacity > 0 ? (double) totalVisitors / totalCapacity : 0);
        stats.setOverallCrowdLevel(CrowdLevel.fromRatio(stats.getOverallCrowdRatio()));
        stats.setAreaStats(areaStatsMap);
        stats.setBusyAreasCount(busyCount);
        stats.setEmptyAreasCount(emptyCount);
        stats.setModerateAreasCount(moderateCount);

        return stats;
    }

    public void broadcastGuidanceMessage(GuidanceMessage message) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "NEW_GUIDANCE");
        data.put("message", message);
        data.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/guidance", JSON.toJSONString(data));
    }

    public void broadcastSuggestion(CrowdSuggestion suggestion) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "NEW_SUGGESTION");
        data.put("suggestion", suggestion);
        data.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/suggestions", JSON.toJSONString(data));
    }
}