package com.warehouse.store;

import com.warehouse.entity.StockRecord;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class StockRecordDataStore {

    private Map<String, StockRecord> recordsMap = new ConcurrentHashMap<>();

    public void saveRecord(StockRecord record) {
        recordsMap.put(record.getId(), record);
    }

    public List<StockRecord> getAllRecords() {
        List<StockRecord> records = new ArrayList<>(recordsMap.values());
        records.sort((r1, r2) -> r2.getCreateTime().compareTo(r1.getCreateTime()));
        return records;
    }

    public List<StockRecord> getRecordsByType(String type) {
        return recordsMap.values().stream()
                .filter(r -> r.getType().equals(type))
                .sorted((r1, r2) -> r2.getCreateTime().compareTo(r1.getCreateTime()))
                .collect(Collectors.toList());
    }

    public List<StockRecord> getRecordsByCategory(String category) {
        return recordsMap.values().stream()
                .filter(r -> r.getCategory().equals(category))
                .sorted((r1, r2) -> r2.getCreateTime().compareTo(r1.getCreateTime()))
                .collect(Collectors.toList());
    }

    public List<StockRecord> getRecordsByPartId(String partId) {
        return recordsMap.values().stream()
                .filter(r -> r.getPartId().equals(partId))
                .sorted((r1, r2) -> r2.getCreateTime().compareTo(r1.getCreateTime()))
                .collect(Collectors.toList());
    }

    public void setRecordsMap(Map<String, StockRecord> recordsMap) {
        this.recordsMap = recordsMap;
    }

    public Map<String, StockRecord> getRecordsMap() {
        return new ConcurrentHashMap<>(recordsMap);
    }
}
