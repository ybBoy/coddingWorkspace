package com.company.oa.controller;

import com.company.oa.common.Result;
import com.company.oa.model.Department;
import com.company.oa.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @GetMapping
    public Result<List<Department>> list() {
        List<Department> departments = departmentService.findAll();
        return Result.success(departments);
    }

    @GetMapping("/{id}")
    public Result<Department> getById(@PathVariable Long id) {
        return departmentService.findById(id)
                .map(Result::success)
                .orElse(Result.error("部门不存在"));
    }

    @PostMapping
    public Result<Department> create(@RequestBody Department department) {
        Department saved = departmentService.save(department);
        return Result.success(saved);
    }

    @PutMapping("/{id}")
    public Result<Department> update(@PathVariable Long id, @RequestBody Department department) {
        return departmentService.findById(id)
                .map(existing -> {
                    department.setId(id);
                    Department updated = departmentService.save(department);
                    return Result.success(updated);
                })
                .orElse(Result.error("部门不存在"));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean deleted = departmentService.deleteById(id);
        if (deleted) {
            return Result.success();
        }
        return Result.error("部门不存在");
    }
}