package com.overtime.controller;

import com.alibaba.fastjson.JSONObject;
import com.overtime.model.User;
import com.overtime.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable String id) {
        return userService.getUserById(id);
    }

    @GetMapping("/department/{dept}")
    public List<User> getByDepartment(@PathVariable String dept) {
        return userService.getUsersByDepartment(dept);
    }

    @GetMapping("/{id}/dashboard")
    public Map<String, Object> getDashboard(@PathVariable String id) {
        User user = userService.getUserById(id);
        Map<String, Object> result = new HashMap<>();
        if (user != null) {
            result.put("id", user.getId());
            result.put("name", user.getName());
            result.put("department", user.getDepartment());
            result.put("role", user.getRole());
            result.put("totalOvertimeHours", user.getTotalOvertimeHours());
            result.put("usedTimeoffHours", user.getUsedTimeoffHours());
            result.put("remainingHours", user.getTotalOvertimeHours() - user.getUsedTimeoffHours());
        }
        return result;
    }

    @GetMapping("/department/{dept}/report")
    public List<Map<String, Object>> getDeptReport(@PathVariable String dept) {
        List<User> users = userService.getUsersByDepartment(dept);
        List<Map<String, Object>> report = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", user.getId());
            item.put("name", user.getName());
            item.put("role", user.getRole());
            item.put("totalOvertimeHours", user.getTotalOvertimeHours());
            item.put("usedTimeoffHours", user.getUsedTimeoffHours());
            item.put("remainingHours", user.getTotalOvertimeHours() - user.getUsedTimeoffHours());
            report.add(item);
        }
        return report;
    }
}
