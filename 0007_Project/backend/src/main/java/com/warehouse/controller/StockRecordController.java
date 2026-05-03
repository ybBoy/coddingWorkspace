package com.warehouse.controller;

import com.warehouse.dto.ApiResponse;
import com.warehouse.entity.StockRecord;
import com.warehouse.service.StockRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/records")
public class StockRecordController {

    @Autowired
    private StockRecordService stockRecordService;

    @GetMapping
    public ApiResponse<List<StockRecord>> getAllRecords(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String partId) {
        
        List<StockRecord> records;
        
        if (partId != null && !partId.isEmpty()) {
            records = stockRecordService.getRecordsByPartId(partId);
        } else if (category != null && !category.isEmpty()) {
            records = stockRecordService.getRecordsByCategory(category);
        } else if (type != null && !type.isEmpty()) {
            records = stockRecordService.getRecordsByType(type);
        } else {
            records = stockRecordService.getAllRecords();
        }
        
        return ApiResponse.success(records);
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> getSummary() {
        Map<String, Object> summary = stockRecordService.getSummaryByCategory();
        return ApiResponse.success(summary);
    }
}
