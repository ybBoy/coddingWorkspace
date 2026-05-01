package com.scenic.service;

import com.scenic.entity.Area;
import com.scenic.entity.CrowdLevel;
import com.scenic.entity.CrowdSuggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class SuggestionService {
    private static final Logger logger = LoggerFactory.getLogger(SuggestionService.class);

    @Autowired
    private DataStoreService dataStoreService;

    @Autowired
    private WebSocketService webSocketService;

    private final Map<Long, Long> lastSuggestionTime = new HashMap<>();
    private static final long SUGGESTION_COOLDOWN = 60000;

    @Scheduled(fixedRate = 5000)
    public void checkAndGenerateSuggestions() {
        Collection<Area> areas = dataStoreService.getAllAreas();
        Set<Long> activeAreas = new HashSet<>();

        for (Area area : areas) {
            activeAreas.add(area.getId());
            
            if (shouldGenerateSuggestion(area)) {
                generateSuggestionForArea(area, areas);
            }
        }

        cleanupInactiveSuggestions(activeAreas);
    }

    private boolean shouldGenerateSuggestion(Area area) {
        CrowdLevel level = area.getCrowdLevel();
        
        if (level == CrowdLevel.BUSY || level == CrowdLevel.OVERCROWDED) {
            Long lastTime = lastSuggestionTime.get(area.getId());
            if (lastTime == null || System.currentTimeMillis() - lastTime > SUGGESTION_COOLDOWN) {
                return true;
            }
        }
        return false;
    }

    private void generateSuggestionForArea(Area busyArea, Collection<Area> allAreas) {
        List<Area> adjacentEmptyAreas = new ArrayList<>();
        
        for (Long adjacentId : busyArea.getAdjacentAreaIds()) {
            Area adjacent = findAreaById(allAreas, adjacentId);
            if (adjacent != null) {
                CrowdLevel level = adjacent.getCrowdLevel();
                if (level == CrowdLevel.EMPTY || level == CrowdLevel.COMFORTABLE) {
                    adjacentEmptyAreas.add(adjacent);
                }
            }
        }

        if (adjacentEmptyAreas.isEmpty()) {
            for (Area area : allAreas) {
                if (!area.getId().equals(busyArea.getId())) {
                    CrowdLevel level = area.getCrowdLevel();
                    if (level == CrowdLevel.EMPTY || level == CrowdLevel.COMFORTABLE) {
                        adjacentEmptyAreas.add(area);
                    }
                }
            }
        }

        if (!adjacentEmptyAreas.isEmpty()) {
            Collections.sort(adjacentEmptyAreas, new Comparator<Area>() {
                @Override
                public int compare(Area a1, Area a2) {
                    return Double.compare(a1.getCrowdRatio(), a2.getCrowdRatio());
                }
            });

            Area targetArea = adjacentEmptyAreas.get(0);
            createSuggestion(busyArea, targetArea);
            lastSuggestionTime.put(busyArea.getId(), System.currentTimeMillis());
        }
    }

    private void createSuggestion(Area sourceArea, Area targetArea) {
        CrowdSuggestion suggestion = new CrowdSuggestion();
        suggestion.setId(dataStoreService.getNextId());
        suggestion.setType("CROWD_GUIDANCE");
        suggestion.setSourceAreaId(sourceArea.getId());
        suggestion.setSourceAreaName(sourceArea.getName());
        suggestion.setTargetAreaId(targetArea.getId());
        suggestion.setTargetAreaName(targetArea.getName());

        String message = String.format("%s当前游客%d人，已接近容量上限（容量%d人），拥挤程度：%s",
                sourceArea.getName(), sourceArea.getCurrentVisitors(),
                sourceArea.getMaxCapacity(), sourceArea.getCrowdLevel().getDescription());

        String suggestionText = String.format("建议引导游客前往%s区域（当前仅%d人，容量%d人，拥挤程度：%s）",
                targetArea.getName(), targetArea.getCurrentVisitors(),
                targetArea.getMaxCapacity(), targetArea.getCrowdLevel().getDescription());

        suggestion.setMessage(message);
        suggestion.setSuggestion(suggestionText);
        
        int priority;
        if (sourceArea.getCrowdLevel() == CrowdLevel.OVERCROWDED) {
            priority = 1;
        } else if (sourceArea.getCrowdLevel() == CrowdLevel.BUSY) {
            priority = 2;
        } else {
            priority = 3;
        }
        suggestion.setPriority(priority);

        dataStoreService.addCrowdSuggestion(suggestion);
        webSocketService.broadcastSuggestion(suggestion);
        
        logger.info("生成拥挤建议: {} -> {}", sourceArea.getName(), targetArea.getName());
    }

    private Area findAreaById(Collection<Area> areas, Long id) {
        for (Area area : areas) {
            if (area.getId().equals(id)) {
                return area;
            }
        }
        return null;
    }

    private void cleanupInactiveSuggestions(Set<Long> activeAreaIds) {
        List<CrowdSuggestion> allSuggestions = dataStoreService.getAllCrowdSuggestions();
        for (CrowdSuggestion suggestion : allSuggestions) {
            if (suggestion.isActive()) {
                Area area = dataStoreService.getAreaById(suggestion.getSourceAreaId());
                if (area != null) {
                    CrowdLevel level = area.getCrowdLevel();
                    if (level != CrowdLevel.BUSY && level != CrowdLevel.OVERCROWDED) {
                        suggestion.setActive(false);
                        logger.info("停用建议: {}", suggestion.getMessage());
                    }
                }
            }
        }
        dataStoreService.clearOldSuggestions();
    }

    public List<CrowdSuggestion> getActiveSuggestions() {
        return dataStoreService.getActiveCrowdSuggestions();
    }

    public List<CrowdSuggestion> getAllSuggestions() {
        return dataStoreService.getAllCrowdSuggestions();
    }
}