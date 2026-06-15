package web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import core.RepairManager;
import entity.RepairItem;
import entity.RepairStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RepairApi implements HttpHandler {
    private final RepairManager manager;
    private final ObjectMapper objectMapper;

    public RepairApi(RepairManager manager) {
        this.manager = manager;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        String method = exchange.getRequestMethod();

        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        URI uri = exchange.getRequestURI();
        String path = uri.getPath();

        try {
            if ("/api/repairs/statistics".equals(path) && "GET".equals(method)) {
                handleGetStatistics(exchange);
            } else if ("/api/repairs/filter".equals(path) && "GET".equals(method)) {
                handleGetByStatus(exchange, uri);
            } else if ("/api/repairs".equals(path) && "GET".equals(method)) {
                handleGetAll(exchange);
            } else if ("/api/repairs".equals(path) && "POST".equals(method)) {
                handleCreate(exchange);
            } else if (path.startsWith("/api/repairs/") && "GET".equals(method)) {
                handleGetById(exchange, path);
            } else if (path.startsWith("/api/repairs/") && "PUT".equals(method)) {
                handleUpdate(exchange, path);
            } else if (path.startsWith("/api/repairs/") && "DELETE".equals(method)) {
                handleDelete(exchange, path);
            } else {
                sendResponse(exchange, 404, "{\"error\": \"Not Found\"}");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void handleGetAll(HttpExchange exchange) throws IOException {
        List<RepairItem> items = manager.getAllItems();
        sendJsonResponse(exchange, 200, items);
    }

    private void handleGetByStatus(HttpExchange exchange, URI uri) throws IOException {
        String query = uri.getQuery();
        if (query != null && query.startsWith("status=")) {
            String statusStr = query.substring(7);
            try {
                RepairStatus status = RepairStatus.valueOf(statusStr.toUpperCase());
                List<RepairItem> items = manager.getItemsByStatus(status);
                sendJsonResponse(exchange, 200, items);
            } catch (IllegalArgumentException e) {
                sendResponse(exchange, 400, "{\"error\": \"Invalid status\"}");
            }
        } else {
            sendResponse(exchange, 400, "{\"error\": \"Missing status parameter\"}");
        }
    }

    private void handleGetById(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path);
        Optional<RepairItem> item = manager.getItemById(id);
        if (item.isPresent()) {
            sendJsonResponse(exchange, 200, item.get());
        } else {
            sendResponse(exchange, 404, "{\"error\": \"Item not found\"}");
        }
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        RepairItem item = objectMapper.readValue(body, RepairItem.class);
        RepairItem saved = manager.addItem(item);
        sendJsonResponse(exchange, 201, saved);
    }

    private void handleUpdate(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path);
        String body = readBody(exchange);

        @SuppressWarnings("unchecked")
        Map<String, Object> updates = objectMapper.readValue(body, Map.class);

        boolean success = false;

        if (updates.containsKey("status")) {
            String statusStr = (String) updates.get("status");
            RepairStatus status = RepairStatus.valueOf(statusStr.toUpperCase());
            success = manager.updateStatus(id, status);
        }

        if (updates.containsKey("remark")) {
            String remark = (String) updates.get("remark");
            success = manager.updateRemark(id, remark) || success;
        }

        if (success) {
            Optional<RepairItem> updated = manager.getItemById(id);
            sendJsonResponse(exchange, 200, updated.get());
        } else {
            sendResponse(exchange, 404, "{\"error\": \"Item not found\"}");
        }
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path);
        boolean deleted = manager.deleteItem(id);
        if (deleted) {
            sendResponse(exchange, 200, "{\"message\": \"Deleted successfully\"}");
        } else {
            sendResponse(exchange, 404, "{\"error\": \"Item not found\"}");
        }
    }

    private void handleGetStatistics(HttpExchange exchange) throws IOException {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCost", manager.getTotalCost());
        stats.put("pendingCount", manager.getPendingCount());
        stats.put("totalCount", manager.getAllItems().size());
        sendJsonResponse(exchange, 200, stats);
    }

    private String extractId(String path) {
        return path.substring("/api/repairs/".length());
    }

    private String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private void sendJsonResponse(HttpExchange exchange, int code, Object data) throws IOException {
        String json = objectMapper.writeValueAsString(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, json.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void sendResponse(HttpExchange exchange, int code, String message) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, message.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes(StandardCharsets.UTF_8));
        }
    }
}
