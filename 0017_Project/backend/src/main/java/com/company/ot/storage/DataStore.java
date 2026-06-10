package com.company.ot.storage;

import com.company.ot.model.OvertimeRequest;
import com.company.ot.model.TimeoffRequest;
import com.company.ot.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DataStore {

    @Value("${data.file.path:./data}")
    private String dataPath;

    private ObjectMapper objectMapper;
    private List<User> users;
    private List<OvertimeRequest> overtimeRequests;
    private List<TimeoffRequest> timeoffRequests;

    private AtomicLong userIdGenerator = new AtomicLong(0);
    private AtomicLong overtimeIdGenerator = new AtomicLong(0);
    private AtomicLong timeoffIdGenerator = new AtomicLong(0);

    @PostConstruct
    public void init() throws IOException {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        File dir = new File(dataPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        loadUsers();
        loadOvertimeRequests();
        loadTimeoffRequests();
    }

    private void loadUsers() throws IOException {
        File file = new File(dataPath, "users.json");
        if (file.exists()) {
            users = objectMapper.readValue(file, new TypeReference<List<User>>() {});
        } else {
            users = new ArrayList<>();
            saveUsers();
        }
        for (User u : users) {
            if (u.getId() > userIdGenerator.get()) {
                userIdGenerator.set(u.getId());
            }
        }
    }

    private void loadOvertimeRequests() throws IOException {
        File file = new File(dataPath, "overtime_requests.json");
        if (file.exists()) {
            overtimeRequests = objectMapper.readValue(file, new TypeReference<List<OvertimeRequest>>() {});
        } else {
            overtimeRequests = new ArrayList<>();
            saveOvertimeRequests();
        }
        for (OvertimeRequest r : overtimeRequests) {
            if (r.getId() > overtimeIdGenerator.get()) {
                overtimeIdGenerator.set(r.getId());
            }
        }
    }

    private void loadTimeoffRequests() throws IOException {
        File file = new File(dataPath, "timeoff_requests.json");
        if (file.exists()) {
            timeoffRequests = objectMapper.readValue(file, new TypeReference<List<TimeoffRequest>>() {});
        } else {
            timeoffRequests = new ArrayList<>();
            saveTimeoffRequests();
        }
        for (TimeoffRequest r : timeoffRequests) {
            if (r.getId() > timeoffIdGenerator.get()) {
                timeoffIdGenerator.set(r.getId());
            }
        }
    }

    public synchronized void saveUsers() throws IOException {
        File file = new File(dataPath, "users.json");
        objectMapper.writeValue(file, users);
    }

    public synchronized void saveOvertimeRequests() throws IOException {
        File file = new File(dataPath, "overtime_requests.json");
        objectMapper.writeValue(file, overtimeRequests);
    }

    public synchronized void saveTimeoffRequests() throws IOException {
        File file = new File(dataPath, "timeoff_requests.json");
        objectMapper.writeValue(file, timeoffRequests);
    }

    public List<User> getUsers() {
        return users;
    }

    public List<OvertimeRequest> getOvertimeRequests() {
        return overtimeRequests;
    }

    public List<TimeoffRequest> getTimeoffRequests() {
        return timeoffRequests;
    }

    public long nextUserId() {
        return userIdGenerator.incrementAndGet();
    }

    public long nextOvertimeId() {
        return overtimeIdGenerator.incrementAndGet();
    }

    public long nextTimeoffId() {
        return timeoffIdGenerator.incrementAndGet();
    }
}
