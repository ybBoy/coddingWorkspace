package com.demo.portal.service;

import com.demo.portal.model.LeaveRequest;
import com.demo.portal.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class DataService {

    @Value("${app.data.dir:./data}")
    private String dataDir;

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<User> users = new ArrayList<>();
    private final List<LeaveRequest> leaveRequests = new ArrayList<>();
    private final AtomicLong leaveIdCounter = new AtomicLong(0);

    @PostConstruct
    public void init() {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        loadUsers();
        loadLeaveRequests();
        recoverUserStatus();
    }

    private File getUsersFile() {
        return new File(dataDir, "users.json");
    }

    private File getLeaveRequestsFile() {
        return new File(dataDir, "leave_requests.json");
    }

    private void loadUsers() {
        try {
            File file = getUsersFile();
            if (file.exists()) {
                List<User> loaded = mapper.readValue(file, new TypeReference<List<User>>() {});
                users.clear();
                users.addAll(loaded);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadLeaveRequests() {
        try {
            File file = getLeaveRequestsFile();
            if (file.exists()) {
                List<LeaveRequest> loaded = mapper.readValue(file, new TypeReference<List<LeaveRequest>>() {});
                leaveRequests.clear();
                leaveRequests.addAll(loaded);
                long maxId = leaveRequests.stream()
                        .mapToLong(r -> {
                            try { return Long.parseLong(r.getId().replace("LR", "")); }
                            catch (Exception e) { return 0; }
                        })
                        .max().orElse(0);
                leaveIdCounter.set(maxId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveUsers() {
        try {
            mapper.writeValue(getUsersFile(), users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveLeaveRequests() {
        try {
            mapper.writeValue(getLeaveRequestsFile(), leaveRequests);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void recoverUserStatus() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (User user : users) {
            if ("请假中".equals(user.getStatus())) {
                boolean hasActiveLeave = leaveRequests.stream()
                        .anyMatch(r -> r.getUserId().equals(user.getId())
                                && ("待审批".equals(r.getStatus()) || "已通过".equals(r.getStatus()))
                                && !today.isAfter(LocalDate.parse(r.getEndDate(), fmt)));
                if (!hasActiveLeave) {
                    user.setStatus("正常在岗");
                }
            }
        }
        saveUsers();
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    public User getUserById(String id) {
        return users.stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null);
    }

    public void updateUserStatus(String userId, String status) {
        User user = getUserById(userId);
        if (user != null) {
            user.setStatus(status);
            saveUsers();
        }
    }

    public boolean hasActiveLeave(String userId) {
        return leaveRequests.stream()
                .anyMatch(r -> r.getUserId().equals(userId)
                        && ("待审批".equals(r.getStatus()) || "已通过".equals(r.getStatus())));
    }

    public LeaveRequest submitLeaveRequest(String userId, String leaveType, String startDate,
                                            String endDate, String reason) {
        User user = getUserById(userId);
        if (user == null) return null;

        if (hasActiveLeave(userId)) return null;

        LeaveRequest request = new LeaveRequest();
        long newId = leaveIdCounter.incrementAndGet();
        request.setId("LR" + String.format("%04d", newId));
        request.setUserId(userId);
        request.setUserName(user.getName());
        request.setDepartment(user.getDepartment());
        request.setLeaveType(leaveType);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setReason(reason);
        request.setStatus("待审批");
        request.setApproverComment("");
        request.setCreateTime(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        leaveRequests.add(request);
        user.setStatus("请假中");
        saveLeaveRequests();
        saveUsers();
        return request;
    }

    public List<LeaveRequest> getLeaveRequestsByUser(String userId) {
        return leaveRequests.stream()
                .filter(r -> r.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<LeaveRequest> getAllLeaveRequests() {
        return new ArrayList<>(leaveRequests);
    }

    public LeaveRequest approveLeaveRequest(String requestId, String comment) {
        LeaveRequest request = leaveRequests.stream()
                .filter(r -> r.getId().equals(requestId))
                .findFirst().orElse(null);
        if (request == null || !"待审批".equals(request.getStatus())) return null;

        request.setStatus("已通过");
        request.setApproverComment(comment != null ? comment : "");
        saveLeaveRequests();
        return request;
    }

    public LeaveRequest rejectLeaveRequest(String requestId, String comment) {
        LeaveRequest request = leaveRequests.stream()
                .filter(r -> r.getId().equals(requestId))
                .findFirst().orElse(null);
        if (request == null || !"待审批".equals(request.getStatus())) return null;

        request.setStatus("已拒绝");
        request.setApproverComment(comment != null ? comment : "");
        updateUserStatus(request.getUserId(), "正常在岗");
        saveLeaveRequests();
        return request;
    }

    public void checkAndRestoreLeaveStatus() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        boolean changed = false;
        for (User user : users) {
            if (!"请假中".equals(user.getStatus())) continue;
            boolean hasActiveLeave = leaveRequests.stream()
                    .anyMatch(r -> r.getUserId().equals(user.getId())
                            && "已通过".equals(r.getStatus())
                            && !today.isAfter(LocalDate.parse(r.getEndDate(), fmt)));
            if (!hasActiveLeave) {
                boolean hasPending = leaveRequests.stream()
                        .anyMatch(r -> r.getUserId().equals(user.getId())
                                && "待审批".equals(r.getStatus()));
                if (!hasPending) {
                    user.setStatus("正常在岗");
                    changed = true;
                }
            }
        }
        if (changed) saveUsers();
    }
}
