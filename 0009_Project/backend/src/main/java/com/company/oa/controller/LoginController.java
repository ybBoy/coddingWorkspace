package com.company.oa.controller;

import com.company.oa.common.Result;
import com.company.oa.dto.LoginDTO;
import com.company.oa.model.Employee;
import com.company.oa.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired
    private EmployeeService employeeService;

    @PostMapping("/login")
    public Result<Employee> login(@RequestBody LoginDTO loginDTO) {
        if (loginDTO.getUsername() == null || loginDTO.getPassword() == null) {
            return Result.error("用户名和密码不能为空");
        }
        
        Optional<Employee> employee = employeeService.login(loginDTO);
        if (employee.isPresent()) {
            Employee emp = employee.get();
            emp.setPassword(null);
            return Result.success(emp);
        }
        return Result.error(401, "用户名或密码错误");
    }

    @GetMapping("/check-admin/{employeeId}")
    public Result<Boolean> checkAdmin(@PathVariable Long employeeId) {
        boolean isAdmin = employeeService.isAdmin(employeeId);
        return Result.success(isAdmin);
    }
}