package com.warehouse.controller;

import com.warehouse.dto.ApiResponse;
import com.warehouse.dto.OutboundRequestReviewDto;
import com.warehouse.entity.OutboundRequest;
import com.warehouse.entity.RequestType;
import com.warehouse.service.OutboundRequestService;
import com.warehouse.util.IpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 管理员申请控制器
 * 提供管理员查看所有申请（出库申请和退货入库申请）、审核申请等接口
 */
@RestController
@RequestMapping("/api/admin/requests")
public class AdminOutboundRequestController {

    @Autowired
    private OutboundRequestService requestService;

    /**
     * 获取所有申请列表
     * 管理员可查看所有申请（出库申请和退货入库申请）
     * 支持按类型和状态筛选
     * 
     * @param type 可选的类型筛选参数（出库申请/退货入库申请）
     * @param status 可选的状态筛选参数
     * @return 申请列表
     */
    @GetMapping
    public ApiResponse<List<OutboundRequest>> getAllRequests(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        List<OutboundRequest> requests;
        
        if (type != null && !type.trim().isEmpty()) {
            requests = requestService.getRequestsByTypeAndStatus(type, status);
        } else if (status != null && !status.trim().isEmpty()) {
            requests = requestService.getRequestsByStatus(status);
        } else {
            requests = requestService.getAllRequests();
        }
        
        return ApiResponse.success(requests);
    }

    /**
     * 获取待审核申请列表
     * 专门用于管理员查看待处理的申请
     * 
     * @return 待审核申请列表
     */
    @GetMapping("/pending")
    public ApiResponse<List<OutboundRequest>> getPendingRequests() {
        return ApiResponse.success(requestService.getPendingRequests());
    }

    /**
     * 获取申请详情
     * 
     * @param id 申请编号
     * @return 申请详情
     */
    @GetMapping("/{id}")
    public ApiResponse<OutboundRequest> getRequestById(@PathVariable String id) {
        OutboundRequest request = requestService.getRequestById(id);
        
        if (request == null) {
            return ApiResponse.error("申请不存在: " + id);
        }
        
        return ApiResponse.success(request);
    }

    /**
     * 审核申请（支持出库申请和退货入库申请）
     * 
     * 管理员审核用户提交的申请：
     * - 对于出库申请，通过时：验证库存后自动执行出库操作
     * - 对于退货入库申请，通过时：自动执行入库操作
     * - 拒绝：记录拒绝原因
     * 
     * @param id 申请编号
     * @param reviewDto 审核数据（是否通过、审核意见）
     * @param request HTTP请求对象（用于获取审核人IP）
     * @return 审核后的申请对象
     */
    @PutMapping("/{id}/review")
    public ApiResponse<OutboundRequest> reviewRequest(
            @PathVariable String id,
            @RequestBody OutboundRequestReviewDto reviewDto,
            HttpServletRequest request) {
        try {
            String reviewerIp = IpUtils.getClientIpAddress(request);
            OutboundRequest result = requestService.reviewRequest(
                    id, 
                    reviewDto.isApproved(), 
                    reviewDto.getComment(), 
                    reviewerIp
            );
            
            String message;
            if (reviewDto.isApproved()) {
                if (result.isOutbound()) {
                    message = "审核通过，已完成出库";
                } else {
                    message = "审核通过，已完成退货入库";
                }
            } else {
                message = "申请已拒绝";
            }
            return ApiResponse.success(message, result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 批量审核通过申请
     * 批量操作多个待审核申请
     * 
     * @param ids 申请编号数组
     * @param request HTTP请求对象
     * @return 操作结果
     */
    @PostMapping("/batch-approve")
    public ApiResponse<Integer> batchApprove(
            @RequestBody List<String> ids,
            HttpServletRequest request) {
        String reviewerIp = IpUtils.getClientIpAddress(request);
        int successCount = 0;
        
        for (String id : ids) {
            try {
                requestService.reviewRequest(id, true, "批量审核通过", reviewerIp);
                successCount++;
            } catch (Exception e) {
                // 单个失败不影响其他
                continue;
            }
        }
        
        return ApiResponse.success("批量审核完成，成功: " + successCount + "/" + ids.size(), successCount);
    }

    /**
     * 获取待审核申请数量
     * 用于管理员菜单显示角标
     * 
     * @return 待审核申请数量
     */
    @GetMapping("/pending/count")
    public ApiResponse<Integer> getPendingCount() {
        return ApiResponse.success(requestService.getPendingRequestCount());
    }
}
