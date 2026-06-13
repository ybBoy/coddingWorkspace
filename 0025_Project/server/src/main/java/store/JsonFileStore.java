package store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import domain.Room;

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
        List<Room> rooms;

        public StoreData() {}
        public StoreData(List<Room> rooms) {
            this.rooms = rooms;
        }

        public List<Room> getRooms() { return rooms; }
        public void setRooms(List<Room> rooms) { this.rooms = rooms; }
    }

    public JsonFileStore(String filePath, Runnable saveHook) {
        this.dataFile = new File(filePath);
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
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

    public synchronized void saveRooms(List<Room> rooms) {
        StoreData data = new StoreData(rooms);
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(dataFile), StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
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
            if (data != null && data.rooms != null) {
                System.out.println("[Store] Data loaded: " + data.rooms.size() + " rooms");
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
