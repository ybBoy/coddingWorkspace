package com.warehouse.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 申请实体类
 * 用于管理用户提交的出库申请和退货入库申请，以及管理员审核流程
 * 
 * 申请流程：
 * 1. 用户提交申请（状态：PENDING）
 * 2. 管理员审核通过（状态：APPROVED）→ 自动执行出库或入库
 * 3. 管理员审核拒绝（状态：REJECTED）→ 记录拒绝原因
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OutboundRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 申请编号，自动生成
     * 格式：REQ-yyyyMMddHHmmss-0001
     */
    private String id;
    
    /**
     * 申请类型
     * @see RequestType
     */
    private String type;
    
    /**
     * 申请人IP地址
     * 用于追踪申请人
     */
    private String applicantIp;
    
    /**
     * 申请明细列表
     * 支持一次申请多个零件
     */
    private List<OutboundRequestItem> items = new ArrayList<>();
    
    /**
     * 申请备注
     * 用户可填写申请原因或备注
     */
    private String remark;
    
    /**
     * 申请状态
     * @see RequestStatus
     */
    private String status;
    
    /**
     * 申请时间
     */
    private Date createTime;
    
    /**
     * 审核时间
     */
    private Date reviewTime;
    
    /**
     * 审核人IP地址
     */
    private String reviewerIp;
    
    /**
     * 审核意见/拒绝原因
     * 拒绝时必须填写
     */
    private String reviewComment;

    /**
     * 默认构造函数
     * 自动设置创建时间、初始状态为待审核、类型为出库申请
     */
    public OutboundRequest() {
        this.createTime = new Date();
        this.status = RequestStatus.PENDING.getDescription();
        this.type = RequestType.OUTBOUND.getDescription();
    }

    /**
     * 带参数的构造函数
     * @param id 申请编号
     * @param applicantIp 申请人IP
     * @param items 申请明细列表
     * @param remark 申请备注
     */
    public OutboundRequest(String id, String applicantIp, List<OutboundRequestItem> items, String remark) {
        this.id = id;
        this.applicantIp = applicantIp;
        this.items = items != null ? items : new ArrayList<>();
        this.remark = remark;
        this.createTime = new Date();
        this.status = RequestStatus.PENDING.getDescription();
    }

    /**
     * 判断申请是否处于待审核状态
     * @return true 表示待审核
     */
    public boolean isPending() {
        return RequestStatus.PENDING.getDescription().equals(this.status);
    }

    /**
     * 判断申请是否已通过
     * @return true 表示已通过
     */
    public boolean isApproved() {
        return RequestStatus.APPROVED.getDescription().equals(this.status);
    }

    /**
     * 判断申请是否已拒绝
     * @return true 表示已拒绝
     */
    public boolean isRejected() {
        return RequestStatus.REJECTED.getDescription().equals(this.status);
    }

    /**
     * 获取申请总数量
     * @return 所有明细项的数量之和
     */
    public int getTotalQuantity() {
        return items.stream().mapToInt(OutboundRequestItem::getQuantity).sum();
    }

    /**
     * 判断申请是否是出库申请
     * @return true 表示是出库申请
     */
    public boolean isOutbound() {
        return RequestType.OUTBOUND.getDescription().equals(this.type);
    }

    /**
     * 判断申请是否是退货入库申请
     * @return true 表示是退货入库申请
     */
    public boolean isReturn() {
        return RequestType.RETURN.getDescription().equals(this.type);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getApplicantIp() {
        return applicantIp;
    }

    public void setApplicantIp(String applicantIp) {
        this.applicantIp = applicantIp;
    }

    public List<OutboundRequestItem> getItems() {
        return items;
    }

    public void setItems(List<OutboundRequestItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getReviewTime() {
        return reviewTime;
    }

    public void setReviewTime(Date reviewTime) {
        this.reviewTime = reviewTime;
    }

    public String getReviewerIp() {
        return reviewerIp;
    }

    public void setReviewerIp(String reviewerIp) {
        this.reviewerIp = reviewerIp;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }
}
