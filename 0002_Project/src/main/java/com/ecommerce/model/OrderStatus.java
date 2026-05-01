package com.ecommerce.model;

/**
 * 订单状态枚举
 * 定义订单的状态流转：待支付 -> 已支付/已取消 -> 已完成
 */
public enum OrderStatus {

    /**
     * 待支付
     * 订单创建后的初始状态，可以支付或取消
     */
    PENDING("待支付"),

    /**
     * 已支付
     * 订单已成功支付，可以确认收货
     */
    PAID("已支付"),

    /**
     * 已取消
     * 订单已取消（只有待支付状态才能取消）
     */
    CANCELLED("已取消"),

    /**
     * 已完成
     * 订单已完成（已支付并确认收货）
     */
    COMPLETED("已完成");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 检查是否可以从当前状态转换到目标状态
     * @param targetStatus 目标状态
     * @return 是否可以转换
     */
    public boolean canTransitionTo(OrderStatus targetStatus) {
        switch (this) {
            case PENDING:
                // 待支付可以转换为已支付或已取消
                return targetStatus == PAID || targetStatus == CANCELLED;
            case PAID:
                // 已支付可以转换为已完成
                return targetStatus == COMPLETED;
            case CANCELLED:
            case COMPLETED:
                // 已取消和已完成是终态，不能转换
                return false;
            default:
                return false;
        }
    }
}
