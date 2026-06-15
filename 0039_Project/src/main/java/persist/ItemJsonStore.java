package persist;

import model.DisposePlan;
import model.HouseholdItem;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ItemJsonStore {
    private final Path dataFilePath;
    private final List<HouseholdItem> items = new ArrayList<>();

    public ItemJsonStore(String dataDir) {
        this.dataFilePath = Paths.get(dataDir, "items.json");
        loadFromFile();
    }

    public synchronized List<HouseholdItem> findAll() {
        return new ArrayList<>(items);
    }

    public synchronized HouseholdItem findById(String id) {
        for (HouseholdItem item : items) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }

    public synchronized void add(HouseholdItem item) {
        items.add(item);
        saveToFile();
    }

    public synchronized boolean update(String id, HouseholdItem updated) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(id)) {
                updated.setId(id);
                items.set(i, updated);
                saveToFile();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean updateDisposePlan(String id, DisposePlan plan) {
        HouseholdItem item = findById(id);
        if (item != null) {
            item.setDisposePlan(plan);
            saveToFile();
            return true;
        }
        return false;
    }

    public synchronized boolean delete(String id) {
        Iterator<HouseholdItem> it = items.iterator();
        while (it.hasNext()) {
            if (it.next().getId().equals(id)) {
                it.remove();
                saveToFile();
                return true;
            }
        }
        return false;
    }

    private void loadFromFile() {
        if (!Files.exists(dataFilePath)) {
            return;
        }
        try {
            String content = new String(Files.readAllBytes(dataFilePath), StandardCharsets.UTF_8);
            List<HouseholdItem> loaded = parseItemsArray(content);
            items.clear();
            items.addAll(loaded);
        } catch (Exception e) {
            System.err.println("Failed to load items from file: " + e.getMessage());
        }
    }

    private synchronized void saveToFile() {
        try {
            String json = serializeItems(items);
            if (!Files.exists(dataFilePath.getParent())) {
                Files.createDirectories(dataFilePath.getParent());
            }
            Files.write(dataFilePath, json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("Failed to save items to file: " + e.getMessage());
        }
    }

    private static String escapeJson(String s) {
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

    private static String serializeItems(List<HouseholdItem> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            HouseholdItem it = list.get(i);
            sb.append("{");
            sb.append("\"id\":\"").append(escapeJson(it.getId())).append("\",");
            sb.append("\"name\":\"").append(escapeJson(it.getName())).append("\",");
            sb.append("\"category\":\"").append(escapeJson(it.getCategory())).append("\",");
            sb.append("\"disposePlan\":\"").append(it.getDisposePlan().name()).append("\",");
            sb.append("\"estimatedPrice\":").append(it.getEstimatedPrice() != null ? it.getEstimatedPrice().toPlainString() : "0").append(",");
            sb.append("\"location\":\"").append(escapeJson(it.getLocation())).append("\",");
            sb.append("\"remark\":\"").append(escapeJson(it.getRemark())).append("\"");
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String serializeSingle(HouseholdItem it) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(escapeJson(it.getId())).append("\",");
        sb.append("\"name\":\"").append(escapeJson(it.getName())).append("\",");
        sb.append("\"category\":\"").append(escapeJson(it.getCategory())).append("\",");
        sb.append("\"disposePlan\":\"").append(it.getDisposePlan().name()).append("\",");
        sb.append("\"disposePlanDisplay\":\"").append(it.getDisposePlan().getDisplayName()).append("\",");
        sb.append("\"estimatedPrice\":").append(it.getEstimatedPrice() != null ? it.getEstimatedPrice().toPlainString() : "0").append(",");
        sb.append("\"location\":\"").append(escapeJson(it.getLocation())).append("\",");
        sb.append("\"remark\":\"").append(escapeJson(it.getRemark())).append("\"");
        sb.append("}");
        return sb.toString();
    }

    public static String serializeList(List<HouseholdItem> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(serializeSingle(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static List<HouseholdItem> parseItemsArray(String json) {
        List<HouseholdItem> result = new ArrayList<>();
        json = json.trim();
        if (json.isEmpty() || json.equals("[]")) return result;
        if (!json.startsWith("[") || !json.endsWith("]")) return result;
        json = json.substring(1, json.length() - 1);
        List<String> objStrs = splitJsonObjects(json);
        for (String objStr : objStrs) {
            HouseholdItem item = parseItem(objStr);
            if (item != null) result.add(item);
        }
        return result;
    }

    private static List<String> splitJsonObjects(String arrContent) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        boolean inString = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < arrContent.length(); i++) {
            char c = arrContent.charAt(i);
            if (c == '"' && (i == 0 || arrContent.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    current.append(c);
                    if (depth == 0) {
                        result.add(current.toString());
                        current = new StringBuilder();
                        continue;
                    }
                } else if (c == ',' && depth == 0) {
                    continue;
                }
            }
            current.append(c);
        }
        return result;
    }

    private static HouseholdItem parseItem(String objStr) {
        try {
            Map<String, String> fields = parseJsonObject(objStr);
            HouseholdItem item = new HouseholdItem();
            if (fields.containsKey("id")) item.setId(fields.get("id"));
            item.setName(fields.getOrDefault("name", ""));
            item.setCategory(fields.getOrDefault("category", ""));
            String dp = fields.getOrDefault("disposePlan", "KEEP");
            item.setDisposePlan(DisposePlan.valueOf(dp));
            String price = fields.getOrDefault("estimatedPrice", "0");
            item.setEstimatedPrice(new BigDecimal(price));
            item.setLocation(fields.getOrDefault("location", ""));
            item.setRemark(fields.getOrDefault("remark", ""));
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    public static Map<String, String> parseJsonObject(String objStr) {
        Map<String, String> map = new HashMap<>();
        objStr = objStr.trim();
        if (objStr.startsWith("{") && objStr.endsWith("}")) {
            objStr = objStr.substring(1, objStr.length() - 1);
        }
        boolean inString = false;
        int depth = 0;
        String key = null;
        StringBuilder buf = new StringBuilder();
        boolean parsingKey = true;

        for (int i = 0; i < objStr.length(); i++) {
            char c = objStr.charAt(i);
            if (c == '"' && (i == 0 || objStr.charAt(i - 1) != '\\')) {
                if (!inString) {
                    inString = true;
                } else {
                    inString = false;
                }
                continue;
            }
            if (!inString) {
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') depth--;
                else if (c == ':' && depth == 0 && parsingKey) {
                    key = unescapeJson(buf.toString().trim());
                    buf = new StringBuilder();
                    parsingKey = false;
                    continue;
                } else if (c == ',' && depth == 0 && !parsingKey) {
                    map.put(key, unescapeJson(buf.toString().trim()));
                    buf = new StringBuilder();
                    parsingKey = true;
                    continue;
                }
            }
            if (inString || c != ' ') {
                buf.append(c);
            } else if (buf.length() > 0) {
                buf.append(c);
            }
        }
        if (key != null && buf.length() > 0) {
            map.put(key, unescapeJson(buf.toString().trim()));
        }
        return map;
    }

    private static String unescapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"': sb.append('"'); i++; break;
                    case '\\': sb.append('\\'); i++; break;
                    case 'n': sb.append('\n'); i++; break;
                    case 'r': sb.append('\r'); i++; break;
                    case 't': sb.append('\t'); i++; break;
                    case 'u':
                        if (i + 5 < s.length()) {
                            String hex = s.substring(i + 2, i + 6);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 5;
                        }
                        break;
                    default: sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
