package http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import domain.FitnessCheckin;
import service.FitnessService;
import storage.JsonUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class CheckinHandler implements HttpHandler {
    private final FitnessService service;

    public CheckinHandler(FitnessService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equals(method)) {
            sendResponse(exchange, 204, "");
            return;
        }

        try {
            if ("GET".equals(method) && path.equals("/api/checkin")) {
                handleGet(exchange);
            } else if ("POST".equals(method) && path.equals("/api/checkin")) {
                handlePost(exchange);
            } else if ("PUT".equals(method) && path.startsWith("/api/checkin/")) {
                handlePut(exchange);
            } else if ("DELETE".equals(method) && path.startsWith("/api/checkin/")) {
                handleDelete(exchange);
            } else if ("GET".equals(method) && path.equals("/api/stats")) {
                handleStats(exchange);
            } else if ("GET".equals(method) && path.equals("/api/settings/goal")) {
                handleGetGoal(exchange);
            } else if ("PUT".equals(method) && path.equals("/api/settings/goal")) {
                handleSetGoal(exchange);
            } else if ("POST".equals(method) && path.equals("/api/checkin/import")) {
                handleImport(exchange);
            } else {
                sendResponse(exchange, 404, JsonUtil.toErrorJson("路径不存在"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, JsonUtil.toErrorJson("服务器内部错误"));
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        List<FitnessCheckin> records;

        if (query != null && query.contains("type=")) {
            String type = query.split("type=")[1].split("&")[0];
            type = java.net.URLDecoder.decode(type, "UTF-8");
            records = service.getCheckinsByType(type);
        } else {
            records = service.getAllCheckins();
        }

        sendJsonResponse(exchange, 200, JsonUtil.toJson(records));
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, String> data = JsonUtil.parseRequestBody(body);

        String dateStr = data.get("checkinDate");
        String exerciseType = data.get("exerciseType");
        String durationStr = data.get("duration");
        String mood = data.get("mood");
        String note = data.get("note");

        if (dateStr == null || exerciseType == null || durationStr == null || mood == null) {
            sendResponse(exchange, 400, JsonUtil.toErrorJson("缺少必填字段"));
            return;
        }

        LocalDate checkinDate;
        int duration;
        try {
            checkinDate = LocalDate.parse(dateStr);
            duration = Integer.parseInt(durationStr);
        } catch (Exception e) {
            sendResponse(exchange, 400, JsonUtil.toErrorJson("日期或时长格式错误"));
            return;
        }

        if (duration <= 0 || duration > 480) {
            sendResponse(exchange, 400, JsonUtil.toErrorJson("时长必须在1-480分钟之间"));
            return;
        }

        FitnessCheckin checkin = service.addCheckin(checkinDate, exerciseType, duration, mood, note);
        sendJsonResponse(exchange, 201, JsonUtil.toJson(checkin));
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String id = path.substring("/api/checkin/".length());

        String body = readRequestBody(exchange);
        Map<String, String> data = JsonUtil.parseRequestBody(body);

        LocalDate checkinDate = null;
        String exerciseType = null;
        Integer duration = null;
        String mood = null;
        String note = null;

        String dateStr = data.get("checkinDate");
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                checkinDate = LocalDate.parse(dateStr);
            } catch (Exception e) {
                sendResponse(exchange, 400, JsonUtil.toErrorJson("日期格式错误"));
                return;
            }
        }

        exerciseType = data.get("exerciseType");

        String durationStr = data.get("duration");
        if (durationStr != null && !durationStr.isEmpty()) {
            try {
                duration = Integer.parseInt(durationStr);
                if (duration <= 0 || duration > 480) {
                    sendResponse(exchange, 400, JsonUtil.toErrorJson("时长必须在1-480分钟之间"));
                    return;
                }
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, JsonUtil.toErrorJson("时长格式错误"));
                return;
            }
        }

        mood = data.get("mood");
        note = data.get("note");

        boolean updated = service.updateCheckin(id, checkinDate, exerciseType, duration, mood, note);
        if (updated) {
            FitnessCheckin updatedRecord = service.getCheckinById(id);
            if (updatedRecord != null) {
                sendJsonResponse(exchange, 200, JsonUtil.toJson(updatedRecord));
            } else {
                sendResponse(exchange, 200, JsonUtil.toSuccessJson("更新成功"));
            }
        } else {
            sendResponse(exchange, 404, JsonUtil.toErrorJson("记录不存在"));
        }
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String id = path.substring("/api/checkin/".length());

        boolean deleted = service.deleteCheckin(id);
        if (deleted) {
            sendResponse(exchange, 200, JsonUtil.toSuccessJson("删除成功"));
        } else {
            sendResponse(exchange, 404, JsonUtil.toErrorJson("记录不存在"));
        }
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        int weeklyMinutes = service.getWeeklyMinutes();
        int streakDays = service.getStreakDays();
        int weeklyGoal = service.getWeeklyGoal();
        int totalCount = service.getAllCheckins().size();

        String json = "{"
                + "\"weeklyMinutes\":" + weeklyMinutes + ","
                + "\"streakDays\":" + streakDays + ","
                + "\"weeklyGoal\":" + weeklyGoal + ","
                + "\"totalCount\":" + totalCount
                + "}";
        sendJsonResponse(exchange, 200, json);
    }

    private void handleGetGoal(HttpExchange exchange) throws IOException {
        int goal = service.getWeeklyGoal();
        String json = "{\"weeklyGoal\":" + goal + "}";
        sendJsonResponse(exchange, 200, json);
    }

    private void handleSetGoal(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, String> data = JsonUtil.parseRequestBody(body);

        String goalStr = data.get("weeklyGoal");
        if (goalStr == null || goalStr.isEmpty()) {
            sendResponse(exchange, 400, JsonUtil.toErrorJson("缺少目标值"));
            return;
        }

        int goal;
        try {
            goal = Integer.parseInt(goalStr);
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, JsonUtil.toErrorJson("目标值格式错误"));
            return;
        }

        if (goal <= 0 || goal > 10080) {
            sendResponse(exchange, 400, JsonUtil.toErrorJson("目标值必须在1-10080分钟之间"));
            return;
        }

        service.setWeeklyGoal(goal);
        sendResponse(exchange, 200, JsonUtil.toSuccessJson("目标已更新"));
    }

    private void handleImport(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);

        List<FitnessCheckin> importList;
        try {
            importList = JsonUtil.parseList(body);
        } catch (Exception e) {
            sendResponse(exchange, 400, JsonUtil.toErrorJson("JSON格式错误"));
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        boolean overwrite = query != null && query.contains("overwrite=true");

        int count = service.importCheckins(importList, overwrite);

        String json = "{\"imported\":" + count + ",\"overwrite\":" + overwrite + "}";
        sendJsonResponse(exchange, 200, json);
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        sendResponse(exchange, statusCode, json);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
