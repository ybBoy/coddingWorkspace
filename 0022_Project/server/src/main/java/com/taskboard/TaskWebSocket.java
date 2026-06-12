package com.taskboard;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.websocket.WsContext;
import java.util.*;

public class TaskWebSocket {
    private static final Gson gson = new Gson();
    private final TaskService taskService;
    private WsContext ctx;

    public TaskWebSocket(TaskService taskService) {
        this.taskService = taskService;
    }

    public void onConnect(WsContext ctx) {
        this.ctx = ctx;
        taskService.addConnection(this);
        sendInitialState();
    }

    public void onClose(WsContext ctx) {
        taskService.removeConnection(this);
    }

    public void onMessage(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String action = json.get("action").getAsString();

            switch (action) {
                case "addTask": {
                    Task task = gson.fromJson(json.get("task"), Task.class);
                    taskService.addTask(task);
                    broadcastUpdate();
                    break;
                }
                case "claimTask": {
                    String taskId = json.get("taskId").getAsString();
                    String nickname = json.get("nickname").getAsString();
                    Task result = taskService.claimTask(taskId, nickname);
                    if (result != null) {
                        broadcastUpdate();
                    }
                    break;
                }
                case "releaseTask": {
                    String taskId = json.get("taskId").getAsString();
                    String nickname = json.get("nickname").getAsString();
                    Task result = taskService.releaseTask(taskId, nickname);
                    if (result != null) {
                        broadcastUpdate();
                    }
                    break;
                }
                case "completeTask": {
                    String taskId = json.get("taskId").getAsString();
                    String nickname = json.get("nickname").getAsString();
                    Task result = taskService.completeTask(taskId, nickname);
                    if (result != null) {
                        broadcastUpdate();
                    }
                    break;
                }
                default:
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }

    private void sendInitialState() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "init");
        data.put("tasks", taskService.getAllTasks());
        data.put("logs", taskService.getRecentLogs());
        send(gson.toJson(data));
    }

    private void broadcastUpdate() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "update");
        data.put("tasks", taskService.getAllTasks());
        data.put("logs", taskService.getRecentLogs());
        taskService.broadcast(gson.toJson(data));
    }

    public void send(String message) {
        if (ctx != null) {
            try {
                ctx.send(message);
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
