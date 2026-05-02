package com.warehouse.service;

import com.warehouse.entity.StockRecord;
import com.warehouse.store.StockRecordDataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class StockRecordService {

    @Autowired
    private StockRecordDataStore stockRecordDataStore;

    @Autowired
    private FileStorageService fileStorageService;

    private int recordCounter = 0;

    public List<StockRecord> getAllRecords() {
        return stockRecordDataStore.getAllRecords();
    }

    public List<StockRecord> getRecordsByType(String type) {
        return stockRecordDataStore.getRecordsByType(type);
    }

    public List<StockRecord> getRecordsByCategory(String category) {
        return stockRecordDataStore.getRecordsByCategory(category);
    }

    public List<StockRecord> getRecordsByPartId(String partId) {
        return stockRecordDataStore.getRecordsByPartId(partId);
    }

    public List<StockRecord> getRecordsByIpAddress(String ipAddress) {
        return stockRecordDataStore.getRecordsByIpAddress(ipAddress);
    }

    public void addRecord(StockRecord record) {
        record.setId(generateRecordId());
        stockRecordDataStore.saveRecord(record);
        fileStorageService.saveRecords();
    }

    public Map<String, Object> getSummaryByCategory() {
        Map<String, Object> summary = new HashMap<>();
        List<StockRecord> allRecords = stockRecordDataStore.getAllRecords();
        
        Map<String, Integer> stockInByCategory = new HashMap<>();
        Map<String, Integer> stockOutByCategory = new HashMap<>();
        
        for (StockRecord record : allRecords) {
            String category = record.getCategory();
            int quantity = record.getQuantity();
            
            if ("入库".equals(record.getType())) {
                stockInByCategory.put(category, stockInByCategory.getOrDefault(category, 0) + quantity);
            } else {
                stockOutByCategory.put(category, stockOutByCategory.getOrDefault(category, 0) + quantity);
            }
        }
        
        List<Map<String, Object>> categorySummary = new ArrayList<>();
        Set<String> allCategories = new HashSet<>();
        allCategories.addAll(stockInByCategory.keySet());
        allCategories.addAll(stockOutByCategory.keySet());
        
        for (String category : allCategories) {
            Map<String, Object> item = new HashMap<>();
            item.put("category", category);
            item.put("stockInQuantity", stockInByCategory.getOrDefault(category, 0));
            item.put("stockOutQuantity", stockOutByCategory.getOrDefault(category, 0));
            item.put("balance", stockInByCategory.getOrDefault(category, 0) - stockOutByCategory.getOrDefault(category, 0));
            categorySummary.add(item);
        }
        
        summary.put("categorySummary", categorySummary);
        summary.put("totalStockIn", stockInByCategory.values().stream().mapToInt(Integer::intValue).sum());
        summary.put("totalStockOut", stockOutByCategory.values().stream().mapToInt(Integer::intValue).sum());
        summary.put("totalRecords", allRecords.size());
        
        return summary;
    }

    private synchronized String generateRecordId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = sdf.format(new Date());
        recordCounter++;
        return "REC-" + timestamp + "-" + String.format("%04d", recordCounter);
    }
}
