package com.meeting.util;

import com.meeting.model.Booking;
import com.meeting.model.MeetingRoom;
import com.meeting.model.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonUtil {

    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String toJson(User u) {
        return "{" +
                "\"id\":\"" + escape(u.getId()) + "\"," +
                "\"name\":\"" + escape(u.getName()) + "\"," +
                "\"role\":\"" + escape(u.getRole()) + "\"}";
    }

    public static String toJson(MeetingRoom r) {
        return "{" +
                "\"id\":\"" + escape(r.getId()) + "\"," +
                "\"name\":\"" + escape(r.getName()) + "\"," +
                "\"capacity\":" + r.getCapacity() + "}";
    }

    public static String toJson(Booking b) {
        return "{" +
                "\"id\":\"" + escape(b.getId()) + "\"," +
                "\"roomId\":\"" + escape(b.getRoomId()) + "\"," +
                "\"userId\":\"" + escape(b.getUserId()) + "\"," +
                "\"userName\":\"" + escape(b.getUserName()) + "\"," +
                "\"date\":\"" + escape(b.getDate()) + "\"," +
                "\"startTime\":\"" + escape(b.getStartTime()) + "\"," +
                "\"endTime\":\"" + escape(b.getEndTime()) + "\"," +
                "\"purpose\":\"" + escape(b.getPurpose()) + "\"," +
                "\"createdAt\":" + b.getCreatedAt() + "}";
    }

    @SuppressWarnings("unchecked")
    public static String listToJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            Object o = list.get(i);
            if (o instanceof User) sb.append(toJson((User) o));
            else if (o instanceof MeetingRoom) sb.append(toJson((MeetingRoom) o));
            else if (o instanceof Booking) sb.append(toJson((Booking) o));
        }
        sb.append("]");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static String success(Object obj) {
        if (obj instanceof List) return listToJson((List<Object>) obj);
        if (obj instanceof User) return toJson((User) obj);
        if (obj instanceof MeetingRoom) return toJson((MeetingRoom) obj);
        if (obj instanceof Booking) return toJson((Booking) obj);
        return obj == null ? "null" : obj.toString();
    }

    public static String wrapSuccess(Object data) {
        return "{\"success\":true,\"data\":" + success(data) + "}";
    }

    public static String wrapError(String message) {
        return "{\"success\":false,\"message\":\"" + escape(message) + "\"}";
    }

    public static class JsonObject {
        public Map<String, Object> map = new LinkedHashMap<String, Object>();
        public void put(String key, Object value) { map.put(key, value); }
        public Object get(String key) { return map.get(key); }
        public String getString(String key) {
            Object v = map.get(key);
            return v == null ? null : v.toString();
        }
        public int getInt(String key) {
            Object v = map.get(key);
            if (v instanceof Number) return ((Number) v).intValue();
            return Integer.parseInt(v.toString());
        }
        public long getLong(String key) {
            Object v = map.get(key);
            if (v instanceof Number) return ((Number) v).longValue();
            return Long.parseLong(v.toString());
        }
        public boolean has(String key) { return map.containsKey(key); }
    }

    public static class JsonArray {
        public List<Object> list = new ArrayList<Object>();
        public int size() { return list.size(); }
        public Object get(int i) { return list.get(i); }
    }

    private static int skipWs(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return i;
    }

    private static String parseStr(String s, int[] pos) {
        int i = pos[0];
        if (s.charAt(i) != '"') throw new RuntimeException("Expected '\"'");
        i++;
        StringBuilder sb = new StringBuilder();
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '"') { i++; pos[0] = i; return sb.toString(); }
            if (c == '\\') {
                i++;
                char e = s.charAt(i);
                if (e == 'n') sb.append('\n');
                else if (e == 'r') sb.append('\r');
                else if (e == 't') sb.append('\t');
                else sb.append(e);
            } else {
                sb.append(c);
            }
            i++;
        }
        throw new RuntimeException("Unterminated string");
    }

    private static Object parseVal(String s, int[] pos) {
        int i = skipWs(s, pos[0]);
        pos[0] = i;
        if (i >= s.length()) throw new RuntimeException("Unexpected end");
        char c = s.charAt(i);
        if (c == '"') return parseStr(s, pos);
        if (c == '{') return parseObj(s, pos);
        if (c == '[') return parseArr(s, pos);
        if (c == 't') { pos[0] = i + 4; return true; }
        if (c == 'f') { pos[0] = i + 5; return false; }
        if (c == 'n') { pos[0] = i + 4; return null; }
        StringBuilder sb = new StringBuilder();
        while (i < s.length()) {
            c = s.charAt(i);
            if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) break;
            sb.append(c); i++;
        }
        pos[0] = i;
        String num = sb.toString();
        try { return Long.parseLong(num); } catch (Exception e) { return Double.parseDouble(num); }
    }

    public static JsonObject parseObj(String s, int[] pos) {
        JsonObject obj = new JsonObject();
        int i = pos[0];
        if (s.charAt(i) != '{') throw new RuntimeException("Expected '{'");
        i++;
        i = skipWs(s, i);
        if (s.charAt(i) == '}') { pos[0] = i + 1; return obj; }
        while (i < s.length()) {
            i = skipWs(s, i);
            int[] p = new int[]{i};
            String key = parseStr(s, p);
            i = skipWs(s, p[0]);
            if (s.charAt(i) != ':') throw new RuntimeException("Expected ':'");
            i++;
            pos[0] = i;
            Object value = parseVal(s, pos);
            obj.put(key, value);
            i = skipWs(s, pos[0]);
            if (s.charAt(i) == ',') { i++; continue; }
            if (s.charAt(i) == '}') { i++; pos[0] = i; return obj; }
        }
        throw new RuntimeException("Unterminated object");
    }

    public static JsonArray parseArr(String s, int[] pos) {
        JsonArray arr = new JsonArray();
        int i = pos[0];
        if (s.charAt(i) != '[') throw new RuntimeException("Expected '['");
        i++;
        i = skipWs(s, i);
        if (s.charAt(i) == ']') { pos[0] = i + 1; return arr; }
        while (i < s.length()) {
            pos[0] = i;
            Object value = parseVal(s, pos);
            arr.list.add(value);
            i = skipWs(s, pos[0]);
            if (s.charAt(i) == ',') { i++; i = skipWs(s, i); continue; }
            if (s.charAt(i) == ']') { i++; pos[0] = i; return arr; }
        }
        throw new RuntimeException("Unterminated array");
    }

    public static JsonObject parseJsonObject(String s) {
        s = s.trim();
        return parseObj(s, new int[]{0});
    }

    public static JsonArray parseJsonArray(String s) {
        s = s.trim();
        return parseArr(s, new int[]{0});
    }
}
