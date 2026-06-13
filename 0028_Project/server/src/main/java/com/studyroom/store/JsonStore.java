package com.studyroom.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.studyroom.model.Seat;
import com.studyroom.model.SeatAction;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JsonStore {
    private static final String DATA_DIR = "data";
    private static final String SEATS_FILE = DATA_DIR + File.separator + "seats.json";
    private static final String ACTIONS_FILE = DATA_DIR + File.separator + "actions.json";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private List<Seat> seatsRef;
    private List<SeatAction> actionsRef;

    public JsonStore() {
        new File(DATA_DIR).mkdirs();
    }

    public void startAutoSave(List<Seat> seats, List<SeatAction> actions, int intervalSeconds) {
        this.seatsRef = seats;
        this.actionsRef = actions;
        scheduler.scheduleAtFixedRate(this::save, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public void save() {
        try {
            if (seatsRef != null) {
                writeJson(SEATS_FILE, seatsRef);
            }
            if (actionsRef != null) {
                writeJson(ACTIONS_FILE, actionsRef);
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
        return readJson(SEATS_FILE, new TypeToken<List<Seat>>(){}.getType());
    }

    public List<SeatAction> loadActions() {
        return readJson(ACTIONS_FILE, new TypeToken<List<SeatAction>>(){}.getType());
    }

    private <T> List<T> readJson(String path, Type type) {
        File file = new File(path);
        if (!file.exists()) return null;
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            List<T> result = gson.fromJson(reader, type);
            return result != null ? result : new ArrayList<T>();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void writeJson(String path, Object data) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        }
    }
}
