package com.ecommerce.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单项实体类
 * 表示订单中的一个商品项
 */
public class OrderItem {

    /**
     * 订单项ID
     */
    private String id;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 商品ID
     */
    private String productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品单价（下单时的价格快照）
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

    // 构造函数
    public OrderItem() {
        this.createdAt = LocalDateTime.now();
    }

    public OrderItem(String id, String orderId, String productId, String productName,
                     BigDecimal productPrice, Integer quantity) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.subtotal = productPrice.multiply(new BigDecimal(quantity));
        this.createdAt = LocalDateTime.now();
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

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
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
}
