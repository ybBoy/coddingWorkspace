package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.Recipe;
import org.json.JSONArray;
import org.json.JSONObject;
import service.RecipeService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RecipeController implements HttpHandler {
    private final RecipeService service;

    public RecipeController() {
        this.service = new RecipeService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if (path.equals("/api/recipes") && method.equals("GET")) {
                handleGetRecipes(exchange);
            } else if (path.equals("/api/recipes") && method.equals("POST")) {
                handleAddRecipe(exchange);
            } else if (path.startsWith("/api/recipes/") && method.equals("GET")
                    && path.endsWith("/duplicate")) {
                handleDuplicateRecipe(exchange);
            } else if (path.startsWith("/api/recipes/") && method.equals("POST")
                    && path.endsWith("/mark-made")) {
                handleMarkAsMade(exchange);
            } else if (path.startsWith("/api/recipes/") && method.equals("PUT")) {
                handleUpdateRecipe(exchange);
            } else if (path.startsWith("/api/recipes/") && method.equals("DELETE")) {
                handleDeleteRecipe(exchange);
            } else if (path.equals("/api/recipes/search") && method.equals("GET")) {
                handleGetRecipes(exchange);
            } else if (path.equals("/api/shopping-list") && method.equals("POST")) {
                handleShoppingList(exchange);
            } else if (path.equals("/api/export") && method.equals("GET")) {
                handleExport(exchange);
            } else if (path.equals("/api/import") && method.equals("POST")) {
                handleImport(exchange);
            } else if (path.equals("/api/stats") && method.equals("GET")) {
                handleGetStats(exchange);
            } else {
                sendResponse(exchange, 404, new JSONObject().put("error", "Not Found").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, new JSONObject().put("error", "Server Error: " + e.getMessage()).toString());
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        try {
            for (String pair : query.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    params.put(URLDecoder.decode(kv[0], "UTF-8"),
                               URLDecoder.decode(kv[1], "UTF-8"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return params;
    }

    private void handleGetRecipes(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        String keyword = params.get("q");
        String taste = params.get("taste");
        String category = params.get("category");
        String sortBy = params.get("sortBy");
        String sortOrder = params.get("sortOrder");
        String ingredients = params.get("ingredients");
        Integer maxTime = null;
        if (params.containsKey("time")) {
            try { maxTime = Integer.parseInt(params.get("time")); } catch (NumberFormatException ignored) {}
        }
        List<Recipe> list = service.search(keyword, taste, category, maxTime, sortBy, sortOrder, ingredients);
        JSONArray arr = new JSONArray();
        for (Recipe r : list) arr.put(recipeToJson(r));
        sendResponse(exchange, 200, arr.toString());
    }

    private void handleAddRecipe(HttpExchange exchange) throws IOException {
        JSONObject body = getRequestBody(exchange);
        String name = body.optString("name", "").trim();
        Integer estimatedTime = body.has("estimatedTime") ? body.getInt("estimatedTime") : null;
        String mainIngredients = body.optString("mainIngredients", "").trim();

        Map<String, String> errors = service.validateRecipe(name, estimatedTime, mainIngredients);
        if (!errors.isEmpty()) {
            sendResponse(exchange, 400, new JSONObject().put("errors", errors).toString());
            return;
        }

        List<String> steps = new ArrayList<>();
        JSONArray stepsArr = body.optJSONArray("steps");
        if (stepsArr != null) {
            for (int i = 0; i < stepsArr.length(); i++) steps.add(stepsArr.getString(i));
        }

        Recipe recipe = service.addRecipe(
                name,
                body.optString("taste", null),
                body.optString("category", null),
                body.has("difficulty") ? body.getInt("difficulty") : null,
                body.has("servings") ? body.getInt("servings") : null,
                body.has("cost") ? body.getDouble("cost") : null,
                body.has("rating") ? body.getInt("rating") : null,
                estimatedTime,
                mainIngredients,
                steps,
                body.optString("notes", null),
                body.optString("image", null)
        );
        sendResponse(exchange, 201, recipeToJson(recipe).toString());
    }

    private void handleUpdateRecipe(HttpExchange exchange) throws IOException {
        String id = extractId(exchange.getRequestURI().getPath());
        Recipe existing = service.getRecipeById(id);
        if (existing == null) {
            sendResponse(exchange, 404, new JSONObject().put("error", "Recipe not found").toString());
            return;
        }

        JSONObject body = getRequestBody(exchange);
        String name = body.optString("name", null);
        if (name != null && name.trim().isEmpty()) name = null;
        Integer estimatedTime = body.has("estimatedTime") ? body.getInt("estimatedTime") : null;
        String mainIngredients = body.optString("mainIngredients", null);

        if (name != null || estimatedTime != null || mainIngredients != null) {
            Map<String, String> errors = service.validateRecipe(
                    name != null ? name : existing.getName(),
                    estimatedTime != null ? estimatedTime : existing.getEstimatedTime(),
                    mainIngredients != null ? mainIngredients : existing.getMainIngredients()
            );
            if (!errors.isEmpty()) {
                sendResponse(exchange, 400, new JSONObject().put("errors", errors).toString());
                return;
            }
        }

        List<String> steps = null;
        JSONArray stepsArr = body.optJSONArray("steps");
        if (stepsArr != null) {
            steps = new ArrayList<>();
            for (int i = 0; i < stepsArr.length(); i++) steps.add(stepsArr.getString(i));
        }

        Long lastMadeAt = null;
        if (body.has("lastMadeAt") && !body.isNull("lastMadeAt")) {
            lastMadeAt = body.getLong("lastMadeAt");
        }

        boolean ok = service.updateRecipeFull(
                id, name,
                body.has("taste") ? body.getString("taste") : null,
                body.has("category") ? body.getString("category") : null,
                body.has("difficulty") ? body.getInt("difficulty") : null,
                body.has("servings") ? body.getInt("servings") : null,
                body.has("cost") ? body.getDouble("cost") : null,
                body.has("rating") ? body.getInt("rating") : null,
                estimatedTime,
                mainIngredients,
                steps,
                body.has("notes") ? body.getString("notes") : null,
                body.has("image") ? body.getString("image") : null,
                lastMadeAt
        );

        if (ok) {
            sendResponse(exchange, 200, recipeToJson(service.getRecipeById(id)).toString());
        } else {
            sendResponse(exchange, 404, new JSONObject().put("error", "Recipe not found").toString());
        }
    }

    private void handleDuplicateRecipe(HttpExchange exchange) throws IOException {
        String fullPath = exchange.getRequestURI().getPath();
        String id = fullPath.substring("/api/recipes/".length(), fullPath.length() - "/duplicate".length());
        Recipe copy = service.duplicateRecipe(id);
        if (copy != null) {
            sendResponse(exchange, 201, recipeToJson(copy).toString());
        } else {
            sendResponse(exchange, 404, new JSONObject().put("error", "Recipe not found").toString());
        }
    }

    private void handleMarkAsMade(HttpExchange exchange) throws IOException {
        String fullPath = exchange.getRequestURI().getPath();
        String id = fullPath.substring("/api/recipes/".length(), fullPath.length() - "/mark-made".length());
        if (service.markAsMade(id)) {
            sendResponse(exchange, 200, recipeToJson(service.getRecipeById(id)).toString());
        } else {
            sendResponse(exchange, 404, new JSONObject().put("error", "Recipe not found").toString());
        }
    }

    private void handleDeleteRecipe(HttpExchange exchange) throws IOException {
        String id = extractId(exchange.getRequestURI().getPath());
        if (service.deleteRecipe(id)) {
            sendResponse(exchange, 200, new JSONObject().put("success", true).toString());
        } else {
            sendResponse(exchange, 404, new JSONObject().put("error", "Recipe not found").toString());
        }
    }

    private void handleShoppingList(HttpExchange exchange) throws IOException {
        JSONObject body = getRequestBody(exchange);
        JSONArray idsArr = body.optJSONArray("recipeIds");
        List<String> ids = new ArrayList<>();
        if (idsArr != null) {
            for (int i = 0; i < idsArr.length(); i++) ids.add(idsArr.getString(i));
        }
        List<String> list = service.generateShoppingList(ids);
        JSONArray arr = new JSONArray();
        for (String s : list) arr.put(s);
        sendResponse(exchange, 200, new JSONObject().put("items", arr).toString());
    }

    private void handleExport(HttpExchange exchange) throws IOException {
        String json = service.exportJson();
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=recipes.json");
        exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleImport(HttpExchange exchange) throws IOException {
        JSONObject body = getRequestBody(exchange);
        String json = body.optString("data", "");
        boolean merge = body.optBoolean("merge", false);
        boolean ok = service.importJson(json, merge);
        if (ok) {
            sendResponse(exchange, 200, new JSONObject().put("success", true).toString());
        } else {
            sendResponse(exchange, 400, new JSONObject().put("error", "导入失败，格式不正确").toString());
        }
    }

    private void handleGetStats(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        String keyword = params.get("q");
        String taste = params.get("taste");
        String category = params.get("category");
        String ingredients = params.get("ingredients");
        Integer maxTime = null;
        if (params.containsKey("time")) {
            try { maxTime = Integer.parseInt(params.get("time")); } catch (NumberFormatException ignored) {}
        }
        int filtered = service.search(keyword, taste, category, maxTime, null, null, ingredients).size();

        JSONObject stats = new JSONObject();
        stats.put("total", service.getTotalCount());
        stats.put("under30", service.getCountUnder30Minutes());
        stats.put("filtered", filtered);
        sendResponse(exchange, 200, stats.toString());
    }

    private String extractId(String path) {
        return path.substring("/api/recipes/".length());
    }

    private JSONObject recipeToJson(Recipe r) {
        JSONObject obj = new JSONObject();
        obj.put("id", r.getId());
        obj.put("name", r.getName());
        obj.put("taste", r.getTaste());
        obj.put("category", r.getCategory());
        obj.put("difficulty", r.getDifficulty());
        obj.put("servings", r.getServings());
        obj.put("cost", r.getCost());
        obj.put("rating", r.getRating());
        obj.put("estimatedTime", r.getEstimatedTime());
        obj.put("mainIngredients", r.getMainIngredients());
        obj.put("steps", new JSONArray(r.getSteps() != null ? r.getSteps() : new ArrayList<String>()));
        obj.put("notes", r.getNotes());
        obj.put("image", r.getImage());
        obj.put("createdAt", r.getCreatedAt());
        obj.put("lastMadeAt", r.getLastMadeAt() != null ? r.getLastMadeAt() : JSONObject.NULL);
        return obj;
    }

    private JSONObject getRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            byte[] buffer = new byte[8192];
            int len;
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) return new JSONObject();
            return new JSONObject(content);
        }
    }

    private void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
