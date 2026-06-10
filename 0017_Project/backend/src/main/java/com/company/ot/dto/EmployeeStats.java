package com.company.ot.dto;

import com.company.ot.model.Department;

public class EmployeeStats {
    private Long userId;
    private String userName;
    private Department department;
    private double totalOvertimeHours;
    private double usedTimeoffHours;
    private double remainingTimeoffHours;

    public EmployeeStats() {
    }

    public EmployeeStats(Long userId, String userName, Department department,
                         double totalOvertimeHours, double usedTimeoffHours) {
        this.userId = userId;
        this.userName = userName;
        this.department = department;
        this.totalOvertimeHours = totalOvertimeHours;
        this.usedTimeoffHours = usedTimeoffHours;
        this.remainingTimeoffHours = totalOvertimeHours - usedTimeoffHours;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public double getTotalOvertimeHours() {
        return totalOvertimeHours;
    }

    public void setTotalOvertimeHours(double totalOvertimeHours) {
        this.totalOvertimeHours = totalOvertimeHours;
    }

    public double getUsedTimeoffHours() {
        return usedTimeoffHours;
    }

    public void setUsedTimeoffHours(double usedTimeoffHours) {
        this.usedTimeoffHours = usedTimeoffHours;
    }

    public double getRemainingTimeoffHours() {
        return remainingTimeoffHours;
    }

    public void setRemainingTimeoffHours(double remainingTimeoffHours) {
        this.remainingTimeoffHours = remainingTimeoffHours;
    }
}
