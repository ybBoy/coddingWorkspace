package com.warehouse.entity;

/**
 * 出库申请状态枚举
 * 定义出库申请的生命周期状态
 */
public enum RequestStatus {
    /**
     * 待审核
     * 用户已提交申请，等待管理员处理
     */
    PENDING("待审核"),
    
    /**
     * 已通过
     * 管理员审核通过，已完成出库操作
     */
    APPROVED("已通过"),
    
    /**
     * 已拒绝
     * 管理员拒绝申请
     */
    REJECTED("已拒绝");

    /**
     * 状态的中文描述
     */
    private String description;

    RequestStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据中文描述获取对应的枚举值
     * @param description 中文描述
     * @return 对应的枚举值，找不到则返回null
     */
    public static RequestStatus fromDescription(String description) {
        for (RequestStatus status : RequestStatus.values()) {
            if (status.getDescription().equals(description)) {
                return status;
            }
        }
        return null;
    }
}
