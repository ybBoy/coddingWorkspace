package com.demo.portal.controller;

import com.demo.portal.model.User;
import com.demo.portal.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private DataService dataService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(dataService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserInfo(@PathVariable String id) {
        dataService.checkAndRestoreLeaveStatus();
        User user = dataService.getUserById(id);
        if (user == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "用户不存在");
            return ResponseEntity.status(404).body(err);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("name", user.getName());
        result.put("department", user.getDepartment());
        result.put("role", user.getRole());
        result.put("status", user.getStatus());
        return ResponseEntity.ok(result);
    }
}
