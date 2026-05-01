package com.ecommerce.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车项实体类
 * 表示购物车中的一个商品项
 */
public class CartItem {

    /**
     * 购物车项ID
     */
    private String id;

    /**
     * 商品ID
     */
    private String productId;

    /**
     * 商品名称（冗余存储，方便展示）
     */
    private String productName;

    /**
     * 商品单价
     */
    private BigDecimal productPrice;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 小计金额（单价 * 数量）
     */
    private BigDecimal subtotal;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // 构造函数
    public CartItem() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public CartItem(String id, String productId, String productName, BigDecimal productPrice, Integer quantity) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.subtotal = productPrice.multiply(new BigDecimal(quantity));
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 计算小计金额
     */
    public void calculateSubtotal() {
        if (this.productPrice != null && this.quantity != null) {
            this.subtotal = this.productPrice.multiply(new BigDecimal(this.quantity));
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
        calculateSubtotal();
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        this.updatedAt = LocalDateTime.now();
        calculateSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
