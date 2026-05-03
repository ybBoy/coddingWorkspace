package com.warehouse.service;

import com.warehouse.dto.OutboundRequestItemDto;
import com.warehouse.dto.OutboundRequestSubmitDto;
import com.warehouse.entity.OutboundRequest;
import com.warehouse.entity.OutboundRequestItem;
import com.warehouse.entity.Part;
import com.warehouse.entity.RequestStatus;
import com.warehouse.store.OutboundRequestDataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 出库申请服务类
 * 提供出库申请的提交、审核、查询等核心业务逻辑
 * 
 * 业务流程：
 * 1. 用户提交申请 → 状态：待审核
 * 2. 管理员审核通过 → 自动执行出库 → 状态：已通过
 * 3. 管理员审核拒绝 → 状态：已拒绝
 */
@Service
public class OutboundRequestService {

    @Autowired
    private OutboundRequestDataStore requestDataStore;

    @Autowired
    private PartService partService;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 申请编号计数器
     * 用于生成自增的申请序号
     */
    private int requestCounter = 0;

    /**
     * 提交出库申请
     * 用户提交出库申请，系统验证数据后创建申请记录
     * 
     * @param submitDto 申请提交数据
     * @param applicantIp 申请人IP地址
     * @return 创建的申请对象
     * @throws IllegalArgumentException 数据验证失败时抛出
     */
    public OutboundRequest submitRequest(OutboundRequestSubmitDto submitDto, String applicantIp) {
        if (submitDto.getItems() == null || submitDto.getItems().isEmpty()) {
            throw new IllegalArgumentException("申请明细不能为空");
        }

        List<OutboundRequestItem> items = new ArrayList<>();
        
        for (OutboundRequestItemDto itemDto : submitDto.getItems()) {
            if (itemDto.getPartId() == null || itemDto.getPartId().trim().isEmpty()) {
                throw new IllegalArgumentException("零件编号不能为空");
            }
            if (itemDto.getQuantity() <= 0) {
                throw new IllegalArgumentException("申请数量必须大于0");
            }

            Part part = partService.getVisiblePartById(itemDto.getPartId());
            if (part == null) {
                throw new IllegalArgumentException("零件不存在或不可见: " + itemDto.getPartId());
            }

            OutboundRequestItem item = new OutboundRequestItem();
            item.setPartId(part.getId());
            item.setPartName(part.getName());
            item.setCategory(part.getCategory());
            item.setQuantity(itemDto.getQuantity());
            item.setUnit(part.getUnit());
            item.setCurrentStock(part.getQuantity());

            items.add(item);
        }

        OutboundRequest request = new OutboundRequest();
        request.setId(generateRequestId());
        request.setApplicantIp(applicantIp);
        request.setItems(items);
        request.setRemark(submitDto.getRemark());
        request.setStatus(RequestStatus.PENDING.getDescription());

        requestDataStore.saveRequest(request);
        fileStorageService.saveRequests();

        return request;
    }

    /**
     * 获取所有申请列表
     * @return 所有申请列表
     */
    public List<OutboundRequest> getAllRequests() {
        return requestDataStore.getAllRequests();
    }

    /**
     * 根据编号获取申请详情
     * @param id 申请编号
     * @return 申请对象，不存在则返回null
     */
    public OutboundRequest getRequestById(String id) {
        return requestDataStore.getRequestById(id);
    }

    /**
     * 获取待审核申请列表
     * @return 待审核申请列表
     */
    public List<OutboundRequest> getPendingRequests() {
        return requestDataStore.getPendingRequests();
    }

    /**
     * 根据申请人IP获取申请列表
     * @param applicantIp 申请人IP
     * @return 该申请人的所有申请列表
     */
    public List<OutboundRequest> getRequestsByApplicantIp(String applicantIp) {
        return requestDataStore.getRequestsByApplicantIp(applicantIp);
    }

    /**
     * 根据申请人IP和状态获取申请列表
     * @param applicantIp 申请人IP
     * @param status 状态
     * @return 匹配的申请列表
     */
    public List<OutboundRequest> getRequestsByApplicantIpAndStatus(String applicantIp, String status) {
        if (status == null || status.trim().isEmpty()) {
            return getRequestsByApplicantIp(applicantIp);
        }
        return requestDataStore.getRequestsByApplicantIpAndStatus(applicantIp, status);
    }

    /**
     * 审核出库申请
     * 
     * 审核通过时：
     * 1. 验证所有申请零件的库存是否充足
     * 2. 逐个执行出库操作
     * 3. 更新申请状态为已通过
     * 
     * 审核拒绝时：
     * 1. 记录拒绝原因
     * 2. 更新申请状态为已拒绝
     * 
     * @param requestId 申请编号
     * @param approved 是否通过
     * @param comment 审核意见/拒绝原因
     * @param reviewerIp 审核人IP
     * @return 审核后的申请对象
     * @throws IllegalArgumentException 审核失败时抛出
     */
    public OutboundRequest reviewRequest(String requestId, boolean approved, String comment, String reviewerIp) {
        OutboundRequest request = requestDataStore.getRequestById(requestId);
        
        if (request == null) {
            throw new IllegalArgumentException("申请不存在: " + requestId);
        }
        
        if (!request.isPending()) {
            throw new IllegalArgumentException("该申请已处理，无法重复审核");
        }

        if (approved) {
            for (OutboundRequestItem item : request.getItems()) {
                Part part = partService.getPartById(item.getPartId());
                if (part == null) {
                    throw new IllegalArgumentException("零件不存在: " + item.getPartId());
                }
                if (part.getQuantity() < item.getQuantity()) {
                    throw new IllegalArgumentException(
                        "零件库存不足: " + part.getName() + 
                        "，当前库存: " + part.getQuantity() + 
                        "，申请数量: " + item.getQuantity()
                    );
                }
            }

            for (OutboundRequestItem item : request.getItems()) {
                partService.stockOut(item.getPartId(), item.getQuantity(), reviewerIp);
            }

            request.setStatus(RequestStatus.APPROVED.getDescription());
        } else {
            if (comment == null || comment.trim().isEmpty()) {
                throw new IllegalArgumentException("拒绝申请时请填写拒绝原因");
            }
            request.setStatus(RequestStatus.REJECTED.getDescription());
        }

        request.setReviewComment(comment);
        request.setReviewerIp(reviewerIp);
        request.setReviewTime(new Date());

        requestDataStore.saveRequest(request);
        fileStorageService.saveRequests();

        return request;
    }

    /**
     * 获取待审核申请数量
     * 用于管理员首页显示待办数量
     * @return 待审核申请数量
     */
    public int getPendingRequestCount() {
        return requestDataStore.getPendingCount();
    }

    /**
     * 根据状态获取申请列表
     * @param status 状态
     * @return 匹配的申请列表
     */
    public List<OutboundRequest> getRequestsByStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return getAllRequests();
        }
        return requestDataStore.getRequestsByStatus(status);
    }

    /**
     * 生成唯一的申请编号
     * 格式：REQ-yyyyMMddHHmmss-0001
     * 使用synchronized保证线程安全
     * @return 生成的申请编号
     */
    private synchronized String generateRequestId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = sdf.format(new Date());
        requestCounter++;
        return "REQ-" + timestamp + "-" + String.format("%04d", requestCounter);
    }
}
