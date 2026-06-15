package com.taskmanager.controller;

import com.taskmanager.model.Member;
import com.taskmanager.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    @Autowired
    private TaskService taskService;

    @GetMapping
    public List<Member> getAllMembers() {
        return taskService.getAllMembers();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getMemberInfo(@PathVariable String id) {
        Member member = taskService.getMemberById(id);
        if (member == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "成员不存在");
            return err;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", member.getId());
        result.put("name", member.getName());
        result.put("role", member.getRole());
        result.put("avatar", member.getAvatar());
        result.put("isLeader", "leader".equals(member.getRole()));
        return result;
    }
}
