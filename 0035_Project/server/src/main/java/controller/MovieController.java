package controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import model.Movie;
import service.MovieService;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovieController {
    private final MovieService service;
    private final Gson gson;

    public MovieController(MovieService service) {
        this.service = service;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }

    public void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();

        try {
            if ("GET".equalsIgnoreCase(method) && path.equals("/api/movies")) {
                handleGetMovies(exchange);
            } else if ("GET".equalsIgnoreCase(method) && path.equals("/api/stats")) {
                handleGetStats(exchange);
            } else if ("GET".equalsIgnoreCase(method) && path.equals("/api/genres")) {
                handleGetGenres(exchange);
            } else if ("GET".equalsIgnoreCase(method) && path.equals("/api/genre-stats")) {
                handleGetGenreStats(exchange);
            } else if ("GET".equalsIgnoreCase(method) && path.equals("/api/status-stats")) {
                handleGetStatusStats(exchange);
            } else if ("GET".equalsIgnoreCase(method) && path.equals("/api/year-stats")) {
                handleGetYearStats(exchange);
            } else if ("GET".equalsIgnoreCase(method) && path.equals("/api/rating-stats")) {
                handleGetRatingStats(exchange);
            } else if ("GET".equalsIgnoreCase(method) && path.equals("/api/export")) {
                handleExport(exchange);
            } else if ("POST".equalsIgnoreCase(method) && path.equals("/api/import")) {
                handleImport(exchange);
            } else if ("GET".equalsIgnoreCase(method) && path.equals("/api/random")) {
                handleGetRandom(exchange);
            } else if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/movies/")) {
                handleGetMovieById(exchange, path);
            } else if ("POST".equalsIgnoreCase(method) && path.equals("/api/movies")) {
                handleAddMovie(exchange);
            } else if ("PUT".equalsIgnoreCase(method) && path.startsWith("/api/movies/")) {
                handleUpdateMovie(exchange, path);
            } else if ("PATCH".equalsIgnoreCase(method) && path.startsWith("/api/movies/")) {
                handleUpdateStatus(exchange, path);
            } else if ("DELETE".equalsIgnoreCase(method) && path.startsWith("/api/movies/")) {
                handleDeleteMovie(exchange, path);
            } else if ("POST".equalsIgnoreCase(method) && path.equals("/api/batch/delete")) {
                handleBatchDelete(exchange);
            } else if ("POST".equalsIgnoreCase(method) && path.equals("/api/batch/status")) {
                handleBatchUpdateStatus(exchange);
            } else {
                sendError(exchange, 404, "Not Found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "服务器内部错误: " + e.getMessage());
        }
    }

    private void handleGetMovies(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String status = params.get("status");
        String genre = params.get("genre");
        String search = params.get("search");
        String sort = params.get("sort");
        List<Movie> movies = service.filterMovies(status, genre, search, sort);
        sendJson(exchange, 200, movies);
    }

    private void handleGetMovieById(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path);
        Movie movie = service.getMovieById(id);
        if (movie != null) {
            sendJson(exchange, 200, movie);
        } else {
            sendError(exchange, 404, "电影不存在");
        }
    }

    private void handleGetStats(HttpExchange exchange) throws IOException {
        Map<String, Object> stats = new HashMap<String, Object>();
        stats.put("total", service.getTotalCount());
        stats.put("watched", service.getCountByStatus("已看"));
        stats.put("wantToWatch", service.getCountByStatus("想看"));
        stats.put("shelved", service.getCountByStatus("搁置"));
        sendJson(exchange, 200, stats);
    }

    private void handleGetGenres(HttpExchange exchange) throws IOException {
        List<String> genres = service.getAllGenres();
        sendJson(exchange, 200, genres);
    }

    private void handleGetGenreStats(HttpExchange exchange) throws IOException {
        Map<String, Integer> genreStats = service.getGenreStats();
        sendJson(exchange, 200, genreStats);
    }

    private void handleGetStatusStats(HttpExchange exchange) throws IOException {
        Map<String, Object> stats = service.getStatusStats();
        sendJson(exchange, 200, stats);
    }

    private void handleGetYearStats(HttpExchange exchange) throws IOException {
        Map<String, Integer> yearStats = service.getYearStats();
        sendJson(exchange, 200, yearStats);
    }

    private void handleGetRatingStats(HttpExchange exchange) throws IOException {
        Map<String, Integer> ratingStats = service.getRatingStats();
        sendJson(exchange, 200, ratingStats);
    }

    private void handleAddMovie(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Movie movie = parseMovie(body);
        if (movie == null || movie.getName() == null || movie.getName().trim().isEmpty()) {
            sendError(exchange, 400, "电影名称不能为空");
            return;
        }
        Movie saved = service.addMovie(movie);
        sendJson(exchange, 201, saved);
    }

    private void handleUpdateStatus(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path);
        String body = readBody(exchange);
        Map<String, Object> data = parseMap(body);
        String status = data != null ? (String) data.get("status") : null;
        if (status == null || status.trim().isEmpty()) {
            sendError(exchange, 400, "状态不能为空");
            return;
        }
        boolean success = service.updateStatus(id, status);
        if (success) {
            Movie updated = service.getMovieById(id);
            sendJson(exchange, 200, updated);
        } else {
            sendError(exchange, 404, "电影不存在");
        }
    }

    private void handleUpdateMovie(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path);
        String body = readBody(exchange);
        Movie updated = parseMovie(body);
        if (updated == null) {
            sendError(exchange, 400, "数据格式错误");
            return;
        }
        boolean success = service.updateMovie(id, updated);
        if (success) {
            Movie result = service.getMovieById(id);
            sendJson(exchange, 200, result);
        } else {
            sendError(exchange, 404, "电影不存在");
        }
    }

    private void handleDeleteMovie(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path);
        boolean success = service.deleteMovie(id);
        if (success) {
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("success", true);
            sendJson(exchange, 200, result);
        } else {
            sendError(exchange, 404, "电影不存在");
        }
    }

    private void handleBatchDelete(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        List<String> ids = parseStringList(body);
        if (ids == null || ids.isEmpty()) {
            sendError(exchange, 400, "请提供要删除的电影ID列表");
            return;
        }
        int count = service.batchDelete(ids);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("deleted", count);
        sendJson(exchange, 200, result);
    }

    private void handleBatchUpdateStatus(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, Object> data = parseMap(body);
        if (data == null) {
            sendError(exchange, 400, "数据格式错误");
            return;
        }
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) data.get("ids");
        String status = (String) data.get("status");
        if (ids == null || ids.isEmpty()) {
            sendError(exchange, 400, "请提供要修改的电影ID列表");
            return;
        }
        if (status == null || status.trim().isEmpty()) {
            sendError(exchange, 400, "状态不能为空");
            return;
        }
        int count = service.batchUpdateStatus(ids, status);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("updated", count);
        result.put("status", status);
        sendJson(exchange, 200, result);
    }

    private void handleExport(HttpExchange exchange) throws IOException {
        String json = service.exportAll();
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Content-Disposition",
                "attachment; filename=\"movies-export.json\"");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private void handleImport(HttpExchange exchange) throws IOException {
        try {
            String body = readBody(exchange);
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            boolean overwrite = "true".equalsIgnoreCase(params.get("overwrite"));

            List<Movie> movies = parseMovieArrayFromJson(body);
            if (movies == null || movies.isEmpty()) {
                sendError(exchange, 400, "未找到有效的电影数据");
                return;
            }

            int count = service.importMovies(movies, overwrite);
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("success", true);
            result.put("imported", count);
            result.put("overwrite", overwrite);
            sendJson(exchange, 200, result);
        } catch (Exception e) {
            sendError(exchange, 400, "导入失败: " + e.getMessage());
        }
    }

    private List<Movie> parseMovieArrayFromJson(String json) {
        try {
            json = json.trim();
            Map<String, Object> wrapper = parseMap(json);
            if (wrapper != null && wrapper.containsKey("movies")) {
                Object moviesObj = wrapper.get("movies");
                if (moviesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> movieMaps = (List<Map<String, Object>>) moviesObj;
                    List<Movie> result = new ArrayList<Movie>();
                    for (Map<String, Object> map : movieMaps) {
                        Movie m = mapToMovie(map);
                        if (m != null && m.getName() != null && !m.getName().isEmpty()) {
                            result.add(m);
                        }
                    }
                    return result;
                }
            }
            Type listType = new TypeToken<List<Movie>>() {}.getType();
            List<Movie> movies = gson.fromJson(json, listType);
            return movies != null ? movies : new ArrayList<Movie>();
        } catch (Exception e) {
            return new ArrayList<Movie>();
        }
    }

    private Movie mapToMovie(Map<String, Object> map) {
        if (map == null) return null;
        Movie movie = new Movie();
        if (map.get("name") != null) movie.setName(String.valueOf(map.get("name")));
        if (map.get("director") != null) movie.setDirector(String.valueOf(map.get("director")));
        if (map.get("year") != null) {
            try { movie.setYear(((Number) map.get("year")).intValue()); } catch (Exception e) {}
        }
        if (map.get("genre") != null) movie.setGenre(String.valueOf(map.get("genre")));
        if (map.get("status") != null) movie.setStatus(String.valueOf(map.get("status")));
        if (map.get("comment") != null) movie.setComment(String.valueOf(map.get("comment")));
        if (map.get("rating") != null) {
            try { movie.setRating(((Number) map.get("rating")).intValue()); } catch (Exception e) {}
        }
        if (map.get("posterUrl") != null) movie.setPosterUrl(String.valueOf(map.get("posterUrl")));
        if (map.get("tags") != null) movie.setTags(String.valueOf(map.get("tags")));
        if (map.get("priority") != null) {
            try { movie.setPriority(((Number) map.get("priority")).intValue()); } catch (Exception e) {}
        }
        if (map.get("watchDate") != null) movie.setWatchDate(String.valueOf(map.get("watchDate")));
        if (map.get("rewatchCount") != null) {
            try { movie.setRewatchCount(((Number) map.get("rewatchCount")).intValue()); } catch (Exception e) {}
        }
        return movie;
    }

    private void handleGetRandom(HttpExchange exchange) throws IOException {
        Movie movie = service.getRandomWantToWatch();
        if (movie != null) {
            sendJson(exchange, 200, movie);
        } else {
            sendError(exchange, 404, "没有想看的电影，先添加一些吧！");
        }
    }

    private String extractId(String path) {
        return path.substring("/api/movies/".length());
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<String, String>();
        if (query == null || query.isEmpty()) return params;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                try {
                    params.put(java.net.URLDecoder.decode(kv[0], "UTF-8"),
                            java.net.URLDecoder.decode(kv[1], "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    params.put(kv[0], kv[1]);
                }
            }
        }
        return params;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    private Movie parseMovie(String json) {
        try {
            return gson.fromJson(json, Movie.class);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMap(String json) {
        try {
            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
            return gson.fromJson(json, mapType);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseStringList(String json) {
        try {
            Type listType = new TypeToken<List<String>>() {}.getType();
            return gson.fromJson(json, listType);
        } catch (Exception e) {
            Map<String, Object> map = parseMap(json);
            if (map != null && map.containsKey("ids")) {
                return (List<String>) map.get("ids");
            }
            return null;
        }
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, String> error = new HashMap<String, String>();
        error.put("error", message);
        sendJson(exchange, statusCode, error);
    }
}
