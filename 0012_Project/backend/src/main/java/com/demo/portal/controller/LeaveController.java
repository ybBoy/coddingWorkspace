package com.demo.portal.controller;

import com.demo.portal.model.LeaveRequest;
import com.demo.portal.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    @Autowired
    private DataService dataService;

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitLeave(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        String leaveType = body.get("leaveType");
        String startDate = body.get("startDate");
        String endDate = body.get("endDate");
        String reason = body.get("reason");

        Map<String, Object> result = new HashMap<>();

        if (userId == null || leaveType == null || startDate == null || endDate == null || reason == null) {
            result.put("success", false);
            result.put("message", "请填写完整请假信息");
            return ResponseEntity.badRequest().body(result);
        }

        if (dataService.hasActiveLeave(userId)) {
            result.put("success", false);
            result.put("message", "您已有正在进行的请假申请，不可重复请假");
            return ResponseEntity.badRequest().body(result);
        }

        LeaveRequest request = dataService.submitLeaveRequest(userId, leaveType, startDate, endDate, reason);
        if (request == null) {
            result.put("success", false);
            result.put("message", "提交失败，用户不存在");
            return ResponseEntity.badRequest().body(result);
        }

        result.put("success", true);
        result.put("message", "请假申请已提交");
        result.put("data", request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/my/{userId}")
    public ResponseEntity<List<LeaveRequest>> getMyLeaves(@PathVariable String userId) {
        return ResponseEntity.ok(dataService.getLeaveRequestsByUser(userId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<LeaveRequest>> getAllLeaves() {
        dataService.checkAndRestoreLeaveStatus();
        return ResponseEntity.ok(dataService.getAllLeaveRequests());
    }

    @PostMapping("/approve/{id}")
    public ResponseEntity<Map<String, Object>> approveLeave(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String comment = body.get("comment");
        Map<String, Object> result = new HashMap<>();
        LeaveRequest request = dataService.approveLeaveRequest(id, comment);
        if (request == null) {
            result.put("success", false);
            result.put("message", "审批失败，申请不存在或已处理");
            return ResponseEntity.badRequest().body(result);
        }
        result.put("success", true);
        result.put("message", "已通过审批");
        result.put("data", request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reject/{id}")
    public ResponseEntity<Map<String, Object>> rejectLeave(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String comment = body.get("comment");
        Map<String, Object> result = new HashMap<>();
        if (comment == null || comment.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "拒绝申请时必须填写审批意见");
            return ResponseEntity.badRequest().body(result);
        }
        LeaveRequest request = dataService.rejectLeaveRequest(id, comment);
        if (request == null) {
            result.put("success", false);
            result.put("message", "审批失败，申请不存在或已处理");
            return ResponseEntity.badRequest().body(result);
        }
        result.put("success", true);
        result.put("message", "已拒绝申请");
        result.put("data", request);
        return ResponseEntity.ok(result);
    }
}
