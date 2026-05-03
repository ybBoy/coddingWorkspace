package com.company.oa.controller;

import com.company.oa.common.Result;
import com.company.oa.model.Role;
import com.company.oa.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping
    public Result<List<Role>> list() {
        List<Role> roles = roleService.findAll();
        return Result.success(roles);
    }

    @GetMapping("/{id}")
    public Result<Role> getById(@PathVariable Long id) {
        return roleService.findById(id)
                .map(Result::success)
                .orElse(Result.error("角色不存在"));
    }

    @PostMapping
    public Result<Role> create(@RequestBody Role role) {
        Role saved = roleService.save(role);
        return Result.success(saved);
    }

    @PutMapping("/{id}")
    public Result<Role> update(@PathVariable Long id, @RequestBody Role role) {
        return roleService.findById(id)
                .map(existing -> {
                    role.setId(id);
                    Role updated = roleService.save(role);
                    return Result.success(updated);
                })
                .orElse(Result.error("角色不存在"));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean deleted = roleService.deleteById(id);
        if (deleted) {
            return Result.success();
        }
        return Result.error("角色不存在");
    }
}