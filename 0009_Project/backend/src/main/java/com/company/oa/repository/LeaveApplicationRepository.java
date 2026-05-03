package com.company.oa.repository;

import com.company.oa.model.LeaveApplication;
import com.company.oa.model.LeaveStatus;
import com.company.oa.storage.JsonFileStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class LeaveApplicationRepository {

    private static final String FILE_NAME = "leaves.json";

    @Autowired
    private JsonFileStorage jsonFileStorage;

    public List<LeaveApplication> findAll() {
        return jsonFileStorage.load(FILE_NAME, LeaveApplication.class);
    }

    public Optional<LeaveApplication> findById(Long id) {
        return findAll().stream()
                .filter(leave -> leave.getId().equals(id))
                .findFirst();
    }

    public List<LeaveApplication> findByEmployeeId(Long employeeId) {
        return findAll().stream()
                .filter(leave -> leave.getEmployeeId().equals(employeeId))
                .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                .collect(Collectors.toList());
    }

    public List<LeaveApplication> findByStatus(LeaveStatus status) {
        return findAll().stream()
                .filter(leave -> leave.getStatus() == status)
                .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                .collect(Collectors.toList());
    }

    public List<LeaveApplication> findPending() {
        return findByStatus(LeaveStatus.PENDING);
    }

    public List<LeaveApplication> findApproved() {
        return findByStatus(LeaveStatus.APPROVED);
    }

    public List<LeaveApplication> findRejected() {
        return findByStatus(LeaveStatus.REJECTED);
    }

    public List<LeaveApplication> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status) {
        return findAll().stream()
                .filter(leave -> leave.getEmployeeId().equals(employeeId))
                .filter(leave -> leave.getStatus() == status)
                .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                .collect(Collectors.toList());
    }

    public LeaveApplication save(LeaveApplication leave) {
        List<LeaveApplication> leaves = findAll();
        
        if (leave.getId() == null) {
            long maxId = leaves.stream()
                    .mapToLong(LeaveApplication::getId)
                    .max()
                    .orElse(0L);
            leave.setId(maxId + 1);
            leave.setCreateTime(LocalDateTime.now());
            leave.setUpdateTime(LocalDateTime.now());
            leaves.add(leave);
        } else {
            for (int i = 0; i < leaves.size(); i++) {
                if (leaves.get(i).getId().equals(leave.getId())) {
                    leave.setUpdateTime(LocalDateTime.now());
                    leaves.set(i, leave);
                    break;
                }
            }
        }
        
        jsonFileStorage.save(FILE_NAME, leaves, LeaveApplication.class);
        return leave;
    }

    public void deleteById(Long id) {
        List<LeaveApplication> leaves = findAll();
        leaves.removeIf(leave -> leave.getId().equals(id));
        jsonFileStorage.save(FILE_NAME, leaves, LeaveApplication.class);
    }
}