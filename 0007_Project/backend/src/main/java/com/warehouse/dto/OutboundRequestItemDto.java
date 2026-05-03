package com.warehouse.dto;

import java.io.Serializable;

/**
 * 出库申请明细项DTO
 * 用于接收用户提交的单个零件申请数据
 */
public class OutboundRequestItemDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 零件编号
     */
    private String partId;
    
    /**
     * 申请数量
     */
    private int quantity;

    public OutboundRequestItemDto() {
    }

    public OutboundRequestItemDto(String partId, int quantity) {
        this.partId = partId;
        this.quantity = quantity;
    }

    public String getPartId() {
        return partId;
    }

    public void setPartId(String partId) {
        this.partId = partId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
