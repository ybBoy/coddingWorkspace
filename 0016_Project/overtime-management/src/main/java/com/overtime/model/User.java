package com.overtime.model;

public class User {
    private String id;
    private String name;
    private String department;
    private String role;
    private double totalOvertimeHours;
    private double usedTimeoffHours;

    public User() {}

    public double getRemainingHours() {
        return totalOvertimeHours - usedTimeoffHours;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public double getTotalOvertimeHours() { return totalOvertimeHours; }
    public void setTotalOvertimeHours(double totalOvertimeHours) { this.totalOvertimeHours = totalOvertimeHours; }
    public double getUsedTimeoffHours() { return usedTimeoffHours; }
    public void setUsedTimeoffHours(double usedTimeoffHours) { this.usedTimeoffHours = usedTimeoffHours; }
}
