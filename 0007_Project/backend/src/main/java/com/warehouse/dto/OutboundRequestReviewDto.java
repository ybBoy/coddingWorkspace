package com.warehouse.dto;

import java.io.Serializable;

/**
 * 出库申请审核DTO
 * 用于接收管理员的审核操作数据
 */
public class OutboundRequestReviewDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否通过审核
     * true: 通过
     * false: 拒绝
     */
    private boolean approved;
    
    /**
     * 审核意见/拒绝原因
     * 拒绝时必须填写
     */
    private String comment;

    public OutboundRequestReviewDto() {
    }

    public OutboundRequestReviewDto(boolean approved, String comment) {
        this.approved = approved;
        this.comment = comment;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
