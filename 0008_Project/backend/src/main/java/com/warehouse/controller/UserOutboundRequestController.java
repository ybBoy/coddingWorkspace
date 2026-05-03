package com.warehouse.controller;

import com.warehouse.dto.ApiResponse;
import com.warehouse.dto.OutboundRequestSubmitDto;
import com.warehouse.entity.OutboundRequest;
import com.warehouse.entity.RequestType;
import com.warehouse.service.OutboundRequestService;
import com.warehouse.util.IpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户申请控制器
 * 提供用户提交出库申请和退货入库申请、查看申请等接口
 */
@RestController
@RequestMapping("/api/user/requests")
public class UserOutboundRequestController {

    @Autowired
    private OutboundRequestService requestService;

    /**
     * 提交出库申请
     * 
     * 用户选择需要的零件和数量，提交出库申请
     * 申请状态设置为"待审核"，等待管理员处理
     * 
     * @param submitDto 申请提交数据
     * @param request HTTP请求对象（用于获取客户端IP）
     * @return 创建的申请对象
     */
    @PostMapping
    public ApiResponse<OutboundRequest> submitRequest(
            @RequestBody OutboundRequestSubmitDto submitDto,
            HttpServletRequest request) {
        try {
            String applicantIp = IpUtils.getClientIpAddress(request);
            OutboundRequest result = requestService.submitRequest(submitDto, applicantIp);
            return ApiResponse.success("申请提交成功", result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 提交退货入库申请
     * 
     * 用户选择需要归还的零件和数量，提交退货入库申请
     * 申请状态设置为"待审核"，等待管理员处理
     * 
     * @param submitDto 申请提交数据
     * @param request HTTP请求对象（用于获取客户端IP）
     * @return 创建的申请对象
     */
    @PostMapping("/return")
    public ApiResponse<OutboundRequest> submitReturnRequest(
            @RequestBody OutboundRequestSubmitDto submitDto,
            HttpServletRequest request) {
        try {
            String applicantIp = IpUtils.getClientIpAddress(request);
            OutboundRequest result = requestService.submitReturnRequest(submitDto, applicantIp);
            return ApiResponse.success("退货入库申请提交成功", result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取我的申请列表
     * 根据用户IP地址获取该用户的所有申请记录
     * 支持按类型和状态筛选
     * 
     * @param type 可选的类型筛选参数（出库申请/退货入库申请）
     * @param status 可选的状态筛选参数
     * @param request HTTP请求对象（用于获取客户端IP）
     * @return 申请列表
     */
    @GetMapping
    public ApiResponse<List<OutboundRequest>> getMyRequests(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        String applicantIp = IpUtils.getClientIpAddress(request);
        List<OutboundRequest> requests;
        
        if (type != null && !type.trim().isEmpty()) {
            requests = requestService.getRequestsByApplicantIpAndTypeAndStatus(applicantIp, type, status);
        } else if (status != null && !status.trim().isEmpty()) {
            requests = requestService.getRequestsByApplicantIpAndStatus(applicantIp, status);
        } else {
            requests = requestService.getRequestsByApplicantIp(applicantIp);
        }
        
        return ApiResponse.success(requests);
    }

    /**
     * 获取我的申请详情
     * 根据申请编号获取申请详情，确保是该用户提交的申请
     * 
     * @param id 申请编号
     * @param request HTTP请求对象
     * @return 申请详情
     */
    @GetMapping("/{id}")
    public ApiResponse<OutboundRequest> getMyRequestById(
            @PathVariable String id,
            HttpServletRequest request) {
        OutboundRequest req = requestService.getRequestById(id);
        
        if (req == null) {
            return ApiResponse.error("申请不存在: " + id);
        }
        
        String applicantIp = IpUtils.getClientIpAddress(request);
        if (!applicantIp.equals(req.getApplicantIp())) {
            return ApiResponse.error("无权查看该申请");
        }
        
        return ApiResponse.success(req);
    }

    /**
     * 获取待审核的申请数量
     * 用于用户端显示待处理的申请数量
     * 
     * @param request HTTP请求对象
     * @return 待审核申请数量
     */
    @GetMapping("/pending/count")
    public ApiResponse<Long> getMyPendingCount(HttpServletRequest request) {
        String applicantIp = IpUtils.getClientIpAddress(request);
        List<OutboundRequest> requests = requestService.getRequestsByApplicantIp(applicantIp);
        long count = requests.stream()
                .filter(OutboundRequest::isPending)
                .count();
        return ApiResponse.success(count);
    }
}
