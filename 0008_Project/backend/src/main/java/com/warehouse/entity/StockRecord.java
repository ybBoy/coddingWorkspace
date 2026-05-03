package com.warehouse.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * 库存操作记录实体类
 * 用于记录零件的入库和出库操作记录
 * 包含操作类型、数量变化、操作人IP等信息
 */
public class StockRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录编号，自动生成
     * 格式：REC-yyyyMMddHHmmss-0001
     */
    private String id;
    
    /**
     * 关联的零件编号
     */
    private String partId;
    
    /**
     * 零件名称（冗余存储，便于查询）
     */
    private String partName;
    
    /**
     * 零件分类
     */
    private String category;
    
    /**
     * 操作类型
     * "入库" 或 "出库"
     */
    private String type;
    
    /**
     * 操作数量
     */
    private int quantity;
    
    /**
     * 计量单位
     */
    private String unit;
    
    /**
     * 操作前库存数量
     */
    private int beforeQuantity;
    
    /**
     * 操作后库存数量
     */
    private int afterQuantity;
    
    /**
     * 操作人IP地址
     * 用于追踪操作来源
     */
    private String ipAddress;
    
    /**
     * 操作时间
     */
    private Date createTime;

    /**
     * 默认构造函数
     * 自动设置创建时间为当前时间
     */
    public StockRecord() {
        this.createTime = new Date();
    }

    /**
     * 带参数的构造函数
     * @param id 记录编号
     * @param partId 零件编号
     * @param partName 零件名称
     * @param category 零件分类
     * @param type 操作类型（入库/出库）
     * @param quantity 操作数量
     * @param unit 计量单位
     * @param beforeQuantity 操作前数量
     * @param afterQuantity 操作后数量
     * @param ipAddress 操作人IP
     */
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
