package com.demo.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class StockRecord {

    private String id;
    private String beanId;
    private String type;
    private int quantity;
    private int beforeStock;
    private int afterStock;
    private String operator;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public StockRecord() {
    }

    public StockRecord(String id, String beanId, String type, int quantity,
                       int beforeStock, int afterStock, String operator, String remark,
                       LocalDateTime timestamp) {
        this.id = id;
        this.beanId = beanId;
        this.type = type;
        this.quantity = quantity;
        this.beforeStock = beforeStock;
        this.afterStock = afterStock;
        this.operator = operator;
        this.remark = remark;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBeanId() {
        return beanId;
    }

    public void setBeanId(String beanId) {
        this.beanId = beanId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getBeforeStock() {
        return beforeStock;
    }

    public void setBeforeStock(int beforeStock) {
        this.beforeStock = beforeStock;
    }

    public int getAfterStock() {
        return afterStock;
    }

    public void setAfterStock(int afterStock) {
        this.afterStock = afterStock;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
