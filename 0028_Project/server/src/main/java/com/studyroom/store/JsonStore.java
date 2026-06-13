package com.studyroom.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.studyroom.model.Seat;
import com.studyroom.model.SeatAction;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JsonStore {
    private final Path dataDir;
    private final Path seatsFile;
    private final Path actionsFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private List<Seat> seatsRef;
    private List<SeatAction> actionsRef;

    private static Path resolveDataDir() {
        try {
            URL url = JsonStore.class.getProtectionDomain().getCodeSource().getLocation();
            Path jarPath = Paths.get(url.toURI()).toAbsolutePath().normalize();
            Path baseDir;
            if (jarPath.getFileName() != null && jarPath.getFileName().toString().endsWith(".jar")) {
                baseDir = jarPath.getParent();
            } else {
                baseDir = Paths.get(".").toAbsolutePath().normalize();
            }
            Path dataDir = baseDir.resolve("data").normalize();
            System.out.println("Data dir: " + dataDir);
            return dataDir;
        } catch (Exception e) {
            Path fallback = Paths.get("data").toAbsolutePath().normalize();
            System.out.println("Data dir (fallback): " + fallback);
            return fallback;
        }
    }

    public JsonStore() {
        this.dataDir = resolveDataDir();
        this.seatsFile = dataDir.resolve("seats.json");
        this.actionsFile = dataDir.resolve("actions.json");
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startAutoSave(List<Seat> seats, List<SeatAction> actions, int intervalSeconds) {
        this.seatsRef = seats;
        this.actionsRef = actions;
        scheduler.scheduleAtFixedRate(this::save, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public void save() {
        try {
            if (seatsRef != null) {
                writeJson(seatsFile, seatsRef);
            }
            if (actionsRef != null) {
                writeJson(actionsFile, actionsRef);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        save();
        scheduler.shutdown();
    }

    public List<Seat> loadSeats() {
        return readJson(seatsFile, new TypeToken<List<Seat>>(){}.getType());
    }

    public List<SeatAction> loadActions() {
        return readJson(actionsFile, new TypeToken<List<SeatAction>>(){}.getType());
    }

    private <T> List<T> readJson(Path path, Type type) {
        if (!Files.exists(path)) return null;
        try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            List<T> result = gson.fromJson(reader, type);
            return result != null ? result : new ArrayList<T>();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void writeJson(Path path, Object data) throws IOException {
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        }
    }
}
