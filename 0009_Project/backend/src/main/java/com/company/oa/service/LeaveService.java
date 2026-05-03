package com.company.oa.service;

import com.company.oa.dto.LeaveApplyDTO;
import com.company.oa.dto.LeaveApprovalDTO;
import com.company.oa.model.*;
import com.company.oa.repository.LeaveApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class LeaveService {

    @Autowired
    private LeaveApplicationRepository leaveRepository;

    @Autowired
    private EmployeeService employeeService;

    public List<LeaveApplication> findAll() {
        return leaveRepository.findAll();
    }

    public Optional<LeaveApplication> findById(Long id) {
        return leaveRepository.findById(id);
    }

    public List<LeaveApplication> findByEmployeeId(Long employeeId) {
        return leaveRepository.findByEmployeeId(employeeId);
    }

    public List<LeaveApplication> findPending() {
        return leaveRepository.findPending();
    }

    public List<LeaveApplication> findApproved() {
        return leaveRepository.findApproved();
    }

    public List<LeaveApplication> findRejected() {
        return leaveRepository.findRejected();
    }

    public LeaveApplication applyLeave(LeaveApplyDTO dto) {
        Optional<Employee> employeeOpt = employeeService.findById(dto.getEmployeeId());
        if (!employeeOpt.isPresent()) {
            throw new RuntimeException("员工不存在");
        }

        Employee employee = employeeOpt.get();

        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new RuntimeException("开始日期不能晚于结束日期");
        }

        if (dto.getStartDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("开始日期不能早于今天");
        }

        LeaveApplication leave = new LeaveApplication();
        leave.setEmployeeId(dto.getEmployeeId());
        leave.setEmployeeName(employee.getName());
        leave.setDepartmentName(employee.getDepartmentName());
        leave.setStartDate(dto.getStartDate());
        leave.setEndDate(dto.getEndDate());
        leave.setReason(dto.getReason());
        leave.setStatus(LeaveStatus.PENDING);

        LeaveApplication saved = leaveRepository.save(leave);

        employeeService.updateStatus(dto.getEmployeeId(), EmployeeStatus.ON_LEAVE);

        return saved;
    }

    public LeaveApplication approveLeave(LeaveApprovalDTO dto) {
        Optional<LeaveApplication> leaveOpt = leaveRepository.findById(dto.getLeaveId());
        if (!leaveOpt.isPresent()) {
            throw new RuntimeException("请假申请不存在");
        }

        LeaveApplication leave = leaveOpt.get();

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new RuntimeException("该申请已被处理");
        }

        Optional<Employee> approverOpt = employeeService.findById(dto.getApproverId());
        if (approverOpt.isPresent()) {
            leave.setApproverId(dto.getApproverId());
            leave.setApproverName(approverOpt.get().getName());
        }

        leave.setApprovalComment(dto.getApprovalComment());

        if (dto.getApproved()) {
            leave.setStatus(LeaveStatus.APPROVED);
        } else {
            leave.setStatus(LeaveStatus.REJECTED);
            employeeService.updateStatus(leave.getEmployeeId(), EmployeeStatus.NORMAL);
        }

        return leaveRepository.save(leave);
    }

    public void updateEmployeeStatusBasedOnLeave(Long employeeId) {
        List<LeaveApplication> leaves = leaveRepository.findByEmployeeIdAndStatus(employeeId, LeaveStatus.APPROVED);
        LocalDate today = LocalDate.now();
        
        boolean onLeave = leaves.stream()
                .anyMatch(leave -> 
                    !leave.getStartDate().isAfter(today) && !leave.getEndDate().isBefore(today)
                );

        if (onLeave) {
            employeeService.updateStatus(employeeId, EmployeeStatus.ON_LEAVE);
        }
    }
}