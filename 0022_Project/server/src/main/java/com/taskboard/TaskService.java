package com.taskboard;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TaskService {
    private final ConcurrentHashMap<String, Task> tasks = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<TaskLog> logs = new CopyOnWriteArrayList<>();
    private final List<TaskWebSocket> connections = new ArrayList<>();

    public ConcurrentHashMap<String, Task> getTasks() {
        return tasks;
    }

    public CopyOnWriteArrayList<TaskLog> getLogs() {
        return logs;
    }

    public synchronized void addConnection(TaskWebSocket ws) {
        connections.add(ws);
    }

    public synchronized void removeConnection(TaskWebSocket ws) {
        connections.remove(ws);
    }

    public Task addTask(Task task) {
        if (task.getId() == null || task.getId().isEmpty()) {
            task.setId(UUID.randomUUID().toString().substring(0, 8));
        }
        task.setCreatedAt(System.currentTimeMillis());
        task.setStatus("pending");
        tasks.put(task.getId(), task);
        addLog("created", task.getTitle(), "");
        return task;
    }

    public Task claimTask(String taskId, String nickname) {
        Task task = tasks.get(taskId);
        if (task == null) return null;
        if (task.getAssignee() != null && !task.getAssignee().isEmpty()) return null;
        task.setAssignee(nickname);
        task.setStatus("in_progress");
        task.setClaimedAt(System.currentTimeMillis());
        addLog("claimed", task.getTitle(), nickname);
        return task;
    }

    public Task releaseTask(String taskId, String nickname) {
        Task task = tasks.get(taskId);
        if (task == null) return null;
        if (!nickname.equals(task.getAssignee())) return null;
        task.setAssignee(null);
        task.setStatus("pending");
        task.setClaimedAt(0);
        addLog("released", task.getTitle(), nickname);
        return task;
    }

    public Task completeTask(String taskId, String nickname) {
        Task task = tasks.get(taskId);
        if (task == null) return null;
        if (!nickname.equals(task.getAssignee())) return null;
        task.setStatus("completed");
        addLog("completed", task.getTitle(), nickname);
        return task;
    }

    private void addLog(String action, String taskTitle, String nickname) {
        TaskLog log = new TaskLog();
        log.setId(UUID.randomUUID().toString().substring(0, 8));
        log.setAction(action);
        log.setTaskTitle(taskTitle);
        log.setNickname(nickname);
        log.setTimestamp(System.currentTimeMillis());
        logs.add(0, log);
        if (logs.size() > 50) {
            logs.remove(logs.size() - 1);
        }
    }

    public List<TaskLog> getRecentLogs() {
        if (logs.size() <= 10) {
            return new ArrayList<TaskLog>(logs);
        }
        return new ArrayList<TaskLog>(logs.subList(0, 10));
    }

    public List<Task> getAllTasks() {
        return new ArrayList<Task>(tasks.values());
    }

    public synchronized void broadcast(String message) {
        List<TaskWebSocket> toRemove = new ArrayList<>();
        for (TaskWebSocket ws : connections) {
            try {
                ws.send(message);
            } catch (Exception e) {
                toRemove.add(ws);
            }
        }
        connections.removeAll(toRemove);
    }
}
