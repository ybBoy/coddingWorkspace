package com.demo.controller;

import com.demo.model.CoffeeBean;
import com.demo.model.StockRecord;
import com.demo.service.BeanInventoryService;
import org.springframework.http.HttpStatus;
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
    public List<CoffeeBean> getAllBeans(@RequestParam(required = false) String roastLevel) {
        if (roastLevel != null && !roastLevel.isEmpty()) {
            return service.getBeansByRoastLevel(roastLevel);
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
    public ResponseEntity<CoffeeBean> addBean(@RequestBody CoffeeBean bean) {
        CoffeeBean saved = service.addBean(bean);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/restock")
    public ResponseEntity<?> restockBean(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        try {
            int amount = body.getOrDefault("amount", 0);
            CoffeeBean updated = service.restockBean(id, amount);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/consume")
    public ResponseEntity<?> consumeBean(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        try {
            int amount = body.getOrDefault("amount", 0);
            CoffeeBean updated = service.consumeBean(id, amount);
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
}
