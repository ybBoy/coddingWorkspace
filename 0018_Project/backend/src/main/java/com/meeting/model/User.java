package com.meeting.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class User {
    private String id;
    private String name;
    private String role;

    public User() {
    }

    public User(String id, String name, String role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }

    @JsonIgnore
    public boolean isAdmin() {
        return "admin".equals(role);
    }
}
