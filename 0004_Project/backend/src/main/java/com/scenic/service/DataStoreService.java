package com.scenic.service;

import com.scenic.entity.Area;
import com.scenic.entity.GuidanceMessage;
import com.scenic.entity.ScenicSpot;
import com.scenic.entity.CrowdSuggestion;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DataStoreService {
    private final Map<Long, Area> areas = new ConcurrentHashMap<>();
    private final Map<Long, ScenicSpot> spots = new ConcurrentHashMap<>();
    private final List<GuidanceMessage> guidanceMessages = new ArrayList<>();
    private final List<CrowdSuggestion> crowdSuggestions = new ArrayList<>();
    private long nextId = 1;

    @PostConstruct
    public void init() {
        initializeTestData();
    }

    private void initializeTestData() {
        Area mainEntrance = new Area(1L, "主入口广场", "景区主入口，游客集散中心", 0.5, 0.1, 500);
        Area ancientTemple = new Area(2L, "古寺区", "历史悠久的古建筑群", 0.3, 0.4, 300);
        Area lakeView = new Area(3L, "湖区", "风景优美的湖泊区域", 0.7, 0.3, 400);
        Area mountain = new Area(4L, "山岳区", "登山步道和观景台", 0.5, 0.7, 200);
        Area garden = new Area(5L, "园林区", "江南园林风格庭院", 0.2, 0.6, 250);
        Area playground = new Area(6L, "游乐区", "亲子游乐设施区域", 0.8, 0.6, 350);

        mainEntrance.getAdjacentAreaIds().addAll(Arrays.asList(2L, 3L, 5L));
        ancientTemple.getAdjacentAreaIds().addAll(Arrays.asList(1L, 4L, 5L));
        lakeView.getAdjacentAreaIds().addAll(Arrays.asList(1L, 4L, 6L));
        mountain.getAdjacentAreaIds().addAll(Arrays.asList(2L, 3L, 5L, 6L));
        garden.getAdjacentAreaIds().addAll(Arrays.asList(1L, 2L, 4L));
        playground.getAdjacentAreaIds().addAll(Arrays.asList(3L, 4L));

        areas.put(1L, mainEntrance);
        areas.put(2L, ancientTemple);
        areas.put(3L, lakeView);
        areas.put(4L, mountain);
        areas.put(5L, garden);
        areas.put(6L, playground);

        spots.put(1L, new ScenicSpot(1L, "主大门", "景区主入口检票处", 1L, 0.5, 0.1, 200));
        spots.put(2L, new ScenicSpot(2L, "游客中心", "信息咨询和休息区", 1L, 0.55, 0.12, 150));
        spots.put(3L, new ScenicSpot(3L, "大雄宝殿", "核心古建筑", 2L, 0.3, 0.4, 100));
        spots.put(4L, new ScenicSpot(4L, "藏经阁", "古代典籍展示", 2L, 0.28, 0.42, 80));
        spots.put(5L, new ScenicSpot(5L, "湖心亭", "湖中观景亭", 3L, 0.7, 0.35, 50));
        spots.put(6L, new ScenicSpot(6L, "游船码头", "游船乘坐点", 3L, 0.75, 0.28, 100));
        spots.put(7L, new ScenicSpot(7L, "观景台", "山顶观景平台", 4L, 0.5, 0.75, 80));
        spots.put(8L, new ScenicSpot(8L, "登山步道", "主要登山路线", 4L, 0.5, 0.65, 120));
        spots.put(9L, new ScenicSpot(9L, "盆景园", "名贵盆景展示", 5L, 0.2, 0.62, 60));
        spots.put(10L, new ScenicSpot(10L, "假山瀑布", "人造景观", 5L, 0.22, 0.58, 70));
        spots.put(11L, new ScenicSpot(11L, "过山车", "大型游乐设施", 6L, 0.8, 0.6, 150));
        spots.put(12L, new ScenicSpot(12L, "旋转木马", "亲子设施", 6L, 0.82, 0.65, 80));

        for (Area area : areas.values()) {
            for (ScenicSpot spot : spots.values()) {
                if (spot.getAreaId().equals(area.getId())) {
                    area.getSpots().add(spot);
                }
            }
        }
    }

    public Collection<Area> getAllAreas() {
        return areas.values();
    }

    public Area getAreaById(Long id) {
        return areas.get(id);
    }

    public Collection<ScenicSpot> getAllSpots() {
        return spots.values();
    }

    public ScenicSpot getSpotById(Long id) {
        return spots.get(id);
    }

    public List<ScenicSpot> getSpotsByAreaId(Long areaId) {
        List<ScenicSpot> result = new ArrayList<>();
        for (ScenicSpot spot : spots.values()) {
            if (spot.getAreaId().equals(areaId)) {
                result.add(spot);
            }
        }
        return result;
    }

    public synchronized long getNextId() {
        return nextId++;
    }

    public List<GuidanceMessage> getAllGuidanceMessages() {
        return guidanceMessages;
    }

    public List<GuidanceMessage> getActiveGuidanceMessages() {
        List<GuidanceMessage> result = new ArrayList<>();
        for (GuidanceMessage msg : guidanceMessages) {
            if (msg.isActive()) {
                result.add(msg);
            }
        }
        return result;
    }

    public void addGuidanceMessage(GuidanceMessage message) {
        guidanceMessages.add(0, message);
    }

    public GuidanceMessage getGuidanceMessageById(Long id) {
        for (GuidanceMessage msg : guidanceMessages) {
            if (msg.getId().equals(id)) {
                return msg;
            }
        }
        return null;
    }

    public List<CrowdSuggestion> getAllCrowdSuggestions() {
        return crowdSuggestions;
    }

    public List<CrowdSuggestion> getActiveCrowdSuggestions() {
        List<CrowdSuggestion> result = new ArrayList<>();
        for (CrowdSuggestion suggestion : crowdSuggestions) {
            if (suggestion.isActive()) {
                result.add(suggestion);
            }
        }
        return result;
    }

    public void addCrowdSuggestion(CrowdSuggestion suggestion) {
        crowdSuggestions.add(0, suggestion);
    }

    public void clearOldSuggestions() {
        Iterator<CrowdSuggestion> it = crowdSuggestions.iterator();
        while (it.hasNext()) {
            CrowdSuggestion s = it.next();
            if (!s.isActive()) {
                it.remove();
            }
        }
    }
}