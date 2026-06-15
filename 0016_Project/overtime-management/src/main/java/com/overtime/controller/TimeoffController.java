package com.overtime.controller;

import com.alibaba.fastjson.JSONObject;
import com.overtime.model.TimeoffRequest;
import com.overtime.model.User;
import com.overtime.service.TimeoffService;
import com.overtime.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/timeoff")
public class TimeoffController {

    @Autowired
    private TimeoffService timeoffService;

    @Autowired
    private UserService userService;

    @PostMapping("/submit")
    public Map<String, Object> submit(@RequestBody TimeoffRequest req) {
        Map<String, Object> result = new HashMap<>();
        String error = timeoffService.submitRequest(req);
        if (error != null) {
            result.put("success", false);
            result.put("message", error);
        } else {
            result.put("success", true);
            result.put("message", "调休申请提交成功");
        }
        return result;
    }

    @GetMapping("/my/{userId}")
    public List<Map<String, Object>> getMyRequests(@PathVariable String userId) {
        List<TimeoffRequest> requests = timeoffService.getRequestsByUser(userId);
        User user = userService.getUserById(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (TimeoffRequest req : requests) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", req.getId());
            item.put("userId", req.getUserId());
            item.put("userName", user != null ? user.getName() : "");
            item.put("date", req.getDate());
            item.put("type", req.getType());
            item.put("reason", req.getReason());
            item.put("hours", req.getHours());
            item.put("status", req.getStatus());
            item.put("rejectReason", req.getRejectReason());
            item.put("createTime", req.getCreateTime());
            result.add(item);
        }
        return result;
    }

    @GetMapping("/pending/{department}")
    public List<Map<String, Object>> getPendingByDept(@PathVariable String department) {
        List<TimeoffRequest> requests = timeoffService.getPendingByDepartment(department);
        List<Map<String, Object>> result = new ArrayList<>();
        for (TimeoffRequest req : requests) {
            User user = userService.getUserById(req.getUserId());
            Map<String, Object> item = new HashMap<>();
            item.put("id", req.getId());
            item.put("userId", req.getUserId());
            item.put("userName", user != null ? user.getName() : "");
            item.put("date", req.getDate());
            item.put("type", req.getType());
            item.put("reason", req.getReason());
            item.put("hours", req.getHours());
            item.put("status", req.getStatus());
            item.put("createTime", req.getCreateTime());
            if (user != null) {
                item.put("userTotalOvertimeHours", user.getTotalOvertimeHours());
                item.put("userUsedTimeoffHours", user.getUsedTimeoffHours());
                item.put("userRemainingHours", user.getTotalOvertimeHours() - user.getUsedTimeoffHours());
            }
            result.add(item);
        }
        return result;
    }

    @PostMapping("/approve/{id}")
    public Map<String, Object> approve(@PathVariable String id, @RequestBody JSONObject body) {
        Map<String, Object> result = new HashMap<>();
        String approverId = body.getString("approverId");
        String error = timeoffService.approve(id, approverId);
        if (error != null) {
            result.put("success", false);
            result.put("message", error);
        } else {
            result.put("success", true);
            result.put("message", "审批通过");
        }
        return result;
    }

    @PostMapping("/reject/{id}")
    public Map<String, Object> reject(@PathVariable String id, @RequestBody JSONObject body) {
        Map<String, Object> result = new HashMap<>();
        String approverId = body.getString("approverId");
        String rejectReason = body.getString("rejectReason");
        String error = timeoffService.reject(id, approverId, rejectReason);
        if (error != null) {
            result.put("success", false);
            result.put("message", error);
        } else {
            result.put("success", true);
            result.put("message", "已拒绝");
        }
        return result;
    }
}
