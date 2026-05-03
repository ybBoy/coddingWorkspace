package com.warehouse.entity;

import java.io.Serializable;

/**
 * 出库申请明细项实体类
 * 表示单个出库申请中的一个零件申请项
 * 支持一次申请多个零件
 */
public class OutboundRequestItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 零件编号
     */
    private String partId;
    
    /**
     * 零件名称（冗余存储，便于显示）
     */
    private String partName;
    
    /**
     * 零件分类
     */
    private String category;
    
    /**
     * 申请数量
     */
    private int quantity;
    
    /**
     * 计量单位
     */
    private String unit;
    
    /**
     * 申请时的库存数量
     * 用于审核时参考当前库存是否充足
     */
    private int currentStock;

    /**
     * 默认构造函数
     */
    public OutboundRequestItem() {
    }

    /**
     * 全参数构造函数
     * @param partId 零件编号
     * @param partName 零件名称
     * @param category 分类
     * @param quantity 申请数量
     * @param unit 计量单位
     * @param currentStock 当前库存
     */
    public OutboundRequestItem(String partId, String partName, String category, 
                               int quantity, String unit, int currentStock) {
        this.partId = partId;
        this.partName = partName;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.currentStock = currentStock;
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

    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
    }
}
