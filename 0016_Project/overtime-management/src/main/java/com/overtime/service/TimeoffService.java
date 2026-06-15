package com.overtime.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.overtime.model.TimeoffRequest;
import com.overtime.model.User;
import com.overtime.util.JsonFileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class TimeoffService {

    @Autowired
    private UserService userService;

    private List<TimeoffRequest> requests = new ArrayList<>();

    @PostConstruct
    public void init() {
        loadFromFile();
    }

    public void loadFromFile() {
        requests.clear();
        JSONArray array = JsonFileUtil.readArray("timeoff_requests.json");
        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.getJSONObject(i);
            TimeoffRequest req = new TimeoffRequest();
            req.setId(obj.getString("id"));
            req.setUserId(obj.getString("userId"));
            req.setDate(obj.getString("date"));
            req.setType(obj.getString("type"));
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
        for (TimeoffRequest req : requests) {
            JSONObject obj = new JSONObject();
            obj.put("id", req.getId());
            obj.put("userId", req.getUserId());
            obj.put("date", req.getDate());
            obj.put("type", req.getType());
            obj.put("reason", req.getReason());
            obj.put("hours", req.getHours());
            obj.put("status", req.getStatus());
            obj.put("approverId", req.getApproverId());
            obj.put("rejectReason", req.getRejectReason());
            obj.put("createTime", req.getCreateTime());
            array.add(obj);
        }
        JsonFileUtil.writeArray("timeoff_requests.json", array);
    }

    public String submitRequest(TimeoffRequest req) {
        LocalDate timeoffDate = LocalDate.parse(req.getDate());
        if (!timeoffDate.isAfter(LocalDate.now()) && !timeoffDate.isEqual(LocalDate.now())) {
            return "调休日期不能是过去日期";
        }
        for (TimeoffRequest existing : requests) {
            if (existing.getUserId().equals(req.getUserId())
                    && existing.getDate().equals(req.getDate())
                    && !existing.getStatus().equals("REJECTED")) {
                return "同一天已有待审批或已通过的调休申请，不能重复提交";
            }
        }
        double hours = "HALF_DAY".equals(req.getType()) ? 4.0 : 8.0;
        User user = userService.getUserById(req.getUserId());
        if (user == null) {
            return "用户不存在";
        }
        double remaining = user.getTotalOvertimeHours() - user.getUsedTimeoffHours();
        if (remaining < hours) {
            return "剩余调休时长不足（当前剩余：" + remaining + "小时，需要：" + hours + "小时）";
        }
        req.setId("TO" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        req.setHours(hours);
        req.setStatus("PENDING");
        req.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        requests.add(req);
        saveToFile();
        return null;
    }

    public List<TimeoffRequest> getRequestsByUser(String userId) {
        List<TimeoffRequest> result = new ArrayList<>();
        for (TimeoffRequest req : requests) {
            if (req.getUserId().equals(userId)) {
                result.add(req);
            }
        }
        return result;
    }

    public List<TimeoffRequest> getPendingByDepartment(String department) {
        List<User> deptUsers = userService.getUsersByDepartment(department);
        List<String> deptUserIds = new ArrayList<>();
        for (User u : deptUsers) {
            deptUserIds.add(u.getId());
        }
        List<TimeoffRequest> result = new ArrayList<>();
        for (TimeoffRequest req : requests) {
            if (deptUserIds.contains(req.getUserId()) && "PENDING".equals(req.getStatus())) {
                result.add(req);
            }
        }
        return result;
    }

    public String approve(String requestId, String approverId) {
        TimeoffRequest req = null;
        for (TimeoffRequest r : requests) {
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
        userService.loadFromFile();
        User user = userService.getUserById(req.getUserId());
        if (user == null) {
            return "用户不存在";
        }
        double remaining = user.getTotalOvertimeHours() - user.getUsedTimeoffHours();
        if (remaining < req.getHours()) {
            return "该员工剩余调休时长不足（当前剩余：" + remaining + "小时，需要：" + req.getHours() + "小时），无法通过";
        }
        req.setStatus("APPROVED");
        req.setApproverId(approverId);
        userService.addUsedTimeoffHours(req.getUserId(), req.getHours());
        saveToFile();
        userService.loadFromFile();
        return null;
    }

    public String reject(String requestId, String approverId, String rejectReason) {
        TimeoffRequest req = null;
        for (TimeoffRequest r : requests) {
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
