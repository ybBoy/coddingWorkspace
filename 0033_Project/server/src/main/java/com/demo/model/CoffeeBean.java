package com.demo.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CoffeeBean {

    private String id;
    private String name;
    private String origin;
    private String roastLevel;
    private int stockGrams;
    private int minStockLevel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private List<StockRecord> stockRecords;

    public CoffeeBean() {
        this.stockRecords = new ArrayList<>();
    }

    public CoffeeBean(String id, String name, String origin, String roastLevel, int stockGrams, int minStockLevel, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.origin = origin;
        this.roastLevel = roastLevel;
        this.stockGrams = stockGrams;
        this.minStockLevel = minStockLevel;
        this.createdAt = createdAt;
        this.stockRecords = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getRoastLevel() {
        return roastLevel;
    }

    public void setRoastLevel(String roastLevel) {
        this.roastLevel = roastLevel;
    }

    public int getStockGrams() {
        return stockGrams;
    }

    public void setStockGrams(int stockGrams) {
        this.stockGrams = stockGrams;
    }

    public int getMinStockLevel() {
        return minStockLevel;
    }

    public void setMinStockLevel(int minStockLevel) {
        this.minStockLevel = minStockLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<StockRecord> getStockRecords() {
        return stockRecords;
    }

    public void setStockRecords(List<StockRecord> stockRecords) {
        this.stockRecords = stockRecords;
    }

    public boolean isLowStock() {
        return stockGrams <= minStockLevel;
    }
}
