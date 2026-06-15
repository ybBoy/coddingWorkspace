package com.overtime.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.overtime.model.User;
import com.overtime.util.JsonFileUtil;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private List<User> users = new ArrayList<>();

    @PostConstruct
    public void init() {
        loadFromFile();
    }

    public void loadFromFile() {
        users.clear();
        JSONArray array = JsonFileUtil.readArray("users.json");
        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.getJSONObject(i);
            User user = new User();
            user.setId(obj.getString("id"));
            user.setName(obj.getString("name"));
            user.setDepartment(obj.getString("department"));
            user.setRole(obj.getString("role"));
            user.setTotalOvertimeHours(obj.getDoubleValue("totalOvertimeHours"));
            user.setUsedTimeoffHours(obj.getDoubleValue("usedTimeoffHours"));
            users.add(user);
        }
    }

    public void saveToFile() {
        JSONArray array = new JSONArray();
        for (User user : users) {
            JSONObject obj = new JSONObject();
            obj.put("id", user.getId());
            obj.put("name", user.getName());
            obj.put("department", user.getDepartment());
            obj.put("role", user.getRole());
            obj.put("totalOvertimeHours", user.getTotalOvertimeHours());
            obj.put("usedTimeoffHours", user.getUsedTimeoffHours());
            array.add(obj);
        }
        JsonFileUtil.writeArray("users.json", array);
    }

    public List<User> getAllUsers() {
        return users;
    }

    public User getUserById(String id) {
        for (User user : users) {
            if (user.getId().equals(id)) {
                return user;
            }
        }
        return null;
    }

    public List<User> getUsersByDepartment(String department) {
        List<User> result = new ArrayList<>();
        for (User user : users) {
            if (user.getDepartment().equals(department)) {
                result.add(user);
            }
        }
        return result;
    }

    public void addOvertimeHours(String userId, double hours) {
        User user = getUserById(userId);
        if (user != null) {
            user.setTotalOvertimeHours(user.getTotalOvertimeHours() + hours);
            saveToFile();
        }
    }

    public void addUsedTimeoffHours(String userId, double hours) {
        User user = getUserById(userId);
        if (user != null) {
            user.setUsedTimeoffHours(user.getUsedTimeoffHours() + hours);
            saveToFile();
        }
    }
}
