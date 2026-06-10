package com.team.taskmanager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team.taskmanager.enums.Priority;

import java.time.LocalDate;

public class TaskCreateRequest {
    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    private Priority priority;

    private Long assigneeId;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
}
