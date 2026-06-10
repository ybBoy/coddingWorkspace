package com.team.taskmanager.controller;

import com.team.taskmanager.dto.TaskCreateRequest;
import com.team.taskmanager.dto.TaskUpdateRequest;
import com.team.taskmanager.enums.Role;
import com.team.taskmanager.enums.TaskStatus;
import com.team.taskmanager.model.Task;
import com.team.taskmanager.model.User;
import com.team.taskmanager.storage.FileStorage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final FileStorage storage;

    public TaskController(FileStorage storage) {
        this.storage = storage;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getTasks(
            @RequestParam Long userId,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(required = false) Long assigneeFilter) {

        Map<String, Object> result = new HashMap<>();
        User currentUser = storage.getUserById(userId);
        if (currentUser == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return ResponseEntity.badRequest().body(result);
        }

        List<Task> allTasks = storage.getTasks();
        List<Task> filtered;

        if (currentUser.getRole() == Role.LEADER) {
            if (assigneeFilter != null) {
                filtered = allTasks.stream()
                        .filter(t -> t.getAssigneeId().equals(assigneeFilter))
                        .collect(Collectors.toList());
            } else {
                filtered = new ArrayList<>(allTasks);
            }
        } else {
            filtered = allTasks.stream()
                    .filter(t -> t.getAssigneeId().equals(userId))
                    .collect(Collectors.toList());
        }

        if (statusFilter != null && !statusFilter.equals("ALL") && !statusFilter.equals("ALL")) {
            if ("UNCOMPLETED".equals(statusFilter)) {
                filtered = filtered.stream()
                        .filter(t -> t.getStatus() != TaskStatus.COMPLETED)
                        .collect(Collectors.toList());
            } else if ("COMPLETED".equals(statusFilter)) {
                filtered = filtered.stream()
                        .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                        .collect(Collectors.toList());
            } else {
                try {
                    TaskStatus ts = TaskStatus.valueOf(statusFilter);
                    filtered = filtered.stream()
                            .filter(t -> t.getStatus() == ts)
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        result.put("success", true);
        result.put("data", filtered);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createTask(@RequestBody TaskCreateRequest req,
                                                  @RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        User currentUser = storage.getUserById(userId);
        if (currentUser == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return ResponseEntity.badRequest().body(result);
        }

        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "标题不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        if (req.getDueDate() == null) {
            result.put("success", false);
            result.put("message", "截止日期不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        if (req.getPriority() == null) {
            result.put("success", false);
            result.put("message", "优先级不能为空");
            return ResponseEntity.badRequest().body(result);
        }

        Long assigneeId;
        String assigneeName;
        if (currentUser.getRole() == Role.LEADER && req.getAssigneeId() != null) {
            User assignee = storage.getUserById(req.getAssigneeId());
            if (assignee == null) {
                result.put("success", false);
                result.put("message", "负责人不存在");
                return ResponseEntity.badRequest().body(result);
            }
            assigneeId = assignee.getId();
            assigneeName = assignee.getName();
        } else {
            assigneeId = currentUser.getId();
            assigneeName = currentUser.getName();
        }

        Task task = new Task(null, req.getTitle(), req.getDueDate(),
                req.getPriority(), TaskStatus.PENDING, assigneeId, assigneeName);

        try {
            Task created = storage.createTask(task);
            result.put("success", true);
            result.put("data", created);
            result.put("message", "任务创建成功");
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "保存失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateTask(@PathVariable Long id,
                                                @RequestBody TaskUpdateRequest req,
                                                @RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        User currentUser = storage.getUserById(userId);
        if (currentUser == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return ResponseEntity.badRequest().body(result);
        }

        Task existing = storage.getTaskById(id);
        if (existing == null) {
            result.put("success", false);
            result.put("message", "任务不存在");
            return ResponseEntity.status(404).body(result);
        }

        boolean isLeader = currentUser.getRole() == Role.LEADER;
        boolean isOwner = existing.getAssigneeId().equals(userId);

        if (!isLeader && !isOwner) {
            result.put("success", false);
            result.put("message", "无权限修改此任务");
            return ResponseEntity.status(403).body(result);
        }

        Task updated = new Task();
        updated.setTitle(req.getTitle() != null ? req.getTitle() : existing.getTitle());
        updated.setDueDate(req.getDueDate() != null ? req.getDueDate() : existing.getDueDate());
        updated.setPriority(req.getPriority() != null ? req.getPriority() : existing.getPriority());

        if (isLeader) {
            updated.setStatus(req.getStatus() != null ? req.getStatus() : existing.getStatus());
            if (req.getAssigneeId() != null) {
                User assignee = storage.getUserById(req.getAssigneeId());
                if (assignee == null) {
                    result.put("success", false);
                    result.put("message", "新负责人不存在");
                    return ResponseEntity.badRequest().body(result);
                }
                updated.setAssigneeId(assignee.getId());
                updated.setAssigneeName(assignee.getName());
            } else {
                updated.setAssigneeId(existing.getAssigneeId());
                updated.setAssigneeName(existing.getAssigneeName());
            }
        } else {
            if (req.getStatus() != null) {
                updated.setStatus(req.getStatus());
            } else {
                updated.setStatus(existing.getStatus());
            }
            updated.setAssigneeId(existing.getAssigneeId());
            updated.setAssigneeName(existing.getAssigneeName());
        }

        if (!isLeader) {
            updated.setTitle(existing.getTitle());
            updated.setDueDate(existing.getDueDate());
            updated.setPriority(existing.getPriority());
        }

        try {
            Task saved = storage.updateTask(id, updated);
            result.put("success", true);
            result.put("data", saved);
            result.put("message", "更新成功");
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "保存失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable Long id,
                                                 @RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        User currentUser = storage.getUserById(userId);
        if (currentUser == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return ResponseEntity.badRequest().body(result);
        }

        if (currentUser.getRole() != Role.LEADER) {
            result.put("success", false);
            result.put("message", "无权限删除任务");
            return ResponseEntity.status(403).body(result);
        }

        Task existing = storage.getTaskById(id);
        if (existing == null) {
            result.put("success", false);
            result.put("message", "任务不存在");
            return ResponseEntity.status(404).body(result);
        }

        try {
            storage.deleteTask(id);
            result.put("success", true);
            result.put("message", "删除成功");
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}
