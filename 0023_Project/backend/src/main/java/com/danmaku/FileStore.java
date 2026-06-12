package com.danmaku;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FileStore {
    private static final String DATA_DIR = "data";
    private static final String MESSAGES_FILE = "messages.json";
    private static final String SETTINGS_FILE = "settings.json";
    private static final Gson gson = new Gson();

    static {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static void saveMessages(List<Message> messages) {
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(DATA_DIR + "/" + MESSAGES_FILE), StandardCharsets.UTF_8)) {
            gson.toJson(messages, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Message> loadMessages() {
        File file = new File(DATA_DIR + "/" + MESSAGES_FILE);
        if (!file.exists()) {
            return new ArrayList<Message>();
        }
        try (Reader reader = new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, new TypeToken<List<Message>>() {}.getType());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<Message>();
        }
    }

    public static void saveSettings(Settings settings) {
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(DATA_DIR + "/" + SETTINGS_FILE), StandardCharsets.UTF_8)) {
            gson.toJson(settings, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Settings loadSettings() {
        File file = new File(DATA_DIR + "/" + SETTINGS_FILE);
        if (!file.exists()) {
            return new Settings(true, new ArrayList<String>());
        }
        try (Reader reader = new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8)) {
            Settings settings = gson.fromJson(reader, Settings.class);
            if (settings == null) {
                return new Settings(true, new ArrayList<String>());
            }
            return settings;
        } catch (IOException e) {
            e.printStackTrace();
            return new Settings(true, new ArrayList<String>());
        }
    }

    public static class Settings {
        private boolean sendingEnabled;
        private List<String> sensitiveWords;

        public Settings() {}

        public Settings(boolean sendingEnabled, List<String> sensitiveWords) {
            this.sendingEnabled = sendingEnabled;
            this.sensitiveWords = sensitiveWords;
        }

        public boolean isSendingEnabled() { return sendingEnabled; }
        public void setSendingEnabled(boolean sendingEnabled) { this.sendingEnabled = sendingEnabled; }

        public List<String> getSensitiveWords() { return sensitiveWords; }
        public void setSensitiveWords(List<String> sensitiveWords) { this.sensitiveWords = sensitiveWords; }
    }
}
