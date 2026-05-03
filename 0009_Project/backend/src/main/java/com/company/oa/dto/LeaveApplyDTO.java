package com.company.oa.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveApplyDTO {
    private Long employeeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}