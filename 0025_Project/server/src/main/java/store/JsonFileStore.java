package store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import domain.Article;
import domain.Note;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JsonFileStore {
    private final File dataFile;
    private final Gson gson;
    private final ScheduledExecutorService scheduler;
    private final Runnable saveHook;

    public static class StoreData {
        Article article;
        List<Note> notes;

        public StoreData() {}
        public StoreData(Article article, List<Note> notes) {
            this.article = article;
            this.notes = notes;
        }

        public Article getArticle() { return article; }
        public void setArticle(Article article) { this.article = article; }

        public List<Note> getNotes() { return notes; }
        public void setNotes(List<Note> notes) { this.notes = notes; }
    }

    public JsonFileStore(String filePath, Runnable saveHook) {
        this.dataFile = new File(filePath);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "json-store-scheduler");
            t.setDaemon(true);
            return t;
        });
        this.saveHook = saveHook;

        File parentDir = dataFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        scheduler.scheduleAtFixedRate(() -> {
            if (this.saveHook != null) {
                try { this.saveHook.run(); } catch (Exception ignored) {}
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    public synchronized void save(Article article, List<Note> notes) {
        StoreData data = new StoreData(article, notes);
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(dataFile), StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
            System.out.println("[Store] Data saved to " + dataFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[Store] Failed to save data: " + e.getMessage());
        }
    }

    public synchronized Optional<StoreData> load() {
        if (!dataFile.exists()) {
            System.out.println("[Store] Data file not found, starting fresh.");
            return Optional.empty();
        }
        try (Reader reader = new InputStreamReader(
                new FileInputStream(dataFile), StandardCharsets.UTF_8)) {
            StoreData data = gson.fromJson(reader, new TypeToken<StoreData>(){}.getType());
            if (data != null && data.article != null) {
                System.out.println("[Store] Data loaded from " + dataFile.getAbsolutePath());
                if (data.notes == null) data.notes = new ArrayList<>();
                return Optional.of(data);
            }
        } catch (IOException e) {
            System.err.println("[Store] Failed to load data: " + e.getMessage());
        }
        return Optional.empty();
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
