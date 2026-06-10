package com.team.taskmanager.model;

import com.team.taskmanager.enums.Role;

public class User {
    private Long id;
    private String name;
    private Role role;
    private String avatarColor;

    public User() {}

    public User(Long id, String name, Role role, String avatarColor) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.avatarColor = avatarColor;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getAvatarColor() { return avatarColor; }
    public void setAvatarColor(String avatarColor) { this.avatarColor = avatarColor; }
}
