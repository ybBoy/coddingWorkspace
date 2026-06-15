package com.taskmanager.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.model.Member;
import com.taskmanager.model.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TaskService {

    @Value("${task.data.file}")
    private String dataFilePath;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<Task> tasks = new ArrayList<>();
    private final List<Member> members = new ArrayList<>();

    @PostConstruct
    public void init() {
        members.add(new Member("m1", "张伟", "leader", "Z"));
        members.add(new Member("m2", "李娜", "member", "L"));
        members.add(new Member("m3", "王强", "member", "W"));
        members.add(new Member("m4", "赵敏", "member", "M"));
        loadData();
    }

    private void loadData() {
        try {
            File file = new File(dataFilePath);
            if (file.exists()) {
                tasks = objectMapper.readValue(file, new TypeReference<List<Task>>() {});
            }
        } catch (IOException e) {
            tasks = new ArrayList<>();
        }
    }

    private synchronized void saveData() {
        try {
            File file = new File(dataFilePath);
            file.getParentFile().mkdirs();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, tasks);
        } catch (IOException e) {
            throw new RuntimeException("保存数据失败", e);
        }
    }

    public List<Member> getAllMembers() {
        return members;
    }

    public Member getMemberById(String id) {
        return members.stream()
                .filter(m -> m.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public boolean isLeader(String memberId) {
        Member m = getMemberById(memberId);
        return m != null && "leader".equals(m.getRole());
    }

    public List<Task> getTasks(String memberId) {
        if (isLeader(memberId)) {
            return tasks;
        }
        return tasks.stream()
                .filter(t -> t.getAssigneeId().equals(memberId))
                .collect(Collectors.toList());
    }

    public List<Task> getTasksByAssignee(String assigneeId) {
        return tasks.stream()
                .filter(t -> t.getAssigneeId().equals(assigneeId))
                .collect(Collectors.toList());
    }

    public Task createTask(Task task) {
        task.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        task.setCreatedAt(java.time.LocalDate.now().toString());
        if (task.getStatus() == null || task.getStatus().isEmpty()) {
            task.setStatus("未开始");
        }
        Member assignee = getMemberById(task.getAssigneeId());
        if (assignee != null) {
            task.setAssigneeName(assignee.getName());
        }
        tasks.add(task);
        saveData();
        return task;
    }

    public Task updateTask(String taskId, Task updated) {
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            if (t.getId().equals(taskId)) {
                if (updated.getTitle() != null) t.setTitle(updated.getTitle());
                if (updated.getDueDate() != null) t.setDueDate(updated.getDueDate());
                if (updated.getPriority() != null) t.setPriority(updated.getPriority());
                if (updated.getStatus() != null) t.setStatus(updated.getStatus());
                if (updated.getAssigneeId() != null) {
                    t.setAssigneeId(updated.getAssigneeId());
                    Member assignee = getMemberById(updated.getAssigneeId());
                    if (assignee != null) t.setAssigneeName(assignee.getName());
                }
                saveData();
                return t;
            }
        }
        return null;
    }

    public boolean deleteTask(String taskId) {
        boolean removed = tasks.removeIf(t -> t.getId().equals(taskId));
        if (removed) saveData();
        return removed;
    }

    public Task getTaskById(String taskId) {
        return tasks.stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElse(null);
    }
}
