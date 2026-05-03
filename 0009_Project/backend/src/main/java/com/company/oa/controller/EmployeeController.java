package com.company.oa.controller;

import com.company.oa.common.Result;
import com.company.oa.model.Employee;
import com.company.oa.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping
    public Result<List<Employee>> list() {
        List<Employee> employees = employeeService.findAll();
        return Result.success(employees);
    }

    @GetMapping("/{id}")
    public Result<Employee> getById(@PathVariable Long id) {
        return employeeService.findById(id)
                .map(Result::success)
                .orElse(Result.error("员工不存在"));
    }

    @GetMapping("/department/{departmentId}")
    public Result<List<Employee>> getByDepartmentId(@PathVariable Long departmentId) {
        List<Employee> employees = employeeService.findByDepartmentId(departmentId);
        return Result.success(employees);
    }

    @PostMapping
    public Result<Employee> create(@RequestBody Employee employee) {
        Employee saved = employeeService.save(employee);
        return Result.success(saved);
    }

    @PutMapping("/{id}")
    public Result<Employee> update(@PathVariable Long id, @RequestBody Employee employee) {
        return employeeService.findById(id)
                .map(existing -> {
                    employee.setId(id);
                    if (employee.getPassword() == null || employee.getPassword().isEmpty()) {
                        employee.setPassword(existing.getPassword());
                    }
                    Employee updated = employeeService.save(employee);
                    return Result.success(updated);
                })
                .orElse(Result.error("员工不存在"));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean deleted = employeeService.deleteById(id);
        if (deleted) {
            return Result.success();
        }
        return Result.error("员工不存在");
    }
}