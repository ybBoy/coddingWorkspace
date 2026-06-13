package com.groupdraw.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.groupdraw.model.ActionLog;
import com.groupdraw.model.Group;
import com.groupdraw.model.Participant;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class JsonStore {
    private static final String DATA_DIR = "data";
    private static final String DATA_FILE = "groupdraw-data.json";
    private static final long SAVE_INTERVAL_MS = 30000;

    private Gson gson;
    private Timer saveTimer;
    private AppState currentState;

    public JsonStore() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.currentState = new AppState();
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

    public void save() {
        try {
            File file = new File(DATA_DIR, DATA_FILE);
            FileWriter writer = new FileWriter(file);
            gson.toJson(currentState, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean load() {
        try {
            File file = new File(DATA_DIR, DATA_FILE);
            if (!file.exists()) {
                return false;
            }
            FileReader reader = new FileReader(file);
            AppState state = gson.fromJson(reader, AppState.class);
            reader.close();
            if (state != null) {
                this.currentState = state;
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Participant> getParticipants() {
        return currentState.participants;
    }

    public void setParticipants(List<Participant> participants) {
        currentState.participants = participants;
    }

    public List<Group> getGroups() {
        return currentState.groups;
    }

    public void setGroups(List<Group> groups) {
        currentState.groups = groups;
    }

    public List<ActionLog> getActionLogs() {
        return currentState.actionLogs;
    }

    public void setActionLogs(List<ActionLog> actionLogs) {
        currentState.actionLogs = actionLogs;
    }

    public String getActivityName() {
        return currentState.activityName;
    }

    public void setActivityName(String activityName) {
        currentState.activityName = activityName;
    }

    public int getGroupCount() {
        return currentState.groupCount;
    }

    public void setGroupCount(int groupCount) {
        currentState.groupCount = groupCount;
    }

    private static class AppState {
        String activityName = "活动抽签分组";
        int groupCount = 4;
        List<Participant> participants = new ArrayList<Participant>();
        List<Group> groups = new ArrayList<Group>();
        List<ActionLog> actionLogs = new ArrayList<ActionLog>();

        public AppState() {
        }
    }
}
