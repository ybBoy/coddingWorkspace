package com.groupdraw.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.groupdraw.model.ActivityTemplate;
import com.groupdraw.model.Room;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Timer;
import java.util.TimerTask;

public class JsonStore {
    private static final String DATA_DIR = "data";
    private static final String DATA_FILE = "groupdraw-rooms.json";
    private static final String TEMPLATES_FILE = "groupdraw-templates.json";
    private static final long SAVE_INTERVAL_MS = 30000;

    private Gson gson;
    private Timer saveTimer;
    private Map<String, Room> rooms;
    private Map<String, ActivityTemplate> templates;

    public JsonStore() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.rooms = new ConcurrentHashMap<String, Room>();
        this.templates = new ConcurrentHashMap<String, ActivityTemplate>();
        ensureDataDir();
    }

    private void ensureDataDir() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void startAutoSave() {
        saveTimer = new Timer("JsonStore-AutoSave", true);
        saveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                save();
            }
        }, SAVE_INTERVAL_MS, SAVE_INTERVAL_MS);
    }

    public void stopAutoSave() {
        if (saveTimer != null) {
            saveTimer.cancel();
            saveTimer = null;
        }
    }

    public synchronized void save() {
        try {
            File file = new File(DATA_DIR, DATA_FILE);
            List<Room> roomList = new ArrayList<Room>(rooms.values());
            FileWriter writer = new FileWriter(file);
            gson.toJson(roomList, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        saveTemplates();
    }

    @SuppressWarnings("unchecked")
    public boolean load() {
        try {
            File file = new File(DATA_DIR, DATA_FILE);
            if (!file.exists()) {
                return false;
            }
            FileReader reader = new FileReader(file);
            Type listType = new TypeToken<ArrayList<Room>>(){}.getType();
            List<Room> roomList = gson.fromJson(reader, listType);
            reader.close();
            if (roomList != null) {
                rooms.clear();
                for (Room room : roomList) {
                    rooms.put(room.getCode(), room);
                }
                loadTemplates();
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Room getRoom(String code) {
        return rooms.get(code);
    }

    public void putRoom(Room room) {
        rooms.put(room.getCode(), room);
    }

    public void removeRoom(String code) {
        rooms.remove(code);
    }

    public List<Room> getAllRooms() {
        return new ArrayList<Room>(rooms.values());
    }

    public boolean hasRoom(String code) {
        return rooms.containsKey(code);
    }

    public synchronized void saveTemplates() {
        try {
            File file = new File(DATA_DIR, TEMPLATES_FILE);
            List<ActivityTemplate> templateList = new ArrayList<ActivityTemplate>(templates.values());
            FileWriter writer = new FileWriter(file);
            gson.toJson(templateList, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void loadTemplates() {
        try {
            File file = new File(DATA_DIR, TEMPLATES_FILE);
            if (!file.exists()) {
                return;
            }
            FileReader reader = new FileReader(file);
            Type listType = new TypeToken<ArrayList<ActivityTemplate>>(){}.getType();
            List<ActivityTemplate> templateList = gson.fromJson(reader, listType);
            reader.close();
            if (templateList != null) {
                templates.clear();
                for (ActivityTemplate t : templateList) {
                    templates.put(t.getId(), t);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveTemplate(ActivityTemplate t) {
        templates.put(t.getId(), t);
        saveTemplates();
    }

    public void deleteTemplate(String id) {
        templates.remove(id);
        saveTemplates();
    }

    public ActivityTemplate getTemplate(String id) {
        return templates.get(id);
    }

    public List<ActivityTemplate> getAllTemplates() {
        return new ArrayList<ActivityTemplate>(templates.values());
    }
}
