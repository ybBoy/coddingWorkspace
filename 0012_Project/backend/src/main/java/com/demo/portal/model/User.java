package com.demo.portal.model;

public class User {
    private String id;
    private String name;
    private String department;
    private String role;
    private String status;

    public User() {}

    public User(String id, String name, String department, String role, String status) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.role = role;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
