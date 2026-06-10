package com.team.taskmanager.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team.taskmanager.enums.Priority;
import com.team.taskmanager.enums.TaskStatus;

import java.time.LocalDate;

public class Task {
    private Long id;
    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    private Priority priority;
    private TaskStatus status;
    private Long assigneeId;
    private String assigneeName;

    public Task() {}

    public Task(Long id, String title, LocalDate dueDate, Priority priority,
                TaskStatus status, Long assigneeId, String assigneeName) {
        this.id = id;
        this.title = title;
        this.dueDate = dueDate;
        this.priority = priority;
        this.status = status;
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }
}
