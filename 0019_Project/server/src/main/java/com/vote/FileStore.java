package com.vote;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * FileStore 职责：
 * - 将内存中的完整 VoteState 序列化为 JSON 写入本地文件
 * - 服务启动时从本地 JSON 文件恢复完整状态
 * - 兼容旧格式（纯选项数组）
 * - 简单的手写 JSON 解析/生成，避免引入额外依赖，适配 JDK8
 *
 * JSON 格式：
 * {
 *   "options": [{"id":"1","name":"方案A","votes":2}, ...],
 *   "clientVotes": {"client1":"opt1", "client2":"opt2", ...},
 *   "locked": false,
 *   "timerEndTime": 1234567890000
 * }
 */
public class FileStore {
    private final Path filePath;

    public FileStore(String fileName) {
        this.filePath = Paths.get(fileName);
    }

    /**
     * 保存完整状态到 JSON 文件
     */
    public synchronized void saveState(VoteState state) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');

        // options 数组
        sb.append("\"options\":[");
        List<VoteOption> options = state.getOptions();
        for (int i = 0; i < options.size(); i++) {
            VoteOption o = options.get(i);
            if (i > 0) sb.append(',');
            sb.append('{')
              .append("\"id\":\"").append(escape(o.getId())).append("\",")
              .append("\"name\":\"").append(escape(o.getName())).append("\",")
              .append("\"votes\":").append(o.getVotes())
              .append('}');
        }
        sb.append("],");

        // clientVotes 对象
        sb.append("\"clientVotes\":{");
        Map<String, String> cv = state.getClientVotes();
        int idx = 0;
        for (Map.Entry<String, String> e : cv.entrySet()) {
            if (idx > 0) sb.append(',');
            sb.append("\"").append(escape(e.getKey())).append("\":\"")
              .append(escape(e.getValue())).append("\"");
            idx++;
        }
        sb.append("},");

        // locked 和 timerEndTime
        sb.append("\"locked\":").append(state.isLocked()).append(",");
        sb.append("\"timerEndTime\":").append(state.getTimerEndTime());

        sb.append('}');

        try {
            Files.write(filePath, sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("保存投票数据失败: " + e.getMessage());
        }
    }

    /**
     * 从 JSON 文件加载完整状态；文件不存在或解析失败则返回空状态
     */
    public synchronized VoteState loadState() {
        VoteState state = new VoteState();
        if (!Files.exists(filePath)) {
            return state;
        }
        try {
            String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8).trim();
            if (content.isEmpty()) return state;

            // 兼容旧格式：如果是数组开头，按旧格式解析
            if (content.startsWith("[")) {
                List<VoteOption> options = parseOptionsArray(content);
                state.setOptions(options);
                System.out.println("已从旧格式文件恢复 " + options.size() + " 个投票选项");
                return state;
            }

            // 新格式：对象
            if (content.startsWith("{")) {
                parseStateObject(content, state);
                return state;
            }
        } catch (Exception e) {
            System.err.println("加载投票数据失败: " + e.getMessage());
        }
        return state;
    }

    // ---------- 解析逻辑 ----------

    private void parseStateObject(String json, VoteState state) {
        // 去掉最外层 {}
        String inner = json.substring(1, json.length() - 1);

        // 解析 options 字段
        String optionsJson = extractFieldArray(inner, "options");
        if (optionsJson != null) {
            List<VoteOption> options = parseOptionsArray(optionsJson);
            state.setOptions(options);
        }

        // 解析 clientVotes 字段
        String cvJson = extractFieldObject(inner, "clientVotes");
        if (cvJson != null) {
            Map<String, String> cv = parseClientVotes(cvJson);
            state.setClientVotes(cv);
        }

        // 解析 locked 字段
        String lockedStr = extractFieldValue(inner, "locked");
        if (lockedStr != null) {
            state.setLocked("true".equals(lockedStr));
        }

        // 解析 timerEndTime 字段
        String timerStr = extractFieldValue(inner, "timerEndTime");
        if (timerStr != null) {
            try {
                state.setTimerEndTime(Long.parseLong(timerStr));
            } catch (NumberFormatException ignored) {}
        }

        System.out.println("已从文件恢复 " + state.getOptions().size() + " 个选项, "
            + state.getClientVotes().size() + " 条投票记录, "
            + "locked=" + state.isLocked() + ", timerEnd=" + state.getTimerEndTime());
    }

    /** 提取字段值（非数组非对象） */
    private String extractFieldValue(String inner, String field) {
        String search = "\"" + field + "\":";
        int idx = inner.indexOf(search);
        if (idx < 0) return null;
        int start = idx + search.length();
        while (start < inner.length() && Character.isWhitespace(inner.charAt(start))) start++;
        int end = start;
        while (end < inner.length() && inner.charAt(end) != ',' && inner.charAt(end) != '}') end++;
        return inner.substring(start, end).trim();
    }

    /** 提取数组字段，返回 [ ... ] 内容（包含括号） */
    private String extractFieldArray(String inner, String field) {
        String search = "\"" + field + "\":";
        int idx = inner.indexOf(search);
        if (idx < 0) return null;
        int start = idx + search.length();
        while (start < inner.length() && Character.isWhitespace(inner.charAt(start))) start++;
        if (start >= inner.length() || inner.charAt(start) != '[') return null;
        int depth = 0;
        int end = start;
        for (; end < inner.length(); end++) {
            char c = inner.charAt(end);
            if (c == '[') depth++;
            else if (c == ']') depth--;
            if (depth == 0) {
                end++;
                break;
            }
        }
        return inner.substring(start, end);
    }

    /** 提取对象字段，返回 { ... } 内容（包含括号） */
    private String extractFieldObject(String inner, String field) {
        String search = "\"" + field + "\":";
        int idx = inner.indexOf(search);
        if (idx < 0) return null;
        int start = idx + search.length();
        while (start < inner.length() && Character.isWhitespace(inner.charAt(start))) start++;
        if (start >= inner.length() || inner.charAt(start) != '{') return null;
        int depth = 0;
        int end = start;
        boolean inStr = false;
        for (; end < inner.length(); end++) {
            char c = inner.charAt(end);
            if (c == '"' && (end == 0 || inner.charAt(end - 1) != '\\')) inStr = !inStr;
            if (!inStr) {
                if (c == '{') depth++;
                else if (c == '}') depth--;
                if (depth == 0) {
                    end++;
                    break;
                }
            }
        }
        return inner.substring(start, end);
    }

    private List<VoteOption> parseOptionsArray(String json) {
        List<VoteOption> list = new ArrayList<>();
        String inner = json.substring(1, json.length() - 1);
        List<String> objects = splitObjects(inner);
        for (String objStr : objects) {
            VoteOption opt = parseOptionObject(objStr);
            if (opt != null) list.add(opt);
        }
        return list;
    }

    private Map<String, String> parseClientVotes(String json) {
        Map<String, String> result = new HashMap<>();
        String inner = json.substring(1, json.length() - 1);
        if (inner.trim().isEmpty()) return result;

        // 简单分割键值对
        List<String> pairs = splitObjects(inner); // 复用 splitObjects 按逗号分割
        for (String pair : pairs) {
            int colon = pair.indexOf(':');
            if (colon < 0) continue;
            String key = pair.substring(0, colon).trim();
            String value = pair.substring(colon + 1).trim();
            key = key.replaceAll("^\"|\"$", "");
            value = value.replaceAll("^\"|\"$", "");
            if (!key.isEmpty()) result.put(key, value);
        }
        return result;
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
                if (c == '{' || c == '[') depth++;
                if (c == '}' || c == ']') depth--;
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

    private VoteOption parseOptionObject(String s) {
        if (s.startsWith("{") && s.endsWith("}")) {
            s = s.substring(1, s.length() - 1);
        }
        Map<String, String> kv = new HashMap<>();
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

    // ---------- 兼容旧 API ----------
    @Deprecated
    public void save(List<VoteOption> options) {
        VoteState state = new VoteState();
        state.setOptions(options);
        saveState(state);
    }

    @Deprecated
    public List<VoteOption> load() {
        return loadState().getOptions();
    }
}
