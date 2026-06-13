package io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import entity.Room;
import entity.RoomLog;
import entity.StayRecord;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RoomJsonStore {

    private static final String DATA_DIR = "data";
    private static final String ROOMS_FILE = DATA_DIR + File.separator + "rooms.json";
    private static final String STAY_RECORDS_FILE = DATA_DIR + File.separator + "stay_records.json";
    private static final String LOGS_FILE = DATA_DIR + File.separator + "logs.json";

    private final Gson gson;
    private final ScheduledExecutorService scheduler;

    private List<Room> rooms = new ArrayList<>();
    private List<StayRecord> stayRecords = new ArrayList<>();
    private List<RoomLog> logs = new ArrayList<>();

    public RoomJsonStore() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        loadFromDisk();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                saveToDisk();
            }
        }, 30, 30, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                saveToDisk();
                scheduler.shutdown();
            }
        }));
    }

    private void loadFromDisk() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
            initDefaultData();
            saveToDisk();
            return;
        }

        try {
            rooms = readList(ROOMS_FILE, new TypeToken<List<Room>>() {}.getType());
            stayRecords = readList(STAY_RECORDS_FILE, new TypeToken<List<StayRecord>>() {}.getType());
            logs = readList(LOGS_FILE, new TypeToken<List<RoomLog>>() {}.getType());

            if (rooms.isEmpty()) {
                initDefaultData();
                saveToDisk();
            } else {
                rebuildCurrentStay();
            }
        } catch (Exception e) {
            System.err.println("[RoomJsonStore] Load failed, using defaults: " + e.getMessage());
            initDefaultData();
            saveToDisk();
        }
    }

    private <T> List<T> readList(String filePath, Type type) {
        File file = new File(filePath);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (Reader reader = new FileReader(file)) {
            List<T> result = gson.fromJson(reader, type);
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("[RoomJsonStore] Read " + filePath + " failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void saveToDisk() {
        try {
            File dir = new File(DATA_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            writeList(ROOMS_FILE, rooms);
            writeList(STAY_RECORDS_FILE, stayRecords);
            writeList(LOGS_FILE, logs);
        } catch (Exception e) {
            System.err.println("[RoomJsonStore] Save failed: " + e.getMessage());
        }
    }

    private <T> void writeList(String filePath, List<T> list) {
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(list, writer);
        } catch (Exception e) {
            System.err.println("[RoomJsonStore] Write " + filePath + " failed: " + e.getMessage());
        }
    }

    private void rebuildCurrentStay() {
        for (Room room : rooms) {
            if (room.getStatus() == entity.RoomStatus.OCCUPIED) {
                for (StayRecord record : stayRecords) {
                    if (record.getRoomId().equals(room.getId()) && record.getActualCheckOutTime() == null) {
                        room.setCurrentStay(record);
                        break;
                    }
                }
            }
        }
    }

    private void initDefaultData() {
        rooms.clear();
        stayRecords.clear();
        logs.clear();

        String[] types = {"标准间", "大床房", "豪华间"};
        int idCounter = 1;

        for (int floor = 1; floor <= 3; floor++) {
            for (int roomNum = 1; roomNum <= 5; roomNum++) {
                String roomNo = String.format("%d%02d", floor, roomNum);
                String id = "room-" + idCounter++;
                String type = types[(roomNum - 1) % types.length];
                Room room = new Room(id, roomNo, floor, entity.RoomStatus.VACANT, type);
                rooms.add(room);
            }
        }
    }

    public void forceSave() {
        saveToDisk();
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public List<StayRecord> getStayRecords() {
        return stayRecords;
    }

    public List<RoomLog> getLogs() {
        return logs;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }

    public void setStayRecords(List<StayRecord> stayRecords) {
        this.stayRecords = stayRecords;
    }

    public void setLogs(List<RoomLog> logs) {
        this.logs = logs;
    }
}
