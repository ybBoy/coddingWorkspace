package com.monitoring.service;

import com.monitoring.model.MonitorDataPoint;
import com.monitoring.model.MonitorItem;
import com.monitoring.storage.JsonFileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class HistoryDataService {

    private static final Logger logger = LoggerFactory.getLogger(HistoryDataService.class);

    private static final String HISTORY_DATA_FILE = "history_data.json";

    @Autowired
    private JsonFileStorage jsonFileStorage;

    @Value("${data.retention.days:7}")
    private int retentionDays;

    private Map<String, List<MonitorDataPoint>> dataStore = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadFromStorage();
        logger.info("历史数据服务初始化完成，数据保留天数: {} 天", retentionDays);
    }

    public void saveDataPoint(String serverId, String monitorType, double value) {
        String key = buildKey(serverId, monitorType);
        MonitorDataPoint point = new MonitorDataPoint(serverId, monitorType, value, LocalDateTime.now());
        
        dataStore.compute(key, (k, points) -> {
            if (points == null) {
                points = new ArrayList<>();
            }
            points.add(point);
            return points;
        });

        cleanupOldData();
        saveToStorage();
    }

    public List<MonitorDataPoint> getLastHourData(String serverId, String monitorType) {
        String key = buildKey(serverId, monitorType);
        List<MonitorDataPoint> points = dataStore.get(key);
        if (points == null) {
            return Collections.emptyList();
        }

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        return points.stream()
                .filter(p -> p.getTimestamp().isAfter(oneHourAgo))
                .sorted(Comparator.comparing(MonitorDataPoint::getTimestamp))
                .collect(Collectors.toList());
    }

    public List<MonitorDataPoint> getLastDayData(String serverId, String monitorType) {
        String key = buildKey(serverId, monitorType);
        List<MonitorDataPoint> points = dataStore.get(key);
        if (points == null) {
            return Collections.emptyList();
        }

        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        return points.stream()
                .filter(p -> p.getTimestamp().isAfter(oneDayAgo))
                .sorted(Comparator.comparing(MonitorDataPoint::getTimestamp))
                .collect(Collectors.toList());
    }

    public Map<String, List<MonitorDataPoint>> getLastHourDataForServer(String serverId) {
        Map<String, List<MonitorDataPoint>> result = new LinkedHashMap<>();
        result.put(MonitorItem.CPU, getLastHourData(serverId, MonitorItem.CPU));
        result.put(MonitorItem.MEMORY, getLastHourData(serverId, MonitorItem.MEMORY));
        result.put(MonitorItem.DISK, getLastHourData(serverId, MonitorItem.DISK));
        return result;
    }

    public Map<String, List<MonitorDataPoint>> getLastDayDataForServer(String serverId) {
        Map<String, List<MonitorDataPoint>> result = new LinkedHashMap<>();
        result.put(MonitorItem.CPU, getLastDayData(serverId, MonitorItem.CPU));
        result.put(MonitorItem.MEMORY, getLastDayData(serverId, MonitorItem.MEMORY));
        result.put(MonitorItem.DISK, getLastDayData(serverId, MonitorItem.DISK));
        return result;
    }

    private void cleanupOldData() {
        LocalDateTime retentionThreshold = LocalDateTime.now().minusDays(retentionDays);
        
        dataStore.forEach((key, points) -> {
            List<MonitorDataPoint> filtered = points.stream()
                    .filter(p -> p.getTimestamp().isAfter(retentionThreshold))
                    .collect(Collectors.toList());
            
            if (filtered.size() < points.size()) {
                dataStore.put(key, filtered);
                logger.debug("清理了 {} 条过期数据，保留了 {} 条", 
                        points.size() - filtered.size(), filtered.size());
            }
        });
    }

    private String buildKey(String serverId, String monitorType) {
        return serverId + "_" + monitorType;
    }

    @SuppressWarnings("unchecked")
    private void loadFromStorage() {
        try {
            Object data = jsonFileStorage.load(HISTORY_DATA_FILE, Object.class);
            if (data instanceof Map) {
                Map<String, List<Map<String, Object>>> rawData = (Map<String, List<Map<String, Object>>>) data;
                dataStore = new ConcurrentHashMap<>();
                
                for (Map.Entry<String, List<Map<String, Object>>> entry : rawData.entrySet()) {
                    List<MonitorDataPoint> points = new ArrayList<>();
                    for (Map<String, Object> pointData : entry.getValue()) {
                        MonitorDataPoint point = new MonitorDataPoint();
                        point.setServerId((String) pointData.get("serverId"));
                        point.setMonitorType((String) pointData.get("monitorType"));
                        
                        Object valueObj = pointData.get("value");
                        if (valueObj instanceof Number) {
                            point.setValue(((Number) valueObj).doubleValue());
                        }
                        
                        Object timestampObj = pointData.get("timestamp");
                        if (timestampObj != null) {
                            try {
                                point.setTimestamp(LocalDateTime.parse(timestampObj.toString()));
                            } catch (Exception e) {
                                point.setTimestamp(LocalDateTime.now());
                            }
                        }
                        points.add(point);
                    }
                    dataStore.put(entry.getKey(), points);
                }
                
                logger.info("从文件加载了历史数据，共 {} 个监控项", dataStore.size());
            }
        } catch (Exception e) {
            logger.warn("加载历史数据失败，将创建新的数据存储: {}", e.getMessage());
            dataStore = new ConcurrentHashMap<>();
        }
    }

    private void saveToStorage() {
        try {
            jsonFileStorage.save(HISTORY_DATA_FILE, dataStore);
        } catch (Exception e) {
            logger.error("保存历史数据失败", e);
        }
    }

    public int getDataPointCount() {
        return dataStore.values().stream().mapToInt(List::size).sum();
    }

    public Set<String> getMonitoredKeys() {
        return new HashSet<>(dataStore.keySet());
    }
}
