package store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import model.Movie;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JsonMovieStore {
    private final String filePath;
    private final Gson gson;

    public JsonMovieStore(String filePath) {
        this.filePath = filePath;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
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
            String json = sb.toString().trim();
            if (json.isEmpty() || json.equals("[]")) {
                return new ArrayList<Movie>();
            }
            Type listType = new TypeToken<List<Movie>>() {}.getType();
            List<Movie> movies = gson.fromJson(json, listType);
            return movies != null ? movies : new ArrayList<Movie>();
        } catch (IOException e) {
            throw new RuntimeException("读取数据文件失败", e);
        }
    }

    public void saveMovies(List<Movie> movies) {
        try {
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8));
            writer.write(gson.toJson(movies));
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("保存数据文件失败", e);
        }
    }

    public String toJson(Object obj) {
        return gson.toJson(obj);
    }

    public <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    public <T> T fromJson(String json, Type typeOfT) {
        return gson.fromJson(json, typeOfT);
    }

    public String generateId() {
        return UUID.randomUUID().toString();
    }
}
