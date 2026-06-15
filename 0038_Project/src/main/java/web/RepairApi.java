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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RepairApi implements HttpHandler {
    private static final int MAX_ITEM_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final int MAX_REMARK_LENGTH = 1000;
    private static final BigDecimal MAX_COST = new BigDecimal("999999.99");

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
                sendError(exchange, 404, "Not Found");
            }
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            System.err.println("Internal error: " + e.getMessage());
            e.printStackTrace();
            sendError(exchange, 500, "Internal Server Error");
        }
    }

    private void handleGetAll(HttpExchange exchange) throws IOException {
        List<RepairItem> items = manager.getAllItems();
        sendJsonResponse(exchange, 200, items);
    }

    private void handleGetByStatus(HttpExchange exchange, URI uri) throws IOException {
        String query = uri.getQuery();
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("Missing status parameter");
        }

        String statusStr = null;
        String[] params = query.split("&");
        for (String param : params) {
            if (param.startsWith("status=")) {
                statusStr = URLDecoder.decode(param.substring(7), StandardCharsets.UTF_8.name());
                break;
            }
        }

        if (statusStr == null || statusStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing status parameter");
        }

        RepairStatus status;
        try {
            status = RepairStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + statusStr);
        }

        List<RepairItem> items = manager.getItemsByStatus(status);
        sendJsonResponse(exchange, 200, items);
    }

    private void handleGetById(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path);
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid item id");
        }
        Optional<RepairItem> item = manager.getItemById(id.trim());
        if (item.isPresent()) {
            sendJsonResponse(exchange, 200, item.get());
        } else {
            sendError(exchange, 404, "Item not found");
        }
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("Request body cannot be empty");
        }

        RepairItem item;
        try {
            item = objectMapper.readValue(body, RepairItem.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON format");
        }

        validateCreateItem(item);

        RepairItem saved = manager.addItem(item);
        sendJsonResponse(exchange, 201, saved);
    }

    private void handleUpdate(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path);
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid item id");
        }
        id = id.trim();

        if (!manager.getItemById(id).isPresent()) {
            sendError(exchange, 404, "Item not found");
            return;
        }

        String body = readBody(exchange);
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("Request body cannot be empty");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> updates;
        try {
            updates = objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON format");
        }

        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("No fields to update");
        }

        boolean hasValidField = false;
        boolean success = false;

        if (updates.containsKey("status")) {
            Object statusObj = updates.get("status");
            if (!(statusObj instanceof String)) {
                throw new IllegalArgumentException("Status must be a string");
            }
            String statusStr = ((String) statusObj).trim();
            if (statusStr.isEmpty()) {
                throw new IllegalArgumentException("Status cannot be empty");
            }
            RepairStatus status;
            try {
                status = RepairStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + statusStr);
            }
            success = manager.updateStatus(id, status);
            hasValidField = true;
        }

        if (updates.containsKey("remark")) {
            Object remarkObj = updates.get("remark");
            String remark = remarkObj == null ? "" : remarkObj.toString();
            if (remark.length() > MAX_REMARK_LENGTH) {
                throw new IllegalArgumentException("Remark exceeds maximum length of " + MAX_REMARK_LENGTH);
            }
            success = manager.updateRemark(id, remark) || success;
            hasValidField = true;
        }

        if (!hasValidField) {
            throw new IllegalArgumentException("No valid fields to update. Only 'status' and 'remark' are supported");
        }

        if (success) {
            Optional<RepairItem> updated = manager.getItemById(id);
            sendJsonResponse(exchange, 200, updated.get());
        } else {
            sendError(exchange, 404, "Item not found");
        }
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path);
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid item id");
        }
        boolean deleted = manager.deleteItem(id.trim());
        if (deleted) {
            Map<String, String> result = new HashMap<>();
            result.put("message", "Deleted successfully");
            sendJsonResponse(exchange, 200, result);
        } else {
            sendError(exchange, 404, "Item not found");
        }
    }

    private void handleGetStatistics(HttpExchange exchange) throws IOException {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCost", manager.getTotalCost());
        stats.put("pendingCount", manager.getPendingCount());
        stats.put("totalCount", manager.getAllItems().size());
        sendJsonResponse(exchange, 200, stats);
    }

    private void validateCreateItem(RepairItem item) {
        if (item.getItemName() == null || item.getItemName().trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be empty");
        }
        if (item.getItemName().trim().length() > MAX_ITEM_NAME_LENGTH) {
            throw new IllegalArgumentException("Item name exceeds maximum length of " + MAX_ITEM_NAME_LENGTH);
        }
        item.setItemName(item.getItemName().trim());

        if (item.getProblemDescription() == null || item.getProblemDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Problem description cannot be empty");
        }
        if (item.getProblemDescription().trim().length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Problem description exceeds maximum length of " + MAX_DESCRIPTION_LENGTH);
        }
        item.setProblemDescription(item.getProblemDescription().trim());

        if (item.getCost() == null) {
            item.setCost(BigDecimal.ZERO);
        }
        if (item.getCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cost cannot be negative");
        }
        if (item.getCost().compareTo(MAX_COST) > 0) {
            throw new IllegalArgumentException("Cost exceeds maximum value of " + MAX_COST);
        }

        if (item.getRemark() != null) {
            if (item.getRemark().length() > MAX_REMARK_LENGTH) {
                throw new IllegalArgumentException("Remark exceeds maximum length of " + MAX_REMARK_LENGTH);
            }
        }
    }

    private String extractId(String path) {
        String id = path.substring("/api/repairs/".length());
        return id;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        int totalRead = 0;
        int maxBodySize = 1024 * 1024;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            totalRead += nRead;
            if (totalRead > maxBodySize) {
                throw new IllegalArgumentException("Request body too large");
            }
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

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        String json = objectMapper.writeValueAsString(error);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, json.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }
}
