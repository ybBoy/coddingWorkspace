package com.company.ot.controller;

import com.company.ot.dto.ApiResponse;
import com.company.ot.model.User;
import com.company.ot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ApiResponse<List<User>> getAllUsers() {
        return ApiResponse.success(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ApiResponse<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("用户不存在"));
    }

    @GetMapping("/{id}/isManager")
    public ApiResponse<Boolean> isManager(@PathVariable Long id) {
        return ApiResponse.success(userService.isManager(id));
    }
}
