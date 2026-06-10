package com.meeting.controller;

import com.meeting.common.Result;
import com.meeting.model.User;
import com.meeting.service.DataStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private DataStoreService dataStoreService;

    @GetMapping
    public Result<List<User>> getAllUsers() {
        return Result.success(dataStoreService.getUsers());
    }

    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable String id) {
        User user = dataStoreService.getUserById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    @GetMapping("/current")
    public Result<User> getCurrentUser(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null || userId.isEmpty()) {
            userId = "u1";
        }
        User user = dataStoreService.getUserById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }
}
