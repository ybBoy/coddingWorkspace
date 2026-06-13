package api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import domain.CareLog;
import domain.Plant;
import service.PlantCareService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlantController {
    private final PlantCareService service;
    private final ObjectMapper objectMapper;

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
                } else if ("GET".equals(method) && path.matches("/api/plants/[^/]+")) {
                    handleGetPlant(exchange, path);
                } else if ("POST".equals(method) && path.equals("/api/plants")) {
                    handleCreatePlant(exchange);
                } else if ("PUT".equals(method) && path.matches("/api/plants/[^/]+")) {
                    handleUpdatePlant(exchange, path);
                } else if ("DELETE".equals(method) && path.matches("/api/plants/[^/]+")) {
                    handleDeletePlant(exchange, path);
                } else if ("PUT".equals(method) && path.matches("/api/plants/[^/]+/status")) {
                    handleUpdateStatus(exchange, path);
                } else if ("POST".equals(method) && path.matches("/api/plants/[^/]+/care-logs")) {
                    handleAddCareLog(exchange, path);
                } else if ("GET".equals(method) && path.matches("/api/plants/[^/]+/care-logs/recent")) {
                    handleGetRecentCareLogs(exchange, path);
                } else if ("GET".equals(method) && path.equals("/api/plants/needing-water")) {
                    handleGetNeedingWater(exchange);
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
        Plant created = service.addPlant(plant);
        sendJsonResponse(exchange, 201, created);
    }

    private void handleUpdatePlant(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path);
        Plant plant = readBody(exchange, Plant.class);
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
        String status = body.get("status");
        Plant updated = service.updatePlantStatus(id, status);
        if (updated != null) {
            sendJsonResponse(exchange, 200, updated);
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Plant not found\"}");
        }
    }

    private void handleAddCareLog(HttpExchange exchange, String path) throws IOException {
        String id = extractIdFromCareLogPath(path);
        Map<String, String> body = readBody(exchange, Map.class);
        String type = body.get("type");
        String note = body.get("note");
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

    private void handleGetNeedingWater(HttpExchange exchange) throws IOException {
        List<Plant> result = service.getPlantsNeedingWater();
        sendJsonResponse(exchange, 200, result);
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

    private Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return result;
        }
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2) {
                result.put(pair[0], pair[1]);
            } else if (pair.length == 1) {
                result.put(pair[0], "");
            }
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
