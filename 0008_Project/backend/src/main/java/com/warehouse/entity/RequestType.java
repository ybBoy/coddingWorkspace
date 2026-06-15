package com.warehouse.entity;

/**
 * 申请类型枚举
 * 定义出库申请和退货入库申请两种类型
 */
public enum RequestType {
    /**
     * 出库申请
     * 用户申请领取零件出库
     */
    OUTBOUND("出库申请"),
    
    /**
     * 退货入库申请
     * 用户申请将使用不完的零件归还入库
     */
    RETURN("退货入库申请");

    /**
     * 类型的中文描述
     */
    private String description;

    RequestType(String description) {
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
    public static RequestType fromDescription(String description) {
        for (RequestType type : RequestType.values()) {
            if (type.getDescription().equals(description)) {
                return type;
            }
        }
        return null;
    }
}
