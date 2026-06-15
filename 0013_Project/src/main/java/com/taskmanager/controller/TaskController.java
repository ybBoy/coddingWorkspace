package com.taskmanager.controller;

import com.taskmanager.model.Task;
import com.taskmanager.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping
    public Map<String, Object> getTasks(
            @RequestParam String memberId,
            @RequestParam(required = false) String assigneeId,
            @RequestParam(required = false) String status) {
        List<Task> result;
        if (assigneeId != null && !assigneeId.isEmpty() && taskService.isLeader(memberId)) {
            result = taskService.getTasksByAssignee(assigneeId);
        } else {
            result = taskService.getTasks(memberId);
        }
        if (status != null && !status.isEmpty()) {
            if ("未完成".equals(status)) {
                result.removeIf(t -> "已完成".equals(t.getStatus()));
            } else {
                String finalStatus = status;
                result.removeIf(t -> !finalStatus.equals(t.getStatus()));
            }
        }
        Map<String, Object> response = new HashMap<>();
        response.put("tasks", result);
        response.put("isLeader", taskService.isLeader(memberId));
        return response;
    }

    @PostMapping
    public Task createTask(@RequestBody Task task) {
        return taskService.createTask(task);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable String id,
            @RequestBody Task task,
            @RequestParam String memberId) {
        Task existing = taskService.getTaskById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        if (!taskService.isLeader(memberId) && !existing.getAssigneeId().equals(memberId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Task updated = taskService.updateTask(id, task);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTask(
            @PathVariable String id,
            @RequestParam String memberId) {
        Map<String, Object> response = new HashMap<>();
        if (!taskService.isLeader(memberId)) {
            response.put("success", false);
            response.put("message", "仅组长可删除任务");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        boolean deleted = taskService.deleteTask(id);
        response.put("success", deleted);
        return ResponseEntity.ok(response);
    }
}
