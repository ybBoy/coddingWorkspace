package com.company.ot.service;

import com.company.ot.dto.EmployeeStats;
import com.company.ot.model.Department;
import com.company.ot.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private UserService userService;

    public List<EmployeeStats> getDepartmentStats(Department department) {
        List<User> users = userService.getUsersByDepartment(department);
        return users.stream()
                .map(u -> new EmployeeStats(
                        u.getId(),
                        u.getName(),
                        u.getDepartment(),
                        u.getTotalOvertimeHours(),
                        u.getUsedTimeoffHours()
                ))
                .collect(Collectors.toList());
    }

    public EmployeeStats getUserStats(Long userId) {
        User u = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return new EmployeeStats(
                u.getId(),
                u.getName(),
                u.getDepartment(),
                u.getTotalOvertimeHours(),
                u.getUsedTimeoffHours()
        );
    }
}
