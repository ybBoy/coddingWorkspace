package com.team.taskmanager.controller;

import com.team.taskmanager.model.User;
import com.team.taskmanager.storage.FileStorage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final FileStorage storage;

    public UserController(FileStorage storage) {
        this.storage = storage;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        List<User> users = storage.getUsers();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", users);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
        User user = storage.getUserById(id);
        Map<String, Object> result = new HashMap<>();
        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return ResponseEntity.status(404).body(result);
        }
        result.put("success", true);
        result.put("data", user);
        return ResponseEntity.ok(result);
    }
}
