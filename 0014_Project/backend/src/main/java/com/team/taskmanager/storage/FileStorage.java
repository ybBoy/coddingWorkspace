package com.team.taskmanager.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.taskmanager.enums.Priority;
import com.team.taskmanager.enums.Role;
import com.team.taskmanager.enums.TaskStatus;
import com.team.taskmanager.model.Task;
import com.team.taskmanager.model.User;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class FileStorage {

    private static final String DATA_DIR = "data";
    private static final String USERS_FILE = "data/users.json";
    private static final String TASKS_FILE = "data/tasks.json";

    private final ObjectMapper objectMapper;
    private List<User> users = new ArrayList<>();
    private List<Task> tasks = new ArrayList<>();
    private final AtomicLong taskIdGenerator = new AtomicLong(0);

    public FileStorage() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostConstruct
    public synchronized void init() {
        try {
            Path dataPath = Paths.get(DATA_DIR);
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
            }

            File usersFile = new File(USERS_FILE);
            if (!usersFile.exists()) {
                initDefaultUsers();
                saveUsers();
            } else {
                loadUsers();
            }

            File tasksFile = new File(TASKS_FILE);
            if (!tasksFile.exists()) {
                initDefaultTasks();
                saveTasks();
            } else {
                loadTasks();
            }
        } catch (IOException e) {
            throw new RuntimeException("初始化数据失败", e);
        }
    }

    private void initDefaultUsers() {
        users = new ArrayList<>();
        users.add(new User(1L, "张伟", Role.LEADER, "#3B82F6"));
        users.add(new User(2L, "李娜", Role.MEMBER, "#10B981"));
        users.add(new User(3L, "王强", Role.MEMBER, "#F59E0B"));
        users.add(new User(4L, "刘敏", Role.MEMBER, "#EF4444"));
    }

    private void initDefaultTasks() {
        tasks = new ArrayList<>();
        taskIdGenerator.set(0);
        addTask(new Task(null, "完成项目需求文档", LocalDate.now().plusDays(3),
                Priority.HIGH, TaskStatus.IN_PROGRESS, 2L, "李娜"));
        addTask(new Task(null, "设计数据库表结构", LocalDate.now().plusDays(5),
                Priority.HIGH, TaskStatus.PENDING, 3L, "王强"));
        addTask(new Task(null, "编写接口测试用例", LocalDate.now().plusDays(7),
                Priority.MEDIUM, TaskStatus.PENDING, 4L, "刘敏"));
        addTask(new Task(null, "整理会议纪要", LocalDate.now().minusDays(1),
                Priority.LOW, TaskStatus.COMPLETED, 2L, "李娜"));
        addTask(new Task(null, "代码评审", LocalDate.now().plusDays(2),
                Priority.MEDIUM, TaskStatus.PENDING, 3L, "王强"));
    }

    private void addTask(Task task) {
        long id = taskIdGenerator.incrementAndGet();
        task.setId(id);
        tasks.add(task);
    }

    public synchronized void loadUsers() throws IOException {
        File file = new File(USERS_FILE);
        if (file.length() == 0) {
            initDefaultUsers();
            return;
        }
        users = objectMapper.readValue(file, new TypeReference<List<User>>() {});
    }

    public synchronized void saveUsers() throws IOException {
        objectMapper.writeValue(new File(USERS_FILE), users);
    }

    public synchronized void loadTasks() throws IOException {
        File file = new File(TASKS_FILE);
        if (file.length() == 0) {
            initDefaultTasks();
            return;
        }
        tasks = objectMapper.readValue(file, new TypeReference<List<Task>>() {});
        long maxId = 0;
        for (Task t : tasks) {
            if (t.getId() != null && t.getId() > maxId) {
                maxId = t.getId();
            }
        }
        taskIdGenerator.set(maxId);
    }

    public synchronized void saveTasks() throws IOException {
        objectMapper.writeValue(new File(TASKS_FILE), tasks);
    }

    public synchronized List<User> getUsers() {
        return new ArrayList<>(users);
    }

    public synchronized User getUserById(Long id) {
        for (User u : users) {
            if (u.getId().equals(id)) {
                return u;
            }
        }
        return null;
    }

    public synchronized List<Task> getTasks() {
        return new ArrayList<>(tasks);
    }

    public synchronized Task getTaskById(Long id) {
        for (Task t : tasks) {
            if (t.getId().equals(id)) {
                return t;
            }
        }
        return null;
    }

    public synchronized Task createTask(Task task) throws IOException {
        long id = taskIdGenerator.incrementAndGet();
        task.setId(id);
        tasks.add(task);
        saveTasks();
        return task;
    }

    public synchronized Task updateTask(Long id, Task updated) throws IOException {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(id)) {
                updated.setId(id);
                tasks.set(i, updated);
                saveTasks();
                return updated;
            }
        }
        return null;
    }

    public synchronized boolean deleteTask(Long id) throws IOException {
        boolean removed = tasks.removeIf(t -> t.getId().equals(id));
        if (removed) {
            saveTasks();
        }
        return removed;
    }
}
