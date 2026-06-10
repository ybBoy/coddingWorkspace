package com.company.ot.service;

import com.company.ot.model.Department;
import com.company.ot.model.Role;
import com.company.ot.model.User;
import com.company.ot.storage.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private DataStore dataStore;

    public List<User> getAllUsers() {
        return dataStore.getUsers();
    }

    public List<User> getUsersByDepartment(Department department) {
        return dataStore.getUsers().stream()
                .filter(u -> u.getDepartment() == department)
                .collect(Collectors.toList());
    }

    public Optional<User> getUserById(Long id) {
        return dataStore.getUsers().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();
    }

    public User saveUser(User user) throws IOException {
        Optional<User> existing = getUserById(user.getId());
        if (existing.isPresent()) {
            User e = existing.get();
            e.setName(user.getName());
            e.setDepartment(user.getDepartment());
            e.setRole(user.getRole());
            e.setTotalOvertimeHours(user.getTotalOvertimeHours());
            e.setUsedTimeoffHours(user.getUsedTimeoffHours());
        } else {
            if (user.getId() == null) {
                user.setId(dataStore.nextUserId());
            }
            dataStore.getUsers().add(user);
        }
        dataStore.saveUsers();
        return user;
    }

    public void addOvertimeHours(Long userId, double hours) throws IOException {
        User user = getUserById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setTotalOvertimeHours(user.getTotalOvertimeHours() + hours);
        dataStore.saveUsers();
    }

    public void addUsedTimeoffHours(Long userId, double hours) throws IOException {
        User user = getUserById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setUsedTimeoffHours(user.getUsedTimeoffHours() + hours);
        dataStore.saveUsers();
    }

    public boolean hasEnoughTimeoff(Long userId, double hours) {
        User user = getUserById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        return user.getRemainingTimeoffHours() >= hours;
    }

    public boolean isManager(Long userId) {
        User user = getUserById(userId).orElse(null);
        return user != null && user.getRole() == Role.MANAGER;
    }
}
