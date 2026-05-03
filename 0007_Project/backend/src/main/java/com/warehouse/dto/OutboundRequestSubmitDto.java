package com.warehouse.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 出库申请提交DTO
 * 用于接收用户提交的出库申请数据
 */
public class OutboundRequestSubmitDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 申请明细列表
     */
    private List<OutboundRequestItemDto> items = new ArrayList<>();
    
    /**
     * 申请备注
     */
    private String remark;

    public OutboundRequestSubmitDto() {
    }

    public List<OutboundRequestItemDto> getItems() {
        return items;
    }

    public void setItems(List<OutboundRequestItemDto> items) {
        this.items = items;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
