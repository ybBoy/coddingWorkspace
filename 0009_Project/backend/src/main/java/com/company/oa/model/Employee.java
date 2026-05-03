package com.company.oa.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Employee {
    private Long id;
    private String username;
    private String password;
    
    private String name;
    private String avatar;
    private Long departmentId;
    private String departmentName;
    private Long roleId;
    private String roleName;
    private EmployeeStatus status;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}