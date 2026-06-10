package com.company.ot.controller;

import com.company.ot.dto.ApiResponse;
import com.company.ot.dto.ApprovalDTO;
import com.company.ot.dto.TimeoffRequestDTO;
import com.company.ot.model.Department;
import com.company.ot.model.Role;
import com.company.ot.model.TimeoffRequest;
import com.company.ot.model.User;
import com.company.ot.service.TimeoffService;
import com.company.ot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/timeoff")
public class TimeoffController {

    @Autowired
    private TimeoffService timeoffService;

    @Autowired
    private UserService userService;

    @GetMapping("/my/{userId}")
    public ApiResponse<List<TimeoffRequest>> getMyRequests(@PathVariable Long userId) {
        return ApiResponse.success(timeoffService.getRequestsByUser(userId));
    }

    @GetMapping("/pending/{approverId}")
    public ApiResponse<List<TimeoffRequest>> getPendingRequests(@PathVariable Long approverId) {
        User approver = userService.getUserById(approverId).orElse(null);
        if (approver == null || approver.getRole() != Role.MANAGER) {
            return ApiResponse.error("无权限访问");
        }
        Department dept = approver.getDepartment();
        return ApiResponse.success(timeoffService.getPendingRequestsByDepartment(dept));
    }

    @PostMapping
    public ApiResponse<TimeoffRequest> createRequest(@RequestBody TimeoffRequestDTO dto) {
        try {
            TimeoffRequest request = timeoffService.createRequest(dto);
            return ApiResponse.success("调休申请提交成功", request);
        } catch (RuntimeException | IOException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/approve")
    public ApiResponse<TimeoffRequest> approveRequest(@RequestBody ApprovalDTO dto) {
        try {
            User approver = userService.getUserById(dto.getApproverId()).orElse(null);
            if (approver == null || approver.getRole() != Role.MANAGER) {
                return ApiResponse.error("无权限审批");
            }
            TimeoffRequest request = timeoffService.approveRequest(dto.getRequestId(), dto.getApproverId());
            return ApiResponse.success("审批通过", request);
        } catch (RuntimeException | IOException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/reject")
    public ApiResponse<TimeoffRequest> rejectRequest(@RequestBody ApprovalDTO dto) {
        try {
            User approver = userService.getUserById(dto.getApproverId()).orElse(null);
            if (approver == null || approver.getRole() != Role.MANAGER) {
                return ApiResponse.error("无权限审批");
            }
            TimeoffRequest request = timeoffService.rejectRequest(
                    dto.getRequestId(), dto.getApproverId(), dto.getComment());
            return ApiResponse.success("已拒绝", request);
        } catch (RuntimeException | IOException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
