package com.demo.controller;

import com.demo.dto.EditBeanRequest;
import com.demo.dto.StatisticsResponse;
import com.demo.dto.StockOperationRequest;
import com.demo.model.CoffeeBean;
import com.demo.model.StockRecord;
import com.demo.service.BeanInventoryService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/beans")
public class BeanController {

    private final BeanInventoryService service;

    public BeanController(BeanInventoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<CoffeeBean> getAllBeans(
            @RequestParam(required = false) String roastLevel,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir) {
        if (roastLevel != null || search != null || sortBy != null) {
            return service.searchAndSort(search, roastLevel, sortBy, sortDir);
        }
        return service.getAllBeans();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CoffeeBean> getBeanById(@PathVariable String id) {
        Optional<CoffeeBean> bean = service.getBeanById(id);
        return bean.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> addBean(@RequestBody CoffeeBean bean) {
        try {
            CoffeeBean saved = service.addBean(bean);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBean(@PathVariable String id, @RequestBody EditBeanRequest req) {
        try {
            CoffeeBean updated = service.updateBean(id, req);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/restock")
    public ResponseEntity<?> restockBean(@PathVariable String id, @RequestBody StockOperationRequest req) {
        try {
            CoffeeBean updated = service.restockBean(id, req);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/consume")
    public ResponseEntity<?> consumeBean(@PathVariable String id, @RequestBody StockOperationRequest req) {
        try {
            CoffeeBean updated = service.consumeBean(id, req);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBean(@PathVariable String id) {
        boolean deleted = service.deleteBean(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/records")
    public ResponseEntity<List<StockRecord>> getRecentRecords(
            @PathVariable String id,
            @RequestParam(defaultValue = "5") int limit) {
        List<StockRecord> records = service.getRecentRecords(id, limit);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/low-stock/count")
    public Map<String, Long> getLowStockCount() {
        Map<String, Long> result = new HashMap<>();
        result.put("count", service.getLowStockCount());
        return result;
    }

    @GetMapping("/warnings/summary")
    public Map<String, Object> getWarningSummary() {
        return service.getWarningSummary();
    }

    @GetMapping("/statistics")
    public StatisticsResponse getStatistics() {
        return service.getStatistics();
    }

    @PostMapping("/import")
    public ResponseEntity<?> importBeans(
            @RequestBody List<CoffeeBean> beans,
            @RequestParam(required = false, defaultValue = "false") boolean replace) {
        try {
            List<CoffeeBean> saved = service.importBeans(beans, replace);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Import failed: " + e.getMessage());
        }
    }

    @GetMapping("/export/json")
    public ResponseEntity<List<CoffeeBean>> exportJson() {
        return ResponseEntity.ok(service.getAllBeans());
    }

    @GetMapping("/export/csv")
    public ResponseEntity<String> exportCsv() {
        List<CoffeeBean> beans = service.getAllBeans();
        StringBuilder sb = new StringBuilder();
        sb.append("id,name,origin,roastLevel,stockGrams,minStockLevel,createdAt\n");
        for (CoffeeBean b : beans) {
            sb.append(escapeCsv(b.getId())).append(",");
            sb.append(escapeCsv(b.getName())).append(",");
            sb.append(escapeCsv(b.getOrigin())).append(",");
            sb.append(b.getRoastLevel()).append(",");
            sb.append(b.getStockGrams()).append(",");
            sb.append(b.getMinStockLevel()).append(",");
            sb.append(b.getCreatedAt() != null ? b.getCreatedAt().toString() : "").append("\n");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=coffee_beans.csv");
        return new ResponseEntity<>(sb.toString(), headers, HttpStatus.OK);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
