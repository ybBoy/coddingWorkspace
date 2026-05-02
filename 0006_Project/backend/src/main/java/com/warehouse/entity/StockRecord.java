package com.warehouse.entity;

import java.io.Serializable;
import java.util.Date;

public class StockRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String partId;
    private String partName;
    private String category;
    private String type;
    private int quantity;
    private String unit;
    private int beforeQuantity;
    private int afterQuantity;
    private String ipAddress;
    private Date createTime;

    public StockRecord() {
        this.createTime = new Date();
    }

    public StockRecord(String id, String partId, String partName, String category, String type, 
                       int quantity, String unit, int beforeQuantity, int afterQuantity, String ipAddress) {
        this.id = id;
        this.partId = partId;
        this.partName = partName;
        this.category = category;
        this.type = type;
        this.quantity = quantity;
        this.unit = unit;
        this.beforeQuantity = beforeQuantity;
        this.afterQuantity = afterQuantity;
        this.ipAddress = ipAddress;
        this.createTime = new Date();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPartId() {
        return partId;
    }

    public void setPartId(String partId) {
        this.partId = partId;
    }

    public String getPartName() {
        return partName;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getBeforeQuantity() {
        return beforeQuantity;
    }

    public void setBeforeQuantity(int beforeQuantity) {
        this.beforeQuantity = beforeQuantity;
    }

    public int getAfterQuantity() {
        return afterQuantity;
    }

    public void setAfterQuantity(int afterQuantity) {
        this.afterQuantity = afterQuantity;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
