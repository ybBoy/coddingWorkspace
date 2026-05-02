package com.warehouse.entity;

import java.io.Serializable;

public class Part implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String category;
    private String specification;
    private int quantity;
    private int minStock;
    private String unit;

    public Part() {
    }

    public Part(String id, String name, String category, String specification, int quantity, int minStock, String unit) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.specification = specification;
        this.quantity = quantity;
        this.minStock = minStock;
        this.unit = unit;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getMinStock() {
        return minStock;
    }

    public void setMinStock(int minStock) {
        this.minStock = minStock;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean needsRestock() {
        return this.quantity <= this.minStock;
    }

    public String getStatus() {
        return needsRestock() ? "需补货" : "正常";
    }
}
