package controller;

import com.sun.net.httpserver.HttpExchange;
import model.Movie;
import service.MovieService;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovieController {
    private final MovieService service;

    public MovieController(MovieService service) {
        this.service = service;
    }

    public void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();

        if ("GET".equalsIgnoreCase(method) && path.equals("/api/movies")) {
            handleGetMovies(exchange);
        } else if ("GET".equalsIgnoreCase(method) && path.equals("/api/stats")) {
            handleGetStats(exchange);
        } else if ("GET".equalsIgnoreCase(method) && path.equals("/api/genres")) {
            handleGetGenres(exchange);
        } else if ("GET".equalsIgnoreCase(method) && path.equals("/api/genre-stats")) {
            handleGetGenreStats(exchange);
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
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
        }
    }

    private void handleGetMovies(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String status = params.get("status");
        String genre = params.get("genre");
        String search = params.get("search");
        String sort = params.get("sort");
        List<Movie> movies = service.filterMovies(status, genre, search, sort);
        sendResponse(exchange, 200, moviesToJson(movies));
    }

    private void handleGetMovieById(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path);
        Movie movie = service.getMovieById(id);
        if (movie != null) {
            sendResponse(exchange, 200, movieToJson(movie));
        } else {
            sendResponse(exchange, 404, "{\"error\":\"电影不存在\"}");
        }
    }

    private void handleGetStats(HttpExchange exchange) throws IOException {
        Map<String, Object> stats = new HashMap<String, Object>();
        stats.put("total", service.getTotalCount());
        stats.put("watched", service.getCountByStatus("已看"));
        stats.put("wantToWatch", service.getCountByStatus("想看"));
        stats.put("shelved", service.getCountByStatus("搁置"));
        sendResponse(exchange, 200, mapToJson(stats));
    }

    private void handleGetGenres(HttpExchange exchange) throws IOException {
        List<String> genres = service.getAllGenres();
        sendResponse(exchange, 200, stringListToJson(genres));
    }

    private void handleGetGenreStats(HttpExchange exchange) throws IOException {
        Map<String, Integer> genreStats = service.getGenreStats();
        sendResponse(exchange, 200, intMapToJson(genreStats));
    }

    private void handleAddMovie(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Movie movie = parseMovieFromJson(body);
        if (movie == null || movie.getName() == null || movie.getName().isEmpty()) {
            sendResponse(exchange, 400, "{\"error\":\"电影名称不能为空\"}");
            return;
        }
        Movie saved = service.addMovie(movie);
        sendResponse(exchange, 201, movieToJson(saved));
    }

    private void handleUpdateStatus(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path);
        String body = readBody(exchange);
        Map<String, String> data = parseJsonPairs(body);
        String status = data.get("status");
        if (status == null || status.isEmpty()) {
            sendResponse(exchange, 400, "{\"error\":\"状态不能为空\"}");
            return;
        }
        boolean success = service.updateStatus(id, status);
        if (success) {
            Movie updated = service.getMovieById(id);
            sendResponse(exchange, 200, movieToJson(updated));
        } else {
            sendResponse(exchange, 404, "{\"error\":\"电影不存在\"}");
        }
    }

    private void handleUpdateMovie(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path);
        String body = readBody(exchange);
        Movie updated = parseMovieFromJson(body);
        if (updated == null) {
            sendResponse(exchange, 400, "{\"error\":\"数据格式错误\"}");
            return;
        }
        boolean success = service.updateMovie(id, updated);
        if (success) {
            Movie result = service.getMovieById(id);
            sendResponse(exchange, 200, movieToJson(result));
        } else {
            sendResponse(exchange, 404, "{\"error\":\"电影不存在\"}");
        }
    }

    private void handleDeleteMovie(HttpExchange exchange, String path) throws IOException {
        String id = extractId(path);
        boolean success = service.deleteMovie(id);
        if (success) {
            sendResponse(exchange, 200, "{\"success\":true}");
        } else {
            sendResponse(exchange, 404, "{\"error\":\"电影不存在\"}");
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

    private Movie parseMovieFromJson(String json) {
        try {
            Map<String, String> data = parseJsonPairs(json);
            Movie movie = new Movie();
            movie.setName(data.get("name"));
            movie.setDirector(data.get("director"));
            String yearStr = data.get("year");
            if (yearStr != null && !yearStr.isEmpty()) {
                try {
                    movie.setYear(Integer.parseInt(yearStr));
                } catch (NumberFormatException e) {
                    movie.setYear(0);
                }
            }
            movie.setGenre(data.get("genre"));
            String status = data.get("status");
            movie.setStatus((status == null || status.isEmpty()) ? "想看" : status);
            movie.setComment(data.get("comment"));
            String ratingStr = data.get("rating");
            if (ratingStr != null && !ratingStr.isEmpty()) {
                try {
                    int r = Integer.parseInt(ratingStr);
                    if (r >= 0 && r <= 5) movie.setRating(r);
                } catch (NumberFormatException e) {
                }
            }
            movie.setPosterUrl(data.get("posterUrl"));
            return movie;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, String> parseJsonPairs(String json) {
        Map<String, String> result = new HashMap<String, String>();
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
        }
        boolean inString = false;
        int depth = 0;
        int start = 0;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') depth--;
                else if (c == ',' && depth == 0) {
                    parsePair(json.substring(start, i), result);
                    start = i + 1;
                }
            }
        }
        if (start < json.length()) {
            parsePair(json.substring(start), result);
        }
        return result;
    }

    private void parsePair(String pair, Map<String, String> result) {
        boolean inString = false;
        for (int i = 0; i < pair.length(); i++) {
            char c = pair.charAt(i);
            if (c == '"' && (i == 0 || pair.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString && c == ':') {
                String key = stripQuotes(pair.substring(0, i).trim());
                String value = pair.substring(i + 1).trim();
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = stripQuotes(value);
                }
                result.put(key, value);
                return;
            }
        }
    }

    private String stripQuotes(String s) {
        s = s.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private String moviesToJson(List<Movie> movies) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < movies.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(movieToJson(movies.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private String movieToJson(Movie movie) {
        if (movie == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(escape(movie.getId())).append("\",");
        sb.append("\"name\":\"").append(escape(movie.getName())).append("\",");
        sb.append("\"director\":\"").append(escape(movie.getDirector())).append("\",");
        sb.append("\"year\":").append(movie.getYear()).append(",");
        sb.append("\"genre\":\"").append(escape(movie.getGenre())).append("\",");
        sb.append("\"status\":\"").append(escape(movie.getStatus())).append("\",");
        sb.append("\"comment\":\"").append(escape(movie.getComment())).append("\",");
        sb.append("\"rating\":").append(movie.getRating()).append(",");
        sb.append("\"posterUrl\":\"").append(escape(movie.getPosterUrl())).append("\",");
        sb.append("\"createdAt\":").append(movie.getCreatedAt());
        sb.append("}");
        return sb.toString();
    }

    private String stringListToJson(List<String> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escape(list.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escape(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value instanceof Number) {
                sb.append(value);
            } else {
                sb.append("\"").append(escape(String.valueOf(value))).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String intMapToJson(Map<String, Integer> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escape(entry.getKey())).append("\":").append(entry.getValue());
        }
        sb.append("}");
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
