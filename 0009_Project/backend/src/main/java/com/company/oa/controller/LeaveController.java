package com.company.oa.controller;

import com.company.oa.common.Result;
import com.company.oa.dto.LeaveApplyDTO;
import com.company.oa.dto.LeaveApprovalDTO;
import com.company.oa.model.LeaveApplication;
import com.company.oa.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @GetMapping
    public Result<List<LeaveApplication>> list() {
        List<LeaveApplication> leaves = leaveService.findAll();
        return Result.success(leaves);
    }

    @GetMapping("/{id}")
    public Result<LeaveApplication> getById(@PathVariable Long id) {
        return leaveService.findById(id)
                .map(Result::success)
                .orElse(Result.error("请假申请不存在"));
    }

    @GetMapping("/employee/{employeeId}")
    public Result<List<LeaveApplication>> getByEmployeeId(@PathVariable Long employeeId) {
        List<LeaveApplication> leaves = leaveService.findByEmployeeId(employeeId);
        return Result.success(leaves);
    }

    @GetMapping("/pending")
    public Result<List<LeaveApplication>> getPending() {
        List<LeaveApplication> leaves = leaveService.findPending();
        return Result.success(leaves);
    }

    @GetMapping("/approved")
    public Result<List<LeaveApplication>> getApproved() {
        List<LeaveApplication> leaves = leaveService.findApproved();
        return Result.success(leaves);
    }

    @GetMapping("/rejected")
    public Result<List<LeaveApplication>> getRejected() {
        List<LeaveApplication> leaves = leaveService.findRejected();
        return Result.success(leaves);
    }

    @PostMapping("/apply")
    public Result<LeaveApplication> apply(@RequestBody LeaveApplyDTO dto) {
        try {
            LeaveApplication leave = leaveService.applyLeave(dto);
            return Result.success(leave);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/approve")
    public Result<LeaveApplication> approve(@RequestBody LeaveApprovalDTO dto) {
        try {
            LeaveApplication leave = leaveService.approveLeave(dto);
            return Result.success(leave);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}