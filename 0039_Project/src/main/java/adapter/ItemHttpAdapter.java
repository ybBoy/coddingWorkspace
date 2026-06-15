package adapter;

import app.DeclutterService;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.DisposePlan;
import model.HouseholdItem;
import persist.ItemJsonStore;

import java.io.*;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ItemHttpAdapter implements HttpHandler {
    private final DeclutterService service;
    private final String uiDir;

    public ItemHttpAdapter(DeclutterService service, String uiDir) {
        this.service = service;
        this.uiDir = uiDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            URI uri = exchange.getRequestURI();
            String path = uri.getPath();

            if (path.startsWith("/api/")) {
                handleApi(exchange, method, path);
            } else {
                handleStatic(exchange, path);
            }
        } catch (Exception e) {
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
        } else if ("DELETE".equals(method) && path.startsWith("/api/items/")) {
            handleDeleteItem(exchange, path);
        } else if ("GET".equals(method) && "/api/stats".equals(path)) {
            handleStats(exchange);
        } else if ("OPTIONS".equals(method)) {
            handleOptions(exchange);
        } else {
            sendError(exchange, 404, "Not Found");
        }
    }

    private void handleListItems(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
        String category = query.get("category");
        String disposePlan = query.get("disposePlan");
        List<HouseholdItem> items = service.listItems(category, disposePlan);
        String json = ItemJsonStore.serializeList(items);
        sendJson(exchange, 200, json);
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
            String priceStr = fields.getOrDefault("estimatedPrice", "0");
            item.setEstimatedPrice(new BigDecimal(priceStr));
            item.setLocation(fields.getOrDefault("location", ""));
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
            if (ok) {
                sendJson(exchange, 200, "{\"success\":true}");
            } else {
                sendError(exchange, 404, "Item not found");
            }
        } catch (Exception e) {
            sendError(exchange, 400, "Bad Request: " + e.getMessage());
        }
    }

    private void handleDeleteItem(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path, "/api/items/", null);
        boolean ok = service.deleteItem(id);
        if (ok) {
            sendJson(exchange, 200, "{\"success\":true}");
        } else {
            sendError(exchange, 404, "Item not found");
        }
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        BigDecimal revenue = service.calculateExpectedRevenue();
        Set<String> categories = service.listCategories();
        int total = service.countItems();
        int keepCount = service.countByDisposePlan(DisposePlan.KEEP);
        int giveCount = service.countByDisposePlan(DisposePlan.GIVE_AWAY);
        int sellCount = service.countByDisposePlan(DisposePlan.SELL);
        int discardCount = service.countByDisposePlan(DisposePlan.DISCARD);

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"expectedRevenue\":").append(revenue.toPlainString()).append(",");
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
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
        }
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        addCorsHeaders(headers);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        String json = "{\"error\":\"" + escapeJson(message) + "\"}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        addCorsHeaders(headers);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void addCorsHeaders(Headers headers) {
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) {
                baos.write(buf, 0, n);
            }
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> result = new java.util.HashMap<>();
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
        if (filename.endsWith(".svg")) return "image/svg+xml";
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
