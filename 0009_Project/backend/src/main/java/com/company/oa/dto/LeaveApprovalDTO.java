package com.company.oa.dto;

import lombok.Data;

@Data
public class LeaveApprovalDTO {
    private Long leaveId;
    private Long approverId;
    private String approvalComment;
    private Boolean approved;
}