package web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import core.RepairManager;
import entity.ItemType;
import entity.RepairHistoryEntry;
import entity.RepairImage;
import entity.RepairItem;
import entity.RepairStatus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class RepairApi implements HttpHandler {
    private static final int MAX_ITEM_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final int MAX_REMARK_LENGTH = 1000;
    private static final BigDecimal MAX_COST = new BigDecimal("999999.99");
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final String UPLOAD_DIR = "uploads";
    private static final List<String> ALLOWED_IMAGE_EXT = new ArrayList<String>() {{
        add("jpg"); add("jpeg"); add("png"); add("gif"); add("webp");
    }};

    private final RepairManager manager;
    private final ObjectMapper objectMapper;

    public RepairApi(RepairManager manager) {
        this.manager = manager;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            System.err.println("Failed to create uploads directory: " + e.getMessage());
        }
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
            } else if ("/api/repairs/search".equals(path) && "GET".equals(method)) {
                handleSearch(exchange, uri);
            } else if ("/api/repairs/types".equals(path) && "GET".equals(method)) {
                handleGetTypes(exchange);
            } else if ("/api/repairs/filter".equals(path) && "GET".equals(method)) {
                handleGetByStatus(exchange, uri);
            } else if ("/api/repairs/export/csv".equals(path) && "GET".equals(method)) {
                handleExportCsv(exchange);
            } else if ("/api/repairs/export/json".equals(path) && "GET".equals(method)) {
                handleExportJson(exchange);
            } else if ("/api/repairs".equals(path) && "GET".equals(method)) {
                handleGetAll(exchange);
            } else if ("/api/repairs".equals(path) && "POST".equals(method)) {
                handleCreate(exchange);
            } else if (path.startsWith("/api/repairs/") && path.endsWith("/images") && "POST".equals(method)) {
                handleUploadImage(exchange, path);
            } else if (path.contains("/images/") && path.startsWith("/api/repairs/") && "DELETE".equals(method)) {
                handleDeleteImage(exchange, path);
            } else if (path.endsWith("/history") && path.startsWith("/api/repairs/") && "GET".equals(method)) {
                handleGetHistory(exchange, path);
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
        sendJsonResponse(exchange, 200, manager.getAllItems());
    }

    private void handleGetTypes(HttpExchange exchange) throws IOException {
        List<Map<String, String>> types = new ArrayList<>();
        for (ItemType type : ItemType.values()) {
            Map<String, String> t = new HashMap<>();
            t.put("value", type.name());
            t.put("label", type.getDisplayName());
            types.add(t);
        }
        sendJsonResponse(exchange, 200, types);
    }

    private void handleSearch(HttpExchange exchange, URI uri) throws IOException {
        Map<String, String> params = parseQuery(uri);
        String keyword = params.get("keyword");
        RepairStatus status = null;
        ItemType type = null;

        String statusStr = params.get("status");
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                status = RepairStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + statusStr);
            }
        }

        String typeStr = params.get("type");
        if (typeStr != null && !typeStr.trim().isEmpty()) {
            try {
                type = ItemType.valueOf(typeStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid type: " + typeStr);
            }
        }

        sendJsonResponse(exchange, 200, manager.searchItems(keyword, status, type));
    }

    private void handleGetByStatus(HttpExchange exchange, URI uri) throws IOException {
        Map<String, String> params = parseQuery(uri);
        String statusStr = params.get("status");
        if (statusStr == null || statusStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing status parameter");
        }
        RepairStatus status;
        try {
            status = RepairStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + statusStr);
        }
        sendJsonResponse(exchange, 200, manager.getItemsByStatus(status));
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

    private void handleGetHistory(HttpExchange exchange, String path) throws IOException {
        String id = path.substring("/api/repairs/".length(), path.length() - "/history".length());
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid item id");
        }
        Optional<RepairItem> item = manager.getItemById(id.trim());
        if (!item.isPresent()) {
            sendError(exchange, 404, "Item not found");
            return;
        }
        List<RepairHistoryEntry> history = item.get().getHistory();
        sendJsonResponse(exchange, 200, history);
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

        if (item.getItemType() == null) {
            item.setItemType(ItemType.OTHER);
        }

        RepairItem saved = manager.addItem(item);
        sendJsonResponse(exchange, 201, saved);
    }

    private void handleUpdate(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path);
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid item id");
        }
        id = id.trim();

        Optional<RepairItem> existingOpt = manager.getItemById(id);
        if (!existingOpt.isPresent()) {
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

        RepairItem existing = existingOpt.get();
        boolean hasValidField = false;

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
            existing.setStatus(status);
            hasValidField = true;
        }

        if (updates.containsKey("remark")) {
            Object remarkObj = updates.get("remark");
            String remark = remarkObj == null ? "" : remarkObj.toString();
            if (remark.length() > MAX_REMARK_LENGTH) {
                throw new IllegalArgumentException("Remark exceeds maximum length of " + MAX_REMARK_LENGTH);
            }
            existing.setRemark(remark);
            hasValidField = true;
        }

        if (updates.containsKey("itemType")) {
            Object typeObj = updates.get("itemType");
            if (typeObj == null || !(typeObj instanceof String) || ((String) typeObj).trim().isEmpty()) {
                existing.setItemType(ItemType.OTHER);
            } else {
                String typeStr = ((String) typeObj).trim().toUpperCase();
                try {
                    existing.setItemType(ItemType.valueOf(typeStr));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid type: " + typeStr);
                }
            }
            hasValidField = true;
        }

        if (updates.containsKey("cost")) {
            Object costObj = updates.get("cost");
            BigDecimal cost;
            try {
                if (costObj instanceof Number) {
                    cost = new BigDecimal(costObj.toString());
                } else if (costObj instanceof String) {
                    cost = new BigDecimal(((String) costObj).trim());
                } else {
                    throw new IllegalArgumentException("Invalid cost");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid cost value");
            }
            if (cost.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Cost cannot be negative");
            }
            if (cost.compareTo(MAX_COST) > 0) {
                throw new IllegalArgumentException("Cost exceeds maximum value of " + MAX_COST);
            }
            existing.setCost(cost);
            hasValidField = true;
        }

        if (updates.containsKey("itemName")) {
            Object nameObj = updates.get("itemName");
            if (nameObj == null || !(nameObj instanceof String) || ((String) nameObj).trim().isEmpty()) {
                throw new IllegalArgumentException("Item name cannot be empty");
            }
            String name = ((String) nameObj).trim();
            if (name.length() > MAX_ITEM_NAME_LENGTH) {
                throw new IllegalArgumentException("Item name exceeds maximum length of " + MAX_ITEM_NAME_LENGTH);
            }
            existing.setItemName(name);
            hasValidField = true;
        }

        if (updates.containsKey("problemDescription")) {
            Object descObj = updates.get("problemDescription");
            if (descObj == null || !(descObj instanceof String) || ((String) descObj).trim().isEmpty()) {
                throw new IllegalArgumentException("Problem description cannot be empty");
            }
            String desc = ((String) descObj).trim();
            if (desc.length() > MAX_DESCRIPTION_LENGTH) {
                throw new IllegalArgumentException("Problem description exceeds maximum length of " + MAX_DESCRIPTION_LENGTH);
            }
            existing.setProblemDescription(desc);
            hasValidField = true;
        }

        if (!hasValidField) {
            throw new IllegalArgumentException("No valid fields to update");
        }

        manager.saveAll();
        sendJsonResponse(exchange, 200, existing);
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
        sendJsonResponse(exchange, 200, manager.getAllStatistics());
    }

    private void handleExportCsv(HttpExchange exchange) throws IOException {
        String csv = manager.exportToCsv();
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=repairs.csv");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void handleExportJson(HttpExchange exchange) throws IOException {
        String json = objectMapper.writeValueAsString(manager.getAllItems());
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=repairs.json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void handleUploadImage(HttpExchange exchange, String path) throws IOException {
        String id = path.substring("/api/repairs/".length(), path.length() - "/images".length());
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid item id");
        }
        id = id.trim();
        if (!manager.getItemById(id).isPresent()) {
            sendError(exchange, 404, "Item not found");
            return;
        }

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.startsWith("multipart/form-data")) {
            throw new IllegalArgumentException("Content-Type must be multipart/form-data");
        }

        // 解析 multipart/form-data
        String boundary = "--" + extractBoundary(contentType);
        byte[] body = readBodyBytes(exchange);

        String bodyStr = new String(body, StandardCharsets.ISO_8859_1);
        int boundaryIdx = bodyStr.indexOf(boundary);
        if (boundaryIdx < 0) {
            throw new IllegalArgumentException("Invalid multipart data");
        }
        int partStart = bodyStr.indexOf("\r\n\r\n", boundaryIdx);
        if (partStart < 0) {
            throw new IllegalArgumentException("Invalid multipart data");
        }
        partStart += 4;
        int partEnd = bodyStr.indexOf("\r\n" + boundary, partStart);
        if (partEnd < 0) {
            throw new IllegalArgumentException("Invalid multipart data");
        }

        // 获取文件名
        String headerPart = bodyStr.substring(boundaryIdx, partStart - 4);
        String filename = extractFilename(headerPart);
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("No filename in upload");
        }
        String ext = getExtension(filename).toLowerCase();
        if (!ALLOWED_IMAGE_EXT.contains(ext)) {
            throw new IllegalArgumentException("Only image files allowed (jpg/png/gif/webp)");
        }

        byte[] fileData = new byte[partEnd - partStart];
        System.arraycopy(body, partStart, fileData, 0, fileData.length);
        if (fileData.length > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Image exceeds 10MB");
        }

        String imgId = UUID.randomUUID().toString();
        String storedName = imgId + "." + ext;
        Path uploadPath = Paths.get(UPLOAD_DIR);
        Files.createDirectories(uploadPath);
        Path target = uploadPath.resolve(storedName).normalize();
        if (!target.startsWith(uploadPath.normalize())) {
            throw new IllegalArgumentException("Invalid path");
        }
        try (FileOutputStream fos = new FileOutputStream(target.toFile())) {
            fos.write(fileData);
        }

        RepairImage img = new RepairImage(imgId, filename, "/uploads/" + storedName, null);
        manager.addImageToItem(id, img);

        sendJsonResponse(exchange, 201, img);
    }

    private void handleDeleteImage(HttpExchange exchange, String path) throws IOException {
        String rest = path.substring("/api/repairs/".length());
        int slash = rest.indexOf("/images/");
        if (slash < 0) {
            throw new IllegalArgumentException("Invalid path");
        }
        String id = rest.substring(0, slash).trim();
        String imageId = rest.substring(slash + "/images/".length()).trim();
        if (id.isEmpty() || imageId.isEmpty()) {
            throw new IllegalArgumentException("Invalid parameters");
        }
        if (!manager.getItemById(id).isPresent()) {
            sendError(exchange, 404, "Item not found");
            return;
        }
        Optional<RepairItem> item = manager.getItemById(id);
        if (item.isPresent()) {
            RepairImage toRemove = null;
            for (RepairImage img : item.get().getImages()) {
                if (imageId.equals(img.getId())) {
                    toRemove = img;
                    break;
                }
            }
            if (toRemove != null && toRemove.getFilePath() != null) {
                try {
                    String fp = toRemove.getFilePath();
                    if (fp.startsWith("/")) fp = fp.substring(1);
                    Path p = Paths.get(fp).normalize();
                    Path uploadRoot = Paths.get(UPLOAD_DIR).normalize();
                    if (p.startsWith(uploadRoot)) {
                        Files.deleteIfExists(p);
                    }
                } catch (IOException e) {
                    System.err.println("Failed to delete image file: " + e.getMessage());
                }
            }
        }
        boolean removed = manager.removeImageFromItem(id, imageId);
        if (removed) {
            Map<String, String> r = new HashMap<>();
            r.put("message", "Image deleted");
            sendJsonResponse(exchange, 200, r);
        } else {
            sendError(exchange, 404, "Image not found");
        }
    }

    private String extractBoundary(String contentType) {
        for (String part : contentType.split(";")) {
            part = part.trim();
            if (part.startsWith("boundary=")) {
                String b = part.substring("boundary=".length());
                if (b.startsWith("\"") && b.endsWith("\"") && b.length() > 2) {
                    b = b.substring(1, b.length() - 1);
                }
                return b;
            }
        }
        return null;
    }

    private String extractFilename(String headerPart) {
        int idx = headerPart.indexOf("filename=\"");
        if (idx < 0) return null;
        int start = idx + "filename=\"".length();
        int end = headerPart.indexOf("\"", start);
        if (end < 0) return null;
        String fn = headerPart.substring(start, end);
        int slash = Math.max(fn.lastIndexOf('/'), fn.lastIndexOf('\\'));
        if (slash >= 0) fn = fn.substring(slash + 1);
        return fn;
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "";
        return filename.substring(dot + 1);
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

    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> params = new HashMap<>();
        String query = uri.getQuery();
        if (query == null || query.isEmpty()) return params;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String key = pair.substring(0, eq);
                String value = pair.substring(eq + 1);
                try {
                    params.put(key, URLDecoder.decode(value, StandardCharsets.UTF_8.name()));
                } catch (java.io.UnsupportedEncodingException e) {
                    params.put(key, value);
                }
            }
        }
        return params;
    }

    private String extractId(String path) {
        return path.substring("/api/repairs/".length());
    }

    private String readBody(HttpExchange exchange) throws IOException {
        return new String(readBodyBytes(exchange), StandardCharsets.UTF_8);
    }

    private byte[] readBodyBytes(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        int totalRead = 0;
        int maxBodySize = 20 * 1024 * 1024;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            totalRead += nRead;
            if (totalRead > maxBodySize) {
                throw new IllegalArgumentException("Request body too large");
            }
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    private void sendJsonResponse(HttpExchange exchange, int code, Object data) throws IOException {
        String json = objectMapper.writeValueAsString(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        String json = objectMapper.writeValueAsString(error);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
