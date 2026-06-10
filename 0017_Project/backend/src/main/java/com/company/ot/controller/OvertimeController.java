package com.company.ot.controller;

import com.company.ot.dto.ApiResponse;
import com.company.ot.dto.ApprovalDTO;
import com.company.ot.dto.OvertimeRequestDTO;
import com.company.ot.model.Department;
import com.company.ot.model.OvertimeRequest;
import com.company.ot.model.Role;
import com.company.ot.model.User;
import com.company.ot.service.OvertimeService;
import com.company.ot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/overtime")
public class OvertimeController {

    @Autowired
    private OvertimeService overtimeService;

    @Autowired
    private UserService userService;

    @GetMapping("/my/{userId}")
    public ApiResponse<List<OvertimeRequest>> getMyRequests(@PathVariable Long userId) {
        return ApiResponse.success(overtimeService.getRequestsByUser(userId));
    }

    @GetMapping("/pending/{approverId}")
    public ApiResponse<List<OvertimeRequest>> getPendingRequests(@PathVariable Long approverId) {
        User approver = userService.getUserById(approverId).orElse(null);
        if (approver == null || approver.getRole() != Role.MANAGER) {
            return ApiResponse.error("无权限访问");
        }
        Department dept = approver.getDepartment();
        return ApiResponse.success(overtimeService.getPendingRequestsByDepartment(dept));
    }

    @PostMapping
    public ApiResponse<OvertimeRequest> createRequest(@RequestBody OvertimeRequestDTO dto) {
        try {
            OvertimeRequest request = overtimeService.createRequest(dto);
            return ApiResponse.success("加班申请提交成功", request);
        } catch (RuntimeException | IOException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/approve")
    public ApiResponse<OvertimeRequest> approveRequest(@RequestBody ApprovalDTO dto) {
        try {
            User approver = userService.getUserById(dto.getApproverId()).orElse(null);
            if (approver == null || approver.getRole() != Role.MANAGER) {
                return ApiResponse.error("无权限审批");
            }
            OvertimeRequest request = overtimeService.approveRequest(dto.getRequestId(), dto.getApproverId());
            return ApiResponse.success("审批通过", request);
        } catch (RuntimeException | IOException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/reject")
    public ApiResponse<OvertimeRequest> rejectRequest(@RequestBody ApprovalDTO dto) {
        try {
            User approver = userService.getUserById(dto.getApproverId()).orElse(null);
            if (approver == null || approver.getRole() != Role.MANAGER) {
                return ApiResponse.error("无权限审批");
            }
            OvertimeRequest request = overtimeService.rejectRequest(
                    dto.getRequestId(), dto.getApproverId(), dto.getComment());
            return ApiResponse.success("已拒绝", request);
        } catch (RuntimeException | IOException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
