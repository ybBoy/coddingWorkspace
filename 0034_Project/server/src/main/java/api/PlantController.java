package api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import domain.CareLog;
import domain.CareType;
import domain.Plant;
import domain.PlantStatistics;
import domain.PlantStatus;
import service.PlantCareService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlantController {
    private final PlantCareService service;
    private final ObjectMapper objectMapper;
    private static final Set<String> VALID_CARE_TYPES = new HashSet<>(
            Arrays.asList("WATERING", "FERTILIZING", "PRUNING"));
    private static final Set<String> VALID_STATUSES = new HashSet<>(
            Arrays.asList("HEALTHY", "GROWING_WELL", "NEEDS_ATTENTION", "SICK", "DORMANT",
                    "健康", "生长良好", "需要关注", "生病", "休眠"));

    public PlantController(PlantCareService service) {
        this.service = service;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public class PlantsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                if ("GET".equals(method) && path.equals("/api/plants")) {
                    handleGetPlants(exchange);
                } else if ("GET".equals(method) && path.equals("/api/plants/sorted")) {
                    handleGetSortedPlants(exchange);
                } else if ("GET".equals(method) && path.equals("/api/plants/needing-water")) {
                    handleGetNeedingWater(exchange);
                } else if ("GET".equals(method) && path.equals("/api/plants/export")) {
                    handleExportPlants(exchange);
                } else if ("POST".equals(method) && path.equals("/api/plants/import")) {
                    handleImportPlants(exchange);
                } else if ("GET".equals(method) && path.equals("/api/statistics")) {
                    handleGetStatistics(exchange);
                } else if ("GET".equals(method) && path.matches("/api/plants/[^/]+/timeline")) {
                    handleGetTimeline(exchange, path);
                } else if ("GET".equals(method) && path.matches("/api/plants/[^/]+/care-logs/recent")) {
                    handleGetRecentCareLogs(exchange, path);
                } else if ("GET".equals(method) && path.matches("/api/plants/[^/]+")) {
                    handleGetPlant(exchange, path);
                } else if ("POST".equals(method) && path.equals("/api/plants")) {
                    handleCreatePlant(exchange);
                } else if ("PUT".equals(method) && path.matches("/api/plants/[^/]+/photo")) {
                    handleUpdatePhoto(exchange, path);
                } else if ("PUT".equals(method) && path.matches("/api/plants/[^/]+/status")) {
                    handleUpdateStatus(exchange, path);
                } else if ("PUT".equals(method) && path.matches("/api/plants/[^/]+")) {
                    handleUpdatePlant(exchange, path);
                } else if ("DELETE".equals(method) && path.matches("/api/plants/[^/]+")) {
                    handleDeletePlant(exchange, path);
                } else if ("POST".equals(method) && path.matches("/api/plants/[^/]+/care-logs")) {
                    handleAddCareLog(exchange, path);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not found\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        }
    }

    private void handleGetPlants(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        Map<String, String> params = queryToMap(uri.getQuery());
        String location = params.get("location");
        String status = params.get("status");

        List<Plant> result;
        if ((location != null && !location.isEmpty()) || (status != null && !status.isEmpty())) {
            result = service.filterPlants(location, status);
        } else {
            result = service.getAllPlants();
        }

        sendJsonResponse(exchange, 200, result);
    }

    private void handleGetSortedPlants(HttpExchange exchange) throws IOException {
        List<Plant> result = service.getAllPlantsSortedByUrgency();
        sendJsonResponse(exchange, 200, result);
    }

    private void handleGetPlant(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path);
        Plant plant = service.getPlantById(id);
        if (plant != null) {
            sendJsonResponse(exchange, 200, plant);
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Plant not found\"}");
        }
    }

    private void handleCreatePlant(HttpExchange exchange) throws IOException {
        Plant plant = readBody(exchange, Plant.class);
        String validationError = validatePlant(plant);
        if (validationError != null) {
            sendResponse(exchange, 400, "{\"error\":\"" + validationError + "\"}");
            return;
        }
        Plant created = service.addPlant(plant);
        sendJsonResponse(exchange, 201, created);
    }

    private void handleUpdatePlant(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path);
        Plant plant = readBody(exchange, Plant.class);
        String validationError = validatePlant(plant);
        if (validationError != null) {
            sendResponse(exchange, 400, "{\"error\":\"" + validationError + "\"}");
            return;
        }
        Plant updated = service.updatePlant(id, plant);
        if (updated != null) {
            sendJsonResponse(exchange, 200, updated);
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Plant not found\"}");
        }
    }

    private void handleDeletePlant(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path);
        boolean deleted = service.deletePlant(id);
        if (deleted) {
            sendResponse(exchange, 204, "");
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Plant not found\"}");
        }
    }

    private void handleUpdateStatus(HttpExchange exchange, String path) throws IOException {
        String id = extractIdFromStatusPath(path);
        Map<String, String> body = readBody(exchange, Map.class);
        String statusStr = body.get("status");
        if (statusStr == null || statusStr.trim().isEmpty()) {
            sendResponse(exchange, 400, "{\"error\":\"Status cannot be empty\"}");
            return;
        }
        if (!VALID_STATUSES.contains(statusStr)) {
            sendResponse(exchange, 400, "{\"error\":\"Invalid status\"}");
            return;
        }
        PlantStatus status = PlantStatus.valueOf(statusStr.toUpperCase().replace(" ", "_"));
        Plant updated = service.updatePlantStatus(id, status);
        if (updated != null) {
            sendJsonResponse(exchange, 200, updated);
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Plant not found\"}");
        }
    }

    private void handleUpdatePhoto(HttpExchange exchange, String path) throws IOException {
        String id = path.replace("/api/plants/", "").replace("/photo", "");
        Map<String, String> body = readBody(exchange, Map.class);
        String photoUrl = body.get("photoUrl");
        if (photoUrl == null || photoUrl.trim().isEmpty()) {
            sendResponse(exchange, 400, "{\"error\":\"Photo URL cannot be empty\"}");
            return;
        }
        Plant updated = service.updatePlantPhoto(id, photoUrl);
        if (updated != null) {
            sendJsonResponse(exchange, 200, updated);
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Plant not found\"}");
        }
    }

    private void handleAddCareLog(HttpExchange exchange, String path) throws IOException {
        String id = extractIdFromCareLogPath(path);
        Map<String, String> body = readBody(exchange, Map.class);
        String typeStr = body.get("type");
        String note = body.get("note");
        if (typeStr == null || typeStr.trim().isEmpty()) {
            sendResponse(exchange, 400, "{\"error\":\"Care type cannot be empty\"}");
            return;
        }
        if (!VALID_CARE_TYPES.contains(typeStr)) {
            sendResponse(exchange, 400,
                    "{\"error\":\"Invalid care type. Must be one of: WATERING, FERTILIZING, PRUNING\"}");
            return;
        }
        CareType type = CareType.valueOf(typeStr);
        CareLog log = service.addCareLog(id, type, note);
        if (log != null) {
            sendJsonResponse(exchange, 201, log);
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Plant not found\"}");
        }
    }

    private void handleGetRecentCareLogs(HttpExchange exchange, String path) throws IOException {
        String id = extractIdFromRecentLogsPath(path);
        URI uri = exchange.getRequestURI();
        Map<String, String> params = queryToMap(uri.getQuery());
        int count = 5;
        String countStr = params.get("count");
        if (countStr != null && !countStr.isEmpty()) {
            try {
                count = Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                count = 5;
            }
        }
        List<CareLog> logs = service.getRecentCareLogs(id, count);
        sendJsonResponse(exchange, 200, logs);
    }

    private void handleGetTimeline(HttpExchange exchange, String path) throws IOException {
        String id = path.replace("/api/plants/", "").replace("/timeline", "");
        Map<LocalDateTime, List<CareLog>> timeline = service.getCareTimeline(id);
        sendJsonResponse(exchange, 200, timeline);
    }

    private void handleGetNeedingWater(HttpExchange exchange) throws IOException {
        List<Plant> result = service.getPlantsNeedingWater();
        sendJsonResponse(exchange, 200, result);
    }

    private void handleGetStatistics(HttpExchange exchange) throws IOException {
        PlantStatistics stats = service.getStatistics();
        sendJsonResponse(exchange, 200, stats);
    }

    private void handleExportPlants(HttpExchange exchange) throws IOException {
        List<Plant> result = service.exportPlants();
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=plants-backup.json");
        sendResponse(exchange, 200, json);
    }

    private void handleImportPlants(HttpExchange exchange) throws IOException {
        try {
            List<Plant> importedPlants = objectMapper.readValue(
                    exchange.getRequestBody(),
                    new TypeReference<List<Plant>>() {}
            );
            service.importPlants(importedPlants);
            sendResponse(exchange, 200, "{\"message\":\"Imported " + importedPlants.size() + " plants successfully\"}");
        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"error\":\"Invalid JSON format: " + e.getMessage() + "\"}");
        }
    }

    private String extractId(String path) {
        return path.replace("/api/plants/", "");
    }

    private String extractIdFromStatusPath(String path) {
        return path.replace("/api/plants/", "").replace("/status", "");
    }

    private String extractIdFromCareLogPath(String path) {
        return path.replace("/api/plants/", "").replace("/care-logs", "");
    }

    private String extractIdFromRecentLogsPath(String path) {
        return path.replace("/api/plants/", "").replace("/care-logs/recent", "");
    }

    private String validatePlant(Plant plant) {
        if (plant == null) {
            return "Plant data cannot be null";
        }
        if (plant.getName() == null || plant.getName().trim().isEmpty()) {
            return "Plant name cannot be empty";
        }
        if (plant.getLocation() == null || plant.getLocation().trim().isEmpty()) {
            return "Location cannot be empty";
        }
        if (plant.getLightRequirement() == null || plant.getLightRequirement().trim().isEmpty()) {
            return "Light requirement cannot be empty";
        }
        if (plant.getWateringIntervalDays() <= 0) {
            return "Watering interval days must be greater than 0";
        }
        return null;
    }

    private Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return result;
        }
        try {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    String key = URLDecoder.decode(pair[0], "UTF-8");
                    String value = URLDecoder.decode(pair[1], "UTF-8");
                    result.put(key, value);
                } else if (pair.length == 1) {
                    result.put(URLDecoder.decode(pair[0], "UTF-8"), "");
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not supported", e);
        }
        return result;
    }

    private <T> T readBody(HttpExchange exchange, Class<T> clazz) throws IOException {
        InputStream is = exchange.getRequestBody();
        return objectMapper.readValue(is, clazz);
    }

    private void sendJsonResponse(HttpExchange exchange, int status, Object body) throws IOException {
        String json = objectMapper.writeValueAsString(body);
        sendResponse(exchange, status, json);
    }

    private void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
