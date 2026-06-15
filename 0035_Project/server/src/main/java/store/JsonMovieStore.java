package store;

import model.Movie;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JsonMovieStore {
    private final String filePath;

    public JsonMovieStore(String filePath) {
        this.filePath = filePath;
        ensureFileExists();
    }

    private void ensureFileExists() {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
                saveMovies(new ArrayList<Movie>());
            } catch (IOException e) {
                throw new RuntimeException("无法创建数据文件: " + filePath, e);
            }
        }
    }

    public List<Movie> loadMovies() {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return parseMovies(sb.toString());
        } catch (IOException e) {
            throw new RuntimeException("读取数据文件失败", e);
        }
    }

    public void saveMovies(List<Movie> movies) {
        try {
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8));
            writer.write(toJsonArray(movies));
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("保存数据文件失败", e);
        }
    }

    private List<Movie> parseMovies(String json) {
        List<Movie> movies = new ArrayList<Movie>();
        json = json.trim();
        if (json.isEmpty() || json.equals("[]")) {
            return movies;
        }
        if (json.startsWith("[") && json.endsWith("]")) {
            json = json.substring(1, json.length() - 1);
        }
        List<String> objectStrs = splitJsonObjects(json);
        for (String objStr : objectStrs) {
            movies.add(parseMovie(objStr));
        }
        return movies;
    }

    private List<String> splitJsonObjects(String json) {
        List<String> result = new ArrayList<String>();
        int depth = 0;
        int start = -1;
        boolean inString = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == '{') {
                    if (depth == 0) start = i;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && start != -1) {
                        result.add(json.substring(start, i + 1));
                        start = -1;
                    }
                }
            }
        }
        return result;
    }

    private Movie parseMovie(String json) {
        Movie movie = new Movie();
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
        }
        String[] pairs = splitJsonPairs(json);
        for (String pair : pairs) {
            String[] kv = splitKeyValue(pair);
            if (kv.length == 2) {
                String key = stripQuotes(kv[0].trim());
                String value = kv[1].trim();
                setMovieField(movie, key, value);
            }
        }
        return movie;
    }

    private String[] splitJsonPairs(String json) {
        List<String> result = new ArrayList<String>();
        int depth = 0;
        int start = 0;
        boolean inString = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') depth--;
                else if (c == ',' && depth == 0) {
                    result.add(json.substring(start, i));
                    start = i + 1;
                }
            }
        }
        if (start < json.length()) {
            result.add(json.substring(start));
        }
        return result.toArray(new String[0]);
    }

    private String[] splitKeyValue(String pair) {
        boolean inString = false;
        for (int i = 0; i < pair.length(); i++) {
            char c = pair.charAt(i);
            if (c == '"' && (i == 0 || pair.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString && c == ':') {
                return new String[]{pair.substring(0, i), pair.substring(i + 1)};
            }
        }
        return new String[0];
    }

    private void setMovieField(Movie movie, String key, String value) {
        if ("id".equals(key)) {
            movie.setId(stripQuotes(value));
        } else if ("name".equals(key)) {
            movie.setName(stripQuotes(value));
        } else if ("director".equals(key)) {
            movie.setDirector(stripQuotes(value));
        } else if ("year".equals(key)) {
            try {
                movie.setYear(Integer.parseInt(value.trim()));
            } catch (NumberFormatException e) {
                movie.setYear(0);
            }
        } else if ("genre".equals(key)) {
            movie.setGenre(stripQuotes(value));
        } else if ("status".equals(key)) {
            movie.setStatus(stripQuotes(value));
        } else if ("comment".equals(key)) {
            movie.setComment(stripQuotes(value));
        } else if ("rating".equals(key)) {
            try {
                movie.setRating(Integer.parseInt(value.trim()));
            } catch (NumberFormatException e) {
                movie.setRating(0);
            }
        } else if ("posterUrl".equals(key)) {
            movie.setPosterUrl(stripQuotes(value));
        } else if ("createdAt".equals(key)) {
            try {
                movie.setCreatedAt(Long.parseLong(value.trim()));
            } catch (NumberFormatException e) {
                movie.setCreatedAt(System.currentTimeMillis());
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

    private String toJsonArray(List<Movie> movies) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < movies.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJsonObject(movies.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private String toJsonObject(Movie movie) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(escapeJson(movie.getId())).append("\",");
        sb.append("\"name\":\"").append(escapeJson(movie.getName())).append("\",");
        sb.append("\"director\":\"").append(escapeJson(movie.getDirector())).append("\",");
        sb.append("\"year\":").append(movie.getYear()).append(",");
        sb.append("\"genre\":\"").append(escapeJson(movie.getGenre())).append("\",");
        sb.append("\"status\":\"").append(escapeJson(movie.getStatus())).append("\",");
        sb.append("\"comment\":\"").append(escapeJson(movie.getComment())).append("\",");
        sb.append("\"rating\":").append(movie.getRating()).append(",");
        sb.append("\"posterUrl\":\"").append(escapeJson(movie.getPosterUrl())).append("\",");
        sb.append("\"createdAt\":").append(movie.getCreatedAt());
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public String generateId() {
        return UUID.randomUUID().toString();
    }
}
