package com.company.ot.service;

import com.company.ot.dto.TimeoffRequestDTO;
import com.company.ot.model.*;
import com.company.ot.storage.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TimeoffService {

    @Autowired
    private DataStore dataStore;

    @Autowired
    private UserService userService;

    public List<TimeoffRequest> getAllRequests() {
        return dataStore.getTimeoffRequests().stream()
                .sorted(Comparator.comparing(TimeoffRequest::getCreateTime).reversed())
                .collect(Collectors.toList());
    }

    public List<TimeoffRequest> getRequestsByUser(Long userId) {
        return dataStore.getTimeoffRequests().stream()
                .filter(r -> r.getUserId().equals(userId))
                .sorted(Comparator.comparing(TimeoffRequest::getCreateTime).reversed())
                .collect(Collectors.toList());
    }

    public List<TimeoffRequest> getPendingRequestsByDepartment(Department department) {
        return dataStore.getTimeoffRequests().stream()
                .filter(r -> r.getDepartment() == department && r.getStatus() == RequestStatus.PENDING)
                .sorted(Comparator.comparing(TimeoffRequest::getCreateTime))
                .collect(Collectors.toList());
    }

    public Optional<TimeoffRequest> getRequestById(Long id) {
        return dataStore.getTimeoffRequests().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst();
    }

    public TimeoffRequest createRequest(TimeoffRequestDTO dto) throws IOException {
        User user = userService.getUserById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (dto.getTimeoffDate() == null) {
            throw new RuntimeException("请选择调休日期");
        }
        if (dto.getTimeoffDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("调休日期不能是过去日期");
        }
        if (dto.getTimeoffType() == null) {
            throw new RuntimeException("请选择调休类型");
        }
        if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            throw new RuntimeException("请填写调休事由");
        }

        double hours = dto.getTimeoffType().getHours();

        boolean exists = dataStore.getTimeoffRequests().stream()
                .anyMatch(r -> r.getUserId().equals(dto.getUserId())
                        && r.getTimeoffDate().equals(dto.getTimeoffDate())
                        && (r.getStatus() == RequestStatus.PENDING || r.getStatus() == RequestStatus.APPROVED));
        if (exists) {
            throw new RuntimeException("该日期已存在待审批或已通过的调休申请");
        }

        TimeoffRequest request = new TimeoffRequest();
        request.setId(dataStore.nextTimeoffId());
        request.setUserId(user.getId());
        request.setUserName(user.getName());
        request.setDepartment(user.getDepartment());
        request.setTimeoffDate(dto.getTimeoffDate());
        request.setTimeoffType(dto.getTimeoffType());
        request.setHours(hours);
        request.setReason(dto.getReason());
        request.setStatus(RequestStatus.PENDING);
        request.setCreateTime(LocalDate.now());

        dataStore.getTimeoffRequests().add(request);
        dataStore.saveTimeoffRequests();
        return request;
    }

    public TimeoffRequest approveRequest(Long requestId, Long approverId) throws IOException {
        TimeoffRequest request = getRequestById(requestId)
                .orElseThrow(() -> new RuntimeException("申请不存在"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("该申请已处理");
        }

        if (!userService.hasEnoughTimeoff(request.getUserId(), request.getHours())) {
            throw new RuntimeException("用户剩余调休时长不足，无法通过该申请");
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setApprovalComment("已通过");
        userService.addUsedTimeoffHours(request.getUserId(), request.getHours());

        dataStore.saveTimeoffRequests();
        return request;
    }

    public TimeoffRequest rejectRequest(Long requestId, Long approverId, String comment) throws IOException {
        TimeoffRequest request = getRequestById(requestId)
                .orElseThrow(() -> new RuntimeException("申请不存在"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("该申请已处理");
        }
        if (comment == null || comment.trim().isEmpty()) {
            throw new RuntimeException("拒绝申请必须填写审批意见");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setApprovalComment(comment);

        dataStore.saveTimeoffRequests();
        return request;
    }
}
