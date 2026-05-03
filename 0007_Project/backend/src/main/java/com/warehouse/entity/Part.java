package com.warehouse.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

/**
 * 零件实体类
 * 用于表示电动汽车库房中的零件信息
 * 包含零件的基本属性、库存数量、最低库存预警等信息
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Part implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 零件编号，唯一标识
     */
    private String id;
    
    /**
     * 零件名称
     */
    private String name;
    
    /**
     * 零件分类（如：电池、电机、轮胎等）
     * 参见 PartCategory 枚举
     */
    private String category;
    
    /**
     * 零件规格描述
     */
    private String specification;
    
    /**
     * 当前库存数量
     */
    private int quantity;
    
    /**
     * 最低库存预警值
     * 当 quantity <= minStock 时，状态显示为"需补货"
     */
    private int minStock;
    
    /**
     * 计量单位（如：块、台、条、升等）
     */
    private String unit;
    
    /**
     * 可见性标志
     * true: 对普通用户可见
     * false: 仅管理员可见
     */
    private boolean visible = true;

    /**
     * 默认构造函数
     */
    public Part() {
    }

    /**
     * 带参数的构造函数
     * @param id 零件编号
     * @param name 零件名称
     * @param category 零件分类
     * @param specification 零件规格
     * @param quantity 库存数量
     * @param minStock 最低库存预警值
     * @param unit 计量单位
     */
    public Part(String id, String name, String category, String specification, int quantity, int minStock, String unit) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.specification = specification;
        this.quantity = quantity;
        this.minStock = minStock;
        this.unit = unit;
        this.visible = true;
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

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * 判断是否需要补货
     * @return true 表示需要补货（当前库存 <= 最低库存预警值），false 表示库存充足
     */
    public boolean needsRestock() {
        return this.quantity <= this.minStock;
    }

    /**
     * 获取零件库存状态
     * @return "需补货" 或 "正常"
     */
    public String getStatus() {
        return needsRestock() ? "需补货" : "正常";
    }
}
