package com.company.ot.service;

import com.company.ot.dto.OvertimeRequestDTO;
import com.company.ot.model.*;
import com.company.ot.storage.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OvertimeService {

    @Autowired
    private DataStore dataStore;

    @Autowired
    private UserService userService;

    public List<OvertimeRequest> getAllRequests() {
        return dataStore.getOvertimeRequests().stream()
                .sorted(Comparator.comparing(OvertimeRequest::getCreateTime).reversed())
                .collect(Collectors.toList());
    }

    public List<OvertimeRequest> getRequestsByUser(Long userId) {
        return dataStore.getOvertimeRequests().stream()
                .filter(r -> r.getUserId().equals(userId))
                .sorted(Comparator.comparing(OvertimeRequest::getCreateTime).reversed())
                .collect(Collectors.toList());
    }

    public List<OvertimeRequest> getPendingRequestsByDepartment(Department department) {
        return dataStore.getOvertimeRequests().stream()
                .filter(r -> r.getDepartment() == department && r.getStatus() == RequestStatus.PENDING)
                .sorted(Comparator.comparing(OvertimeRequest::getCreateTime))
                .collect(Collectors.toList());
    }

    public Optional<OvertimeRequest> getRequestById(Long id) {
        return dataStore.getOvertimeRequests().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst();
    }

    public OvertimeRequest createRequest(OvertimeRequestDTO dto) throws IOException {
        User user = userService.getUserById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (dto.getOvertimeDate() == null) {
            throw new RuntimeException("请选择加班日期");
        }
        if (dto.getOvertimeDate().isAfter(LocalDate.now())) {
            throw new RuntimeException("加班日期不能是未来日期");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new RuntimeException("请选择开始时间和结束时间");
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new RuntimeException("结束时间必须晚于开始时间");
        }
        if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            throw new RuntimeException("请填写加班事由");
        }

        boolean exists = dataStore.getOvertimeRequests().stream()
                .anyMatch(r -> r.getUserId().equals(dto.getUserId())
                        && r.getOvertimeDate().equals(dto.getOvertimeDate())
                        && (r.getStatus() == RequestStatus.PENDING || r.getStatus() == RequestStatus.APPROVED));
        if (exists) {
            throw new RuntimeException("该日期已存在待审批或已通过的加班申请");
        }

        double hours = calculateHours(dto.getStartTime(), dto.getEndTime());

        OvertimeRequest request = new OvertimeRequest();
        request.setId(dataStore.nextOvertimeId());
        request.setUserId(user.getId());
        request.setUserName(user.getName());
        request.setDepartment(user.getDepartment());
        request.setOvertimeDate(dto.getOvertimeDate());
        request.setStartTime(dto.getStartTime());
        request.setEndTime(dto.getEndTime());
        request.setHours(hours);
        request.setReason(dto.getReason());
        request.setStatus(RequestStatus.PENDING);
        request.setCreateTime(LocalDate.now());

        dataStore.getOvertimeRequests().add(request);
        dataStore.saveOvertimeRequests();
        return request;
    }

    public OvertimeRequest approveRequest(Long requestId, Long approverId) throws IOException {
        OvertimeRequest request = getRequestById(requestId)
                .orElseThrow(() -> new RuntimeException("申请不存在"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("该申请已处理");
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setApprovalComment("已通过");
        userService.addOvertimeHours(request.getUserId(), request.getHours());

        dataStore.saveOvertimeRequests();
        return request;
    }

    public OvertimeRequest rejectRequest(Long requestId, Long approverId, String comment) throws IOException {
        OvertimeRequest request = getRequestById(requestId)
                .orElseThrow(() -> new RuntimeException("申请不存在"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("该申请已处理");
        }
        if (comment == null || comment.trim().isEmpty()) {
            throw new RuntimeException("拒绝申请必须填写审批意见");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setApprovalComment(comment);

        dataStore.saveOvertimeRequests();
        return request;
    }

    private double calculateHours(LocalTime start, LocalTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        double rawHours = minutes / 60.0;
        double halfHours = Math.ceil(rawHours * 2) / 2.0;
        return halfHours;
    }
}
