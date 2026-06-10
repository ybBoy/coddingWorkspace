package com.company.ot.controller;

import com.company.ot.dto.ApiResponse;
import com.company.ot.dto.EmployeeStats;
import com.company.ot.model.Role;
import com.company.ot.model.User;
import com.company.ot.service.ReportService;
import com.company.ot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserService userService;

    @GetMapping("/my/{userId}")
    public ApiResponse<EmployeeStats> getMyStats(@PathVariable Long userId) {
        try {
            return ApiResponse.success(reportService.getUserStats(userId));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/department/{managerId}")
    public ApiResponse<List<EmployeeStats>> getDepartmentStats(@PathVariable Long managerId) {
        User manager = userService.getUserById(managerId).orElse(null);
        if (manager == null || manager.getRole() != Role.MANAGER) {
            return ApiResponse.error("无权限访问");
        }
        return ApiResponse.success(reportService.getDepartmentStats(manager.getDepartment()));
    }
}
