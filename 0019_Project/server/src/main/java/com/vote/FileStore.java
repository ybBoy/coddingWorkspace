package com.vote;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * FileStore 职责：
 * - 将内存中的投票选项列表序列化为 JSON 并写入本地文件
 * - 服务启动时从本地 JSON 文件恢复数据
 * - 简单的手写 JSON 解析/生成，避免引入额外依赖，适配 JDK8
 */
public class FileStore {
    private final Path filePath;

    public FileStore(String fileName) {
        this.filePath = Paths.get(fileName);
    }

    /**
     * 将选项列表保存为 JSON 文件
     * 格式: [{"id":"1","name":"方案A","votes":2}, ...]
     */
    public synchronized void save(List<VoteOption> options) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < options.size(); i++) {
            VoteOption o = options.get(i);
            if (i > 0) sb.append(',');
            sb.append('{')
              .append("\"id\":\"").append(escape(o.getId())).append("\",")
              .append("\"name\":\"").append(escape(o.getName())).append("\",")
              .append("\"votes\":").append(o.getVotes())
              .append('}');
        }
        sb.append(']');
        try {
            Files.write(filePath, sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("保存投票数据失败: " + e.getMessage());
        }
    }

    /**
     * 从 JSON 文件读取选项列表；文件不存在或解析失败则返回空列表
     */
    public synchronized List<VoteOption> load() {
        List<VoteOption> result = new ArrayList<>();
        if (!Files.exists(filePath)) {
            return result;
        }
        try {
            String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8).trim();
            if (content.isEmpty() || content.equals("[]")) {
                return result;
            }
            result = parseJsonArray(content);
        } catch (Exception e) {
            System.err.println("加载投票数据失败: " + e.getMessage());
        }
        return result;
    }

    // ---------- 以下为简单的手写 JSON 解析（仅适配本项目结构） ----------

    private List<VoteOption> parseJsonArray(String json) {
        List<VoteOption> list = new ArrayList<>();
        // 去掉最外层的 []
        String inner = json.substring(1, json.length() - 1);
        // 简单分割对象（假定 name 中不含特殊字符）
        List<String> objects = splitObjects(inner);
        for (String objStr : objects) {
            VoteOption opt = parseObject(objStr);
            if (opt != null) list.add(opt);
        }
        return list;
    }

    private List<String> splitObjects(String s) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        boolean inStr = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) inStr = !inStr;
            if (!inStr) {
                if (c == '{') depth++;
                if (c == '}') depth--;
                if (c == ',' && depth == 0) {
                    result.add(current.toString().trim());
                    current = new StringBuilder();
                    continue;
                }
            }
            current.append(c);
        }
        if (current.length() > 0) {
            String last = current.toString().trim();
            if (!last.isEmpty()) result.add(last);
        }
        return result;
    }

    private VoteOption parseObject(String s) {
        if (s.startsWith("{") && s.endsWith("}")) {
            s = s.substring(1, s.length() - 1);
        }
        Map<String, String> kv = new HashMap<>();
        // 按逗号分隔键值对，简化处理
        String[] pairs = s.split(",");
        for (String pair : pairs) {
            int colon = pair.indexOf(':');
            if (colon < 0) continue;
            String key = pair.substring(0, colon).trim();
            String value = pair.substring(colon + 1).trim();
            key = key.replaceAll("^\"|\"$", "");
            value = value.replaceAll("^\"|\"$", "");
            kv.put(key, value);
        }
        String id = kv.get("id");
        String name = kv.get("name");
        String votesStr = kv.get("votes");
        if (id == null || name == null || votesStr == null) return null;
        try {
            int votes = Integer.parseInt(votesStr);
            return new VoteOption(id, name, votes);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
