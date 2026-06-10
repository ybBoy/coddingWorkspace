package com.company.ot.model;

public class User {
    private Long id;
    private String name;
    private Department department;
    private Role role;
    private double totalOvertimeHours;
    private double usedTimeoffHours;

    public User() {
    }

    public User(Long id, String name, Department department, Role role) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.role = role;
        this.totalOvertimeHours = 0;
        this.usedTimeoffHours = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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
        return totalOvertimeHours - usedTimeoffHours;
    }
}
