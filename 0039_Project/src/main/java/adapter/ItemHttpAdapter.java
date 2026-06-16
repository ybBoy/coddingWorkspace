package adapter;

import app.DeclutterService;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.DisposePlan;
import model.HouseholdItem;
import model.ItemStatus;
import persist.ItemJsonStore;

import java.io.*;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ItemHttpAdapter implements HttpHandler {
    private final DeclutterService service;
    private final String uiDir;
    private final String uploadDir;

    public ItemHttpAdapter(DeclutterService service, String uiDir, String uploadDir) {
        this.service = service;
        this.uiDir = uiDir;
        this.uploadDir = uploadDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            URI uri = exchange.getRequestURI();
            String path = uri.getPath();

            if (path.startsWith("/api/")) {
                handleApi(exchange, method, path);
            } else if (path.startsWith("/uploads/")) {
                handleUpload(exchange, path);
            } else {
                handleStatic(exchange, path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private void handleApi(HttpExchange exchange, String method, String path) throws IOException {
        if ("GET".equals(method) && "/api/items".equals(path)) {
            handleListItems(exchange);
        } else if ("POST".equals(method) && "/api/items".equals(path)) {
            handleAddItem(exchange);
        } else if ("PUT".equals(method) && path.startsWith("/api/items/") && path.endsWith("/dispose-plan")) {
            handleUpdateDisposePlan(exchange, path);
        } else if ("PUT".equals(method) && path.startsWith("/api/items/") && path.endsWith("/status")) {
            handleUpdateStatus(exchange, path);
        } else if ("PUT".equals(method) && path.startsWith("/api/items/") && path.endsWith("/image")) {
            handleUpdateImage(exchange, path);
        } else if ("DELETE".equals(method) && path.startsWith("/api/items/")) {
            handleDeleteItem(exchange, path);
        } else if ("POST".equals(method) && "/api/items/batch/dispose-plan".equals(path)) {
            handleBatchUpdateDisposePlan(exchange);
        } else if ("POST".equals(method) && "/api/items/batch/status".equals(path)) {
            handleBatchUpdateStatus(exchange);
        } else if ("POST".equals(method) && "/api/items/batch/delete".equals(path)) {
            handleBatchDelete(exchange);
        } else if ("GET".equals(method) && "/api/stats".equals(path)) {
            handleStats(exchange);
        } else if ("GET".equals(method) && "/api/stats/detailed".equals(path)) {
            handleDetailedStats(exchange);
        } else if ("GET".equals(method) && "/api/export/csv".equals(path)) {
            handleExportCsv(exchange);
        } else if ("GET".equals(method) && "/api/export/json".equals(path)) {
            handleExportJson(exchange);
        } else if ("POST".equals(method) && "/api/upload".equals(path)) {
            handleUploadImage(exchange);
        } else if ("OPTIONS".equals(method)) {
            handleOptions(exchange);
        } else {
            sendError(exchange, 404, "Not Found");
        }
    }

    private void handleListItems(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
        String keyword = query.get("keyword");
        String category = query.get("category");
        String disposePlan = query.get("disposePlan");
        String status = query.get("status");
        String minPriceStr = query.get("minPrice");
        String maxPriceStr = query.get("maxPrice");
        String sortBy = query.get("sortBy");
        String sortOrder = query.get("sortOrder");

        BigDecimal minPrice = null, maxPrice = null;
        try { if (minPriceStr != null && !minPriceStr.isEmpty()) minPrice = new BigDecimal(minPriceStr); } catch (Exception ignored) {}
        try { if (maxPriceStr != null && !maxPriceStr.isEmpty()) maxPrice = new BigDecimal(maxPriceStr); } catch (Exception ignored) {}

        List<HouseholdItem> items = service.searchItems(
                keyword, category, disposePlan, status,
                minPrice, maxPrice, sortBy, sortOrder);
        sendJson(exchange, 200, ItemJsonStore.serializeList(items));
    }

    private void handleAddItem(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, String> fields = ItemJsonStore.parseJsonObject(body);
        try {
            HouseholdItem item = new HouseholdItem();
            item.setName(fields.getOrDefault("name", ""));
            item.setCategory(fields.getOrDefault("category", ""));
            String dp = fields.getOrDefault("disposePlan", "KEEP");
            item.setDisposePlan(DisposePlan.fromNameOrDisplayName(dp));
            String statusStr = fields.get("status");
            if (statusStr != null && !statusStr.isEmpty()) {
                item.setStatus(ItemStatus.fromNameOrDisplayName(statusStr));
            }
            String priceStr = fields.getOrDefault("estimatedPrice", "0");
            item.setEstimatedPrice(new BigDecimal(priceStr));
            item.setLocation(fields.getOrDefault("location", ""));
            item.setImageUrl(fields.getOrDefault("imageUrl", ""));
            item.setRemark(fields.getOrDefault("remark", ""));
            service.addItem(item);
            sendJson(exchange, 201, ItemJsonStore.serializeSingle(item));
        } catch (Exception e) {
            sendError(exchange, 400, "Bad Request: " + e.getMessage());
        }
    }

    private void handleUpdateDisposePlan(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path, "/api/items/", "/dispose-plan");
        String body = readBody(exchange);
        Map<String, String> fields = ItemJsonStore.parseJsonObject(body);
        try {
            String dp = fields.getOrDefault("disposePlan", "");
            DisposePlan plan = DisposePlan.fromNameOrDisplayName(dp);
            boolean ok = service.updateDisposePlan(id, plan);
            if (ok) sendJson(exchange, 200, "{\"success\":true}");
            else sendError(exchange, 404, "Item not found");
        } catch (Exception e) {
            sendError(exchange, 400, "Bad Request: " + e.getMessage());
        }
    }

    private void handleUpdateStatus(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path, "/api/items/", "/status");
        String body = readBody(exchange);
        Map<String, String> fields = ItemJsonStore.parseJsonObject(body);
        try {
            String st = fields.getOrDefault("status", "");
            ItemStatus status = ItemStatus.fromNameOrDisplayName(st);
            HouseholdItem item = service.findItem(id);
            if (item == null) {
                sendError(exchange, 404, "Item not found");
                return;
            }
            if (status != null && !status.isCompatibleWith(item.getDisposePlan())) {
                sendError(exchange, 400, "状态 " + status.getDisplayName() + " 与处理方式 " + item.getDisposePlan().getDisplayName() + " 不兼容");
                return;
            }
            boolean ok = service.updateStatus(id, status);
            if (ok) sendJson(exchange, 200, "{\"success\":true}");
            else sendError(exchange, 500, "Update failed");
        } catch (Exception e) {
            sendError(exchange, 400, "Bad Request: " + e.getMessage());
        }
    }

    private void handleUpdateImage(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path, "/api/items/", "/image");
        String body = readBody(exchange);
        Map<String, String> fields = ItemJsonStore.parseJsonObject(body);
        String imageUrl = fields.getOrDefault("imageUrl", "");
        boolean ok = service.updateImageUrl(id, imageUrl);
        if (ok) sendJson(exchange, 200, "{\"success\":true}");
        else sendError(exchange, 404, "Item not found");
    }

    private void handleDeleteItem(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path, "/api/items/", null);
        boolean ok = service.deleteItem(id);
        if (ok) sendJson(exchange, 200, "{\"success\":true}");
        else sendError(exchange, 404, "Item not found");
    }

    private List<String> parseIdList(String body) {
        Map<String, String> fields = ItemJsonStore.parseJsonObject(body);
        String idsStr = fields.get("ids");
        if (idsStr == null || idsStr.isEmpty()) return Collections.emptyList();
        if (idsStr.startsWith("[") && idsStr.endsWith("]")) {
            idsStr = idsStr.substring(1, idsStr.length() - 1);
        }
        List<String> result = new ArrayList<>();
        for (String s : idsStr.split(",")) {
            String t = s.trim();
            if (t.startsWith("\"") && t.endsWith("\"")) {
                t = t.substring(1, t.length() - 1);
            }
            if (!t.isEmpty()) result.add(t);
        }
        return result;
    }

    private static List<String> parseJsonStringArray(String arr) {
        List<String> result = new ArrayList<>();
        arr = arr.trim();
        if (arr.equals("[]")) return result;
        arr = arr.substring(1, arr.length() - 1);
        boolean inString = false;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (c == '"' && (i == 0 || arr.charAt(i - 1) != '\\')) {
                if (!inString) { inString = true; continue; }
                else { inString = false; result.add(cur.toString()); cur = new StringBuilder(); continue; }
            }
            if (inString) cur.append(c);
            else if (c == ',') continue;
        }
        return result;
    }

    private void handleBatchUpdateDisposePlan(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        try {
            List<String> ids = parseIdList(body);
            Map<String, String> fields = ItemJsonStore.parseJsonObject(body);
            String dp = fields.getOrDefault("disposePlan", "");
            DisposePlan plan = DisposePlan.fromNameOrDisplayName(dp);
            int count = service.batchUpdateDisposePlan(ids, plan);
            sendJson(exchange, 200, "{\"success\":true,\"updated\":" + count + "}");
        } catch (Exception e) {
            sendError(exchange, 400, "Bad Request: " + e.getMessage());
        }
    }

    private void handleBatchUpdateStatus(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        try {
            List<String> ids = parseIdList(body);
            Map<String, String> fields = ItemJsonStore.parseJsonObject(body);
            String st = fields.getOrDefault("status", "");
            ItemStatus status = ItemStatus.fromNameOrDisplayName(st);
            int count = service.batchUpdateStatus(ids, status);
            sendJson(exchange, 200, "{\"success\":true,\"updated\":" + count + "}");
        } catch (Exception e) {
            sendError(exchange, 400, "Bad Request: " + e.getMessage());
        }
    }

    private void handleBatchDelete(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        try {
            List<String> ids = parseIdList(body);
            int count = service.batchDelete(ids);
            sendJson(exchange, 200, "{\"success\":true,\"deleted\":" + count + "}");
        } catch (Exception e) {
            sendError(exchange, 400, "Bad Request: " + e.getMessage());
        }
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        BigDecimal revenue = service.calculateExpectedRevenue();
        BigDecimal soldRevenue = service.calculateSoldRevenue();
        Set<String> categories = service.listCategories();
        int total = service.countItems();
        int keepCount = service.countByDisposePlan(DisposePlan.KEEP);
        int giveCount = service.countByDisposePlan(DisposePlan.GIVE_AWAY);
        int sellCount = service.countByDisposePlan(DisposePlan.SELL);
        int discardCount = service.countByDisposePlan(DisposePlan.DISCARD);

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"expectedRevenue\":").append(revenue.toPlainString()).append(",");
        sb.append("\"soldRevenue\":").append(soldRevenue.toPlainString()).append(",");
        sb.append("\"totalItems\":").append(total).append(",");
        sb.append("\"keepCount\":").append(keepCount).append(",");
        sb.append("\"giveAwayCount\":").append(giveCount).append(",");
        sb.append("\"sellCount\":").append(sellCount).append(",");
        sb.append("\"discardCount\":").append(discardCount).append(",");
        sb.append("\"categories\":[");
        int i = 0;
        for (String c : categories) {
            if (i++ > 0) sb.append(",");
            sb.append("\"").append(escapeJson(c)).append("\"");
        }
        sb.append("]");
        sb.append("}");
        sendJson(exchange, 200, sb.toString());
    }

    private void handleDetailedStats(HttpExchange exchange) throws IOException {
        Map<String, Object> stats = service.getDetailedStats();
        String json = serializeObject(stats);
        sendJson(exchange, 200, json);
    }

    @SuppressWarnings("unchecked")
    private static String serializeObject(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
                sb.append(serializeObject(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(serializeObject(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj instanceof Set) {
            return serializeObject(new ArrayList<>((Set<?>) obj));
        }
        return "\"" + escapeJson(String.valueOf(obj)) + "\"";
    }

    private void handleExportCsv(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
        String keyword = query.get("keyword");
        String category = query.get("category");
        String disposePlan = query.get("disposePlan");
        String status = query.get("status");
        String minPriceStr = query.get("minPrice");
        String maxPriceStr = query.get("maxPrice");
        String sortBy = query.getOrDefault("sortBy", "createdAt");
        String sortOrder = query.getOrDefault("sortOrder", "desc");

        BigDecimal minPrice = null, maxPrice = null;
        try { if (minPriceStr != null && !minPriceStr.isEmpty()) minPrice = new BigDecimal(minPriceStr); } catch (Exception ignored) {}
        try { if (maxPriceStr != null && !maxPriceStr.isEmpty()) maxPrice = new BigDecimal(maxPriceStr); } catch (Exception ignored) {}

        List<HouseholdItem> items = service.searchItems(
                keyword, category, disposePlan, status, minPrice, maxPrice, sortBy, sortOrder);

        String csv = service.exportCsv(items);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "text/csv; charset=utf-8");
        headers.set("Content-Disposition", "attachment; filename=\"declutter-items.csv\"");
        addCorsHeaders(headers);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private void handleExportJson(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
        String keyword = query.get("keyword");
        String category = query.get("category");
        String disposePlan = query.get("disposePlan");
        String status = query.get("status");
        String minPriceStr = query.get("minPrice");
        String maxPriceStr = query.get("maxPrice");
        String sortBy = query.getOrDefault("sortBy", "createdAt");
        String sortOrder = query.getOrDefault("sortOrder", "desc");

        BigDecimal minPrice = null, maxPrice = null;
        try { if (minPriceStr != null && !minPriceStr.isEmpty()) minPrice = new BigDecimal(minPriceStr); } catch (Exception ignored) {}
        try { if (maxPriceStr != null && !maxPriceStr.isEmpty()) maxPrice = new BigDecimal(maxPriceStr); } catch (Exception ignored) {}

        List<HouseholdItem> items = service.searchItems(
                keyword, category, disposePlan, status, minPrice, maxPrice, sortBy, sortOrder);

        String json = service.exportJson(items);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("Content-Disposition", "attachment; filename=\"declutter-items.json\"");
        addCorsHeaders(headers);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private void handleUploadImage(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, String> fields = ItemJsonStore.parseJsonObject(body);
        String dataUrl = fields.get("image");
        if (dataUrl == null || dataUrl.isEmpty()) {
            dataUrl = fields.get("imageBase64");
        }
        if (dataUrl == null || !dataUrl.startsWith("data:image/")) {
            sendError(exchange, 400, "Invalid image data");
            return;
        }
        try {
            int commaIdx = dataUrl.indexOf(',');
            if (commaIdx < 0) throw new IllegalArgumentException("Invalid data URL");
            String mimePart = dataUrl.substring(5, commaIdx);
            String base64 = dataUrl.substring(commaIdx + 1);
            String ext = "png";
            if (mimePart.contains("jpeg") || mimePart.contains("jpg")) ext = "jpg";
            else if (mimePart.contains("png")) ext = "png";
            else if (mimePart.contains("gif")) ext = "gif";
            else if (mimePart.contains("webp")) ext = "webp";

            byte[] imageBytes = Base64.getDecoder().decode(base64);

            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String fileName = UUID.randomUUID().toString() + "." + ext;
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, imageBytes);

            String url = "/uploads/" + fileName;
            sendJson(exchange, 200, "{\"success\":true,\"url\":\"" + url + "\"}");
        } catch (Exception e) {
            sendError(exchange, 400, "Upload failed: " + e.getMessage());
        }
    }

    private void handleUpload(HttpExchange exchange, String path) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method Not Allowed");
            return;
        }
        String fileName = path.substring("/uploads/".length());
        Path filePath = Paths.get(uploadDir, fileName);
        if (!filePath.normalize().startsWith(Paths.get(uploadDir).normalize())) {
            sendError(exchange, 403, "Forbidden");
            return;
        }
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            sendError(exchange, 404, "Not Found");
            return;
        }
        byte[] content = Files.readAllBytes(filePath);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", guessContentType(fileName));
        addCorsHeaders(headers);
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(content); }
    }

    private void handleOptions(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        addCorsHeaders(headers);
        exchange.sendResponseHeaders(204, -1);
    }

    private void handleStatic(HttpExchange exchange, String path) throws IOException {
        if ("/".equals(path) || path.isEmpty()) {
            path = "/index.html";
        }
        Path filePath = Paths.get(uiDir, path.substring(1));
        if (!filePath.normalize().startsWith(Paths.get(uiDir).normalize())) {
            sendError(exchange, 403, "Forbidden");
            return;
        }
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            sendError(exchange, 404, "Not Found");
            return;
        }
        String contentType = guessContentType(filePath.getFileName().toString());
        byte[] content = Files.readAllBytes(filePath);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        addCorsHeaders(headers);
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(content); }
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        addCorsHeaders(headers);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        String json = "{\"error\":\"" + escapeJson(message) + "\"}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        addCorsHeaders(headers);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private static void addCorsHeaders(Headers headers) {
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                baos.write(buf, 0, n);
            }
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) return result;
        try {
            for (String pair : query.split("&")) {
                int idx = pair.indexOf('=');
                if (idx > 0) {
                    String key = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                    String value = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                    result.put(key, value);
                }
            }
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private static String extractId(String path, String prefix, String suffix) {
        String result = path;
        if (prefix != null && result.startsWith(prefix)) {
            result = result.substring(prefix.length());
        }
        if (suffix != null && result.endsWith(suffix)) {
            result = result.substring(0, result.length() - suffix.length());
        }
        return result;
    }

    private static String guessContentType(String filename) {
        if (filename.endsWith(".html")) return "text/html; charset=utf-8";
        if (filename.endsWith(".css")) return "text/css; charset=utf-8";
        if (filename.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (filename.endsWith(".json")) return "application/json; charset=utf-8";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".gif")) return "image/gif";
        if (filename.endsWith(".webp")) return "image/webp";
        if (filename.endsWith(".svg")) return "image/svg+xml";
        if (filename.endsWith(".csv")) return "text/csv; charset=utf-8";
        return "application/octet-stream";
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
}
