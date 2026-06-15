package storage;

import domain.FitnessCheckin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonUtil {

    public static String toJson(FitnessCheckin checkin) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(escape(checkin.getId())).append("\",");
        sb.append("\"checkinDate\":\"").append(checkin.getCheckinDate().toString()).append("\",");
        sb.append("\"exerciseType\":\"").append(escape(checkin.getExerciseType())).append("\",");
        sb.append("\"duration\":").append(checkin.getDuration()).append(",");
        sb.append("\"mood\":\"").append(escape(checkin.getMood())).append("\",");
        sb.append("\"note\":\"").append(escape(checkin.getNote() != null ? checkin.getNote() : "")).append("\"");
        sb.append("}");
        return sb.toString();
    }

    public static String toJson(List<FitnessCheckin> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(toJson(list.get(i)));
            if (i < list.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static List<FitnessCheckin> parseList(String json) {
        List<FitnessCheckin> result = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) {
            return result;
        }

        JsonParser parser = new JsonParser(json);
        parser.skipWhitespace();
        if (!parser.match('[')) {
            return result;
        }

        parser.skipWhitespace();
        while (!parser.match(']')) {
            if (parser.peek() == ',') {
                parser.next();
                continue;
            }
            Map<String, Object> obj = parser.parseObject();
            if (obj != null) {
                FitnessCheckin checkin = mapToCheckin(obj);
                if (checkin != null) {
                    result.add(checkin);
                }
            }
            parser.skipWhitespace();
        }

        return result;
    }

    public static Map<String, String> parseRequestBody(String json) {
        Map<String, String> result = new HashMap<>();
        if (json == null || json.trim().isEmpty()) {
            return result;
        }

        JsonParser parser = new JsonParser(json);
        parser.skipWhitespace();
        Map<String, Object> obj = parser.parseObject();
        if (obj != null) {
            for (Map.Entry<String, Object> entry : obj.entrySet()) {
                Object val = entry.getValue();
                result.put(entry.getKey(), val != null ? val.toString() : null);
            }
        }

        return result;
    }

    private static FitnessCheckin mapToCheckin(Map<String, Object> map) {
        try {
            FitnessCheckin checkin = new FitnessCheckin();
            checkin.setId((String) map.get("id"));
            String dateStr = (String) map.get("checkinDate");
            if (dateStr != null) {
                checkin.setCheckinDate(LocalDate.parse(dateStr));
            }
            checkin.setExerciseType((String) map.get("exerciseType"));
            Object dur = map.get("duration");
            if (dur instanceof Number) {
                checkin.setDuration(((Number) dur).intValue());
            } else if (dur instanceof String) {
                checkin.setDuration(Integer.parseInt((String) dur));
            }
            checkin.setMood((String) map.get("mood"));
            checkin.setNote((String) map.get("note"));

            if (checkin.getId() != null && checkin.getCheckinDate() != null) {
                return checkin;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    public static String toErrorJson(String message) {
        return "{\"error\":true,\"message\":\"" + escape(message) + "\"}";
    }

    public static String toSuccessJson(String message) {
        return "{\"success\":true,\"message\":\"" + escape(message) + "\"}";
    }

    static class JsonParser {
        private final String src;
        private int pos;

        public JsonParser(String src) {
            this.src = src;
            this.pos = 0;
        }

        public char peek() {
            if (pos >= src.length()) return '\0';
            return src.charAt(pos);
        }

        public char next() {
            if (pos >= src.length()) return '\0';
            return src.charAt(pos++);
        }

        public boolean match(char c) {
            if (peek() == c) {
                pos++;
                return true;
            }
            return false;
        }

        public void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }

        public Map<String, Object> parseObject() {
            Map<String, Object> obj = new HashMap<>();
            skipWhitespace();
            if (!match('{')) {
                return null;
            }
            skipWhitespace();
            while (!match('}')) {
                skipWhitespace();
                String key = parseString();
                if (key == null) return obj;
                skipWhitespace();
                if (!match(':')) return obj;
                skipWhitespace();
                Object value = parseValue();
                obj.put(key, value);
                skipWhitespace();
                match(',');
            }
            return obj;
        }

        public Object parseValue() {
            skipWhitespace();
            char c = peek();
            if (c == '"') {
                return parseString();
            } else if (c == '{') {
                return parseObject();
            } else if (c == '[') {
                return parseArray();
            } else if (c == 't' || c == 'f') {
                return parseBoolean();
            } else if (c == 'n') {
                return parseNull();
            } else if (c == '-' || (c >= '0' && c <= '9')) {
                return parseNumber();
            }
            return null;
        }

        public String parseString() {
            if (!match('"')) return null;
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = next();
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    char esc = next();
                    switch (esc) {
                        case '"':  sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/':  sb.append('/'); break;
                        case 'n':  sb.append('\n'); break;
                        case 'r':  sb.append('\r'); break;
                        case 't':  sb.append('\t'); break;
                        case 'b':  sb.append('\b'); break;
                        case 'f':  sb.append('\f'); break;
                        case 'u':
                            if (pos + 4 <= src.length()) {
                                String hex = src.substring(pos, pos + 4);
                                try {
                                    sb.append((char) Integer.parseInt(hex, 16));
                                    pos += 4;
                                } catch (NumberFormatException e) {
                                    sb.append('\\').append('u');
                                }
                            }
                            break;
                        default:
                            sb.append('\\').append(esc);
                    }
                } else if (c != '\0') {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        public Number parseNumber() {
            int start = pos;
            if (peek() == '-') next();
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                pos++;
            }
            boolean isFloat = false;
            if (peek() == '.') {
                isFloat = true;
                pos++;
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                    pos++;
                }
            }
            if (peek() == 'e' || peek() == 'E') {
                isFloat = true;
                pos++;
                if (peek() == '+' || peek() == '-') pos++;
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                    pos++;
                }
            }
            String numStr = src.substring(start, pos);
            try {
                if (isFloat) {
                    return Double.parseDouble(numStr);
                } else {
                    return Long.parseLong(numStr);
                }
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        public Boolean parseBoolean() {
            if (pos + 4 <= src.length() && src.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (pos + 5 <= src.length() && src.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            return Boolean.FALSE;
        }

        public Object parseNull() {
            if (pos + 4 <= src.length() && src.startsWith("null", pos)) {
                pos += 4;
            }
            return null;
        }

        public List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            if (!match('[')) return list;
            skipWhitespace();
            while (!match(']')) {
                if (peek() == ',') {
                    next();
                    continue;
                }
                list.add(parseValue());
                skipWhitespace();
            }
            return list;
        }
    }
}
