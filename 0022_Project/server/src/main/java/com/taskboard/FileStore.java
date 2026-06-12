package com.taskboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FileStore {
    private static final String TASKS_FILE = "tasks.json";
    private static final String LOGS_FILE = "logs.json";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final TaskService taskService;

    public FileStore(TaskService taskService) {
        this.taskService = taskService;
        loadFromDisk();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                saveToDisk();
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    private void loadFromDisk() {
        try {
            File tf = new File(TASKS_FILE);
            if (tf.exists()) {
                Reader r = new FileReader(tf);
                Type listType = new TypeToken<ArrayList<Task>>(){}.getType();
                List<Task> tasks = gson.fromJson(r, listType);
                r.close();
                if (tasks != null) {
                    for (Task t : tasks) {
                        taskService.getTasks().put(t.getId(), t);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load tasks: " + e.getMessage());
        }
        try {
            File lf = new File(LOGS_FILE);
            if (lf.exists()) {
                Reader r = new FileReader(lf);
                Type listType = new TypeToken<ArrayList<TaskLog>>(){}.getType();
                List<TaskLog> logs = gson.fromJson(r, listType);
                r.close();
                if (logs != null) {
                    for (TaskLog l : logs) {
                        taskService.getLogs().add(l);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load logs: " + e.getMessage());
        }
    }

    public void saveToDisk() {
        try {
            Writer w = new FileWriter(TASKS_FILE);
            gson.toJson(new ArrayList<Task>(taskService.getTasks().values()), w);
            w.close();
        } catch (Exception e) {
            System.err.println("Failed to save tasks: " + e.getMessage());
        }
        try {
            Writer w = new FileWriter(LOGS_FILE);
            gson.toJson(taskService.getLogs(), w);
            w.close();
        } catch (Exception e) {
            System.err.println("Failed to save logs: " + e.getMessage());
        }
    }

    public void shutdown() {
        saveToDisk();
        scheduler.shutdown();
    }
}
