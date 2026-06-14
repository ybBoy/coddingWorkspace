package controller;

import com.sun.net.httpserver.HttpExchange;
import model.Recipe;
import org.json.JSONArray;
import org.json.JSONObject;
import service.RecipeService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeController {
    private RecipeService service;

    public RecipeController() {
        this.service = new RecipeService();
    }

    public void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if (path.equals("/api/recipes") && method.equals("GET")) {
            handleGetRecipes(exchange);
        } else if (path.equals("/api/recipes") && method.equals("POST")) {
            handleAddRecipe(exchange);
        } else if (path.startsWith("/api/recipes/") && method.equals("PUT")) {
            handleUpdateRecipe(exchange);
        } else if (path.startsWith("/api/recipes/") && method.equals("PATCH")) {
            handlePatchRecipeNotes(exchange);
        } else if (path.startsWith("/api/recipes/") && method.equals("DELETE")) {
            handleDeleteRecipe(exchange);
        } else if (path.equals("/api/stats") && method.equals("GET")) {
            handleGetStats(exchange);
        } else {
            sendResponse(exchange, 404, new JSONObject().put("error", "Not Found").toString());
        }
    }

    private void handleGetRecipes(HttpExchange exchange) throws IOException {
        Map<String, String> params = getQueryParams(exchange.getRequestURI());
        String taste = params.get("taste");
        String timeStr = params.get("time");

        List<Recipe> result;
        if (taste != null && timeStr != null) {
            int maxTime = Integer.parseInt(timeStr);
            result = service.filterByTasteAndTime(taste, maxTime);
        } else if (taste != null) {
            result = service.filterByTaste(taste);
        } else if (timeStr != null) {
            int maxTime = Integer.parseInt(timeStr);
            result = service.filterByTime(maxTime);
        } else {
            result = service.getAllRecipes();
        }

        JSONArray array = new JSONArray();
        for (Recipe recipe : result) {
            array.put(recipeToJson(recipe));
        }
        sendResponse(exchange, 200, array.toString());
    }

    private void handleAddRecipe(HttpExchange exchange) throws IOException {
        JSONObject body = getRequestBody(exchange);
        String name = body.getString("name");
        String taste = body.getString("taste");
        int estimatedTime = body.getInt("estimatedTime");
        String mainIngredients = body.getString("mainIngredients");
        String notes = body.optString("notes", "");

        Recipe recipe = service.addRecipe(name, taste, estimatedTime, mainIngredients, notes);
        sendResponse(exchange, 201, recipeToJson(recipe).toString());
    }

    private void handleUpdateRecipe(HttpExchange exchange) throws IOException {
        String id = extractId(exchange.getRequestURI().getPath());
        JSONObject body = getRequestBody(exchange);
        String name = body.getString("name");
        String taste = body.getString("taste");
        int estimatedTime = body.getInt("estimatedTime");
        String mainIngredients = body.getString("mainIngredients");
        String notes = body.optString("notes", "");

        boolean success = service.updateRecipe(id, name, taste, estimatedTime, mainIngredients, notes);
        if (success) {
            Recipe updated = service.getRecipeById(id);
            sendResponse(exchange, 200, recipeToJson(updated).toString());
        } else {
            sendResponse(exchange, 404, new JSONObject().put("error", "Recipe not found").toString());
        }
    }

    private void handlePatchRecipeNotes(HttpExchange exchange) throws IOException {
        String id = extractId(exchange.getRequestURI().getPath());
        JSONObject body = getRequestBody(exchange);
        String notes = body.optString("notes", "");

        boolean success = service.updateRecipeNotes(id, notes);
        if (success) {
            Recipe updated = service.getRecipeById(id);
            sendResponse(exchange, 200, recipeToJson(updated).toString());
        } else {
            sendResponse(exchange, 404, new JSONObject().put("error", "Recipe not found").toString());
        }
    }

    private void handleDeleteRecipe(HttpExchange exchange) throws IOException {
        String id = extractId(exchange.getRequestURI().getPath());
        boolean success = service.deleteRecipe(id);
        if (success) {
            sendResponse(exchange, 200, new JSONObject().put("message", "Deleted successfully").toString());
        } else {
            sendResponse(exchange, 404, new JSONObject().put("error", "Recipe not found").toString());
        }
    }

    private void handleGetStats(HttpExchange exchange) throws IOException {
        Map<String, String> params = getQueryParams(exchange.getRequestURI());
        String taste = params.get("taste");
        String timeStr = params.get("time");

        int filteredCount;
        if (taste != null && timeStr != null) {
            int maxTime = Integer.parseInt(timeStr);
            filteredCount = service.filterByTasteAndTime(taste, maxTime).size();
        } else if (taste != null) {
            filteredCount = service.filterByTaste(taste).size();
        } else if (timeStr != null) {
            int maxTime = Integer.parseInt(timeStr);
            filteredCount = service.filterByTime(maxTime).size();
        } else {
            filteredCount = service.getTotalCount();
        }

        JSONObject stats = new JSONObject();
        stats.put("total", service.getTotalCount());
        stats.put("under30", service.getCountUnder30Minutes());
        stats.put("filtered", filteredCount);
        sendResponse(exchange, 200, stats.toString());
    }

    private String extractId(String path) {
        String[] parts = path.split("/");
        return parts[parts.length - 1];
    }

    private JSONObject recipeToJson(Recipe recipe) {
        JSONObject obj = new JSONObject();
        obj.put("id", recipe.getId());
        obj.put("name", recipe.getName());
        obj.put("taste", recipe.getTaste());
        obj.put("estimatedTime", recipe.getEstimatedTime());
        obj.put("mainIngredients", recipe.getMainIngredients());
        obj.put("notes", recipe.getNotes());
        obj.put("createdAt", recipe.getCreatedAt());
        return obj;
    }

    private JSONObject getRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        String body = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        return new JSONObject(body);
    }

    private Map<String, String> getQueryParams(URI uri) {
        Map<String, String> params = new HashMap<>();
        String query = uri.getQuery();
        if (query != null) {
            for (String pair : query.split("&")) {
                String[] parts = pair.split("=");
                if (parts.length == 2) {
                    params.put(parts[0], parts[1]);
                }
            }
        }
        return params;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes(StandardCharsets.UTF_8));
        os.close();
    }

    public void handleOptions(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(204, -1);
    }
}
