package com.overtime.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.overtime.model.OvertimeRequest;
import com.overtime.model.User;
import com.overtime.util.JsonFileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class OvertimeService {

    @Autowired
    private UserService userService;

    private List<OvertimeRequest> requests = new ArrayList<>();

    @PostConstruct
    public void init() {
        loadFromFile();
    }

    public void loadFromFile() {
        requests.clear();
        JSONArray array = JsonFileUtil.readArray("overtime_requests.json");
        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.getJSONObject(i);
            OvertimeRequest req = new OvertimeRequest();
            req.setId(obj.getString("id"));
            req.setUserId(obj.getString("userId"));
            req.setDate(obj.getString("date"));
            req.setStartTime(obj.getString("startTime"));
            req.setEndTime(obj.getString("endTime"));
            req.setReason(obj.getString("reason"));
            req.setHours(obj.getDoubleValue("hours"));
            req.setStatus(obj.getString("status"));
            req.setApproverId(obj.getString("approverId"));
            req.setRejectReason(obj.getString("rejectReason"));
            req.setCreateTime(obj.getString("createTime"));
            requests.add(req);
        }
    }

    public void saveToFile() {
        JSONArray array = new JSONArray();
        for (OvertimeRequest req : requests) {
            JSONObject obj = new JSONObject();
            obj.put("id", req.getId());
            obj.put("userId", req.getUserId());
            obj.put("date", req.getDate());
            obj.put("startTime", req.getStartTime());
            obj.put("endTime", req.getEndTime());
            obj.put("reason", req.getReason());
            obj.put("hours", req.getHours());
            obj.put("status", req.getStatus());
            obj.put("approverId", req.getApproverId());
            obj.put("rejectReason", req.getRejectReason());
            obj.put("createTime", req.getCreateTime());
            array.add(obj);
        }
        JsonFileUtil.writeArray("overtime_requests.json", array);
    }

    public static double calculateHours(String startTime, String endTime) {
        LocalTime start = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime end = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"));
        long minutes = ChronoUnit.MINUTES.between(start, end);
        if (minutes <= 0) {
            return 0;
        }
        double halfHours = Math.ceil(minutes / 30.0);
        return halfHours * 0.5;
    }

    public String submitRequest(OvertimeRequest req) {
        LocalDate overtimeDate = LocalDate.parse(req.getDate());
        if (overtimeDate.isAfter(LocalDate.now())) {
            return "加班日期不能是未来日期";
        }
        for (OvertimeRequest existing : requests) {
            if (existing.getUserId().equals(req.getUserId())
                    && existing.getDate().equals(req.getDate())
                    && !existing.getStatus().equals("REJECTED")) {
                return "同一天已有待审批或已通过的加班申请，不能重复提交";
            }
        }
        if (req.getStartTime().compareTo(req.getEndTime()) >= 0) {
            return "结束时间必须晚于开始时间";
        }
        double hours = calculateHours(req.getStartTime(), req.getEndTime());
        if (hours <= 0) {
            return "加班时长计算异常";
        }
        req.setId("OT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        req.setHours(hours);
        req.setStatus("PENDING");
        req.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        requests.add(req);
        saveToFile();
        return null;
    }

    public List<OvertimeRequest> getRequestsByUser(String userId) {
        List<OvertimeRequest> result = new ArrayList<>();
        for (OvertimeRequest req : requests) {
            if (req.getUserId().equals(userId)) {
                result.add(req);
            }
        }
        return result;
    }

    public List<OvertimeRequest> getPendingByDepartment(String department) {
        List<User> deptUsers = userService.getUsersByDepartment(department);
        List<String> deptUserIds = new ArrayList<>();
        for (User u : deptUsers) {
            deptUserIds.add(u.getId());
        }
        List<OvertimeRequest> result = new ArrayList<>();
        for (OvertimeRequest req : requests) {
            if (deptUserIds.contains(req.getUserId()) && "PENDING".equals(req.getStatus())) {
                result.add(req);
            }
        }
        return result;
    }

    public String approve(String requestId, String approverId) {
        OvertimeRequest req = null;
        for (OvertimeRequest r : requests) {
            if (r.getId().equals(requestId)) {
                req = r;
                break;
            }
        }
        if (req == null) {
            return "申请不存在";
        }
        if (!"PENDING".equals(req.getStatus())) {
            return "该申请已处理";
        }
        req.setStatus("APPROVED");
        req.setApproverId(approverId);
        userService.addOvertimeHours(req.getUserId(), req.getHours());
        saveToFile();
        userService.loadFromFile();
        return null;
    }

    public String reject(String requestId, String approverId, String rejectReason) {
        OvertimeRequest req = null;
        for (OvertimeRequest r : requests) {
            if (r.getId().equals(requestId)) {
                req = r;
                break;
            }
        }
        if (req == null) {
            return "申请不存在";
        }
        if (!"PENDING".equals(req.getStatus())) {
            return "该申请已处理";
        }
        if (rejectReason == null || rejectReason.trim().isEmpty()) {
            return "拒绝必须填写审批意见";
        }
        req.setStatus("REJECTED");
        req.setApproverId(approverId);
        req.setRejectReason(rejectReason);
        saveToFile();
        return null;
    }
}
