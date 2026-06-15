package storage;

import domain.FitnessCheckin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonUtil {

    public static String toJson(FitnessCheckin checkin) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(escape(checkin.getId())).append("\",");
        sb.append("\"checkinDate\":\"").append(checkin.getCheckinDate().toString()).append("\",");
        sb.append("\"exerciseType\":\"").append(escape(checkin.getExerciseType())).append("\",");
        sb.append("\"duration\":").append(checkin.getDuration()).append(",");
        sb.append("\"mood\":\"").append(escape(checkin.getMood())).append("\",");
        if (checkin.getNote() != null) {
            sb.append("\"note\":\"").append(escape(checkin.getNote())).append("\"");
        } else {
            sb.append("\"note\":\"\"");
        }
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
        if (json == null || json.trim().isEmpty() || json.trim().equals("[]")) {
            return result;
        }

        Pattern objectPattern = Pattern.compile("\\{([^}]*)\\}");
        Matcher objectMatcher = objectPattern.matcher(json);

        while (objectMatcher.find()) {
            String objectContent = objectMatcher.group(1);
            FitnessCheckin checkin = parseObject(objectContent);
            if (checkin != null) {
                result.add(checkin);
            }
        }

        return result;
    }

    private static FitnessCheckin parseObject(String content) {
        try {
            FitnessCheckin checkin = new FitnessCheckin();

            Pattern fieldPattern = Pattern.compile("\"([^\"]+)\":(\"([^\"]*)\"|(\\d+))");
            Matcher fieldMatcher = fieldPattern.matcher(content);

            while (fieldMatcher.find()) {
                String key = fieldMatcher.group(1);
                String strValue = fieldMatcher.group(3);
                String numValue = fieldMatcher.group(4);

                if ("id".equals(key)) {
                    checkin.setId(strValue);
                } else if ("checkinDate".equals(key)) {
                    checkin.setCheckinDate(LocalDate.parse(strValue));
                } else if ("exerciseType".equals(key)) {
                    checkin.setExerciseType(strValue);
                } else if ("duration".equals(key) && numValue != null) {
                    checkin.setDuration(Integer.parseInt(numValue));
                } else if ("mood".equals(key)) {
                    checkin.setMood(strValue);
                } else if ("note".equals(key)) {
                    checkin.setNote(strValue);
                }
            }

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
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static String toErrorJson(String message) {
        return "{\"error\":true,\"message\":\"" + escape(message) + "\"}";
    }

    public static String toSuccessJson(String message) {
        return "{\"success\":true,\"message\":\"" + escape(message) + "\"}";
    }

    public static Map<String, String> parseRequestBody(String json) {
        Map<String, String> result = new HashMap<>();
        if (json == null || json.trim().isEmpty()) {
            return result;
        }

        Pattern fieldPattern = Pattern.compile("\"([^\"]+)\":\\s*(\"([^\"]*)\"|(\\d+))");
        Matcher fieldMatcher = fieldPattern.matcher(json);

        while (fieldMatcher.find()) {
            String key = fieldMatcher.group(1);
            String strValue = fieldMatcher.group(3);
            String numValue = fieldMatcher.group(4);
            if (strValue != null) {
                result.put(key, strValue);
            } else if (numValue != null) {
                result.put(key, numValue);
            }
        }

        return result;
    }
}
