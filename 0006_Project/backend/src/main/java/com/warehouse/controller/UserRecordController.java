package com.warehouse.controller;

import com.warehouse.dto.ApiResponse;
import com.warehouse.entity.StockRecord;
import com.warehouse.service.StockRecordService;
import com.warehouse.util.IpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/records")
public class UserRecordController {

    @Autowired
    private StockRecordService stockRecordService;

    @GetMapping
    public ApiResponse<List<StockRecord>> getMyRecords(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String partId,
            HttpServletRequest request) {
        
        String ipAddress = IpUtils.getClientIpAddress(request);
        List<StockRecord> allRecords = stockRecordService.getRecordsByIpAddress(ipAddress);
        
        List<StockRecord> filtered = allRecords;
        
        if (partId != null && !partId.isEmpty()) {
            filtered = filtered.stream()
                    .filter(r -> r.getPartId().equals(partId))
                    .collect(java.util.stream.Collectors.toList());
        }
        if (category != null && !category.isEmpty()) {
            filtered = filtered.stream()
                    .filter(r -> r.getCategory().equals(category))
                    .collect(java.util.stream.Collectors.toList());
        }
        if (type != null && !type.isEmpty()) {
            filtered = filtered.stream()
                    .filter(r -> r.getType().equals(type))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        return ApiResponse.success(filtered);
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> getMySummary(HttpServletRequest request) {
        String ipAddress = IpUtils.getClientIpAddress(request);
        List<StockRecord> myRecords = stockRecordService.getRecordsByIpAddress(ipAddress);
        
        Map<String, Object> summary = new java.util.HashMap<>();
        
        Map<String, Integer> stockInByCategory = new java.util.HashMap<>();
        Map<String, Integer> stockOutByCategory = new java.util.HashMap<>();
        
        for (StockRecord record : myRecords) {
            String category = record.getCategory();
            int quantity = record.getQuantity();
            
            if ("入库".equals(record.getType())) {
                stockInByCategory.put(category, stockInByCategory.getOrDefault(category, 0) + quantity);
            } else {
                stockOutByCategory.put(category, stockOutByCategory.getOrDefault(category, 0) + quantity);
            }
        }
        
        List<Map<String, Object>> categorySummary = new java.util.ArrayList<>();
        java.util.Set<String> allCategories = new java.util.HashSet<>();
        allCategories.addAll(stockInByCategory.keySet());
        allCategories.addAll(stockOutByCategory.keySet());
        
        for (String category : allCategories) {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("category", category);
            item.put("stockInQuantity", stockInByCategory.getOrDefault(category, 0));
            item.put("stockOutQuantity", stockOutByCategory.getOrDefault(category, 0));
            item.put("balance", stockInByCategory.getOrDefault(category, 0) - stockOutByCategory.getOrDefault(category, 0));
            categorySummary.add(item);
        }
        
        summary.put("categorySummary", categorySummary);
        summary.put("totalStockIn", stockInByCategory.values().stream().mapToInt(Integer::intValue).sum());
        summary.put("totalStockOut", stockOutByCategory.values().stream().mapToInt(Integer::intValue).sum());
        summary.put("totalRecords", myRecords.size());
        summary.put("clientIp", ipAddress);
        
        return ApiResponse.success(summary);
    }
}
