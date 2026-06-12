package com.danmaku;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class FileStore {
    private static final String DATA_DIR = "data";
    private static final String BACKUP_DIR = "data/backup";
    private static final String MESSAGES_FILE = "messages.json";
    private static final String SETTINGS_FILE = "settings.json";
    private static final String LOGS_FILE = "logs.json";
    private static final Gson gson = new Gson();

    static {
        new File(DATA_DIR).mkdirs();
        new File(BACKUP_DIR).mkdirs();
    }

    public static void saveMessages(List<Message> messages) {
        writeJson(DATA_DIR + "/" + MESSAGES_FILE, messages);
    }

    public static List<Message> loadMessages() {
        return readJson(DATA_DIR + "/" + MESSAGES_FILE,
                new TypeToken<List<Message>>() {}.getType(), new ArrayList<Message>());
    }

    public static void saveSettings(Settings settings) {
        writeJson(DATA_DIR + "/" + SETTINGS_FILE, settings);
    }

    public static Settings loadSettings() {
        Settings s = readJson(DATA_DIR + "/" + SETTINGS_FILE, Settings.class, null);
        return s != null ? s : new Settings();
    }

    public static void saveLogs(List<OperationLog> logs) {
        writeJson(DATA_DIR + "/" + LOGS_FILE, logs);
    }

    public static List<OperationLog> loadLogs() {
        return readJson(DATA_DIR + "/" + LOGS_FILE,
                new TypeToken<List<OperationLog>>() {}.getType(), new ArrayList<OperationLog>());
    }

    public static void rotateBackup() {
        try {
            String ts = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            Path src = Paths.get(DATA_DIR, MESSAGES_FILE);
            if (Files.exists(src)) {
                Path dst = Paths.get(BACKUP_DIR, "messages-" + ts + ".json");
                Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
            }
            Path srcSet = Paths.get(DATA_DIR, SETTINGS_FILE);
            if (Files.exists(srcSet)) {
                Path dstSet = Paths.get(BACKUP_DIR, "settings-" + ts + ".json");
                Files.copy(srcSet, dstSet, StandardCopyOption.REPLACE_EXISTING);
            }
            cleanOldBackups(10);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String exportData() {
        try {
            String ts = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            String exportDir = "data/export";
            new File(exportDir).mkdirs();
            Path src = Paths.get(DATA_DIR, MESSAGES_FILE);
            if (Files.exists(src)) {
                Path dst = Paths.get(exportDir, "danmaku-export-" + ts + ".json");
                Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                return dst.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void cleanOldBackups(int keepCount) {
        File dir = new File(BACKUP_DIR);
        File[] files = dir.listFiles((d, name) -> name.startsWith("messages-"));
        if (files != null && files.length > keepCount) {
            List<File> sorted = new ArrayList<File>();
            for (File f : files) sorted.add(f);
            Collections.sort(sorted, new Comparator<File>() {
                public int compare(File a, File b) {
                    return Long.compare(b.lastModified(), a.lastModified());
                }
            });
            for (int i = keepCount; i < sorted.size(); i++) {
                sorted.get(i).delete();
            }
        }
    }

    private static void writeJson(String path, Object obj) {
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(path), StandardCharsets.UTF_8)) {
            gson.toJson(obj, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static <T> T readJson(String path, TypeToken<T> type, T defaultVal) {
        if (!new File(path).exists()) return defaultVal;
        try (Reader reader = new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8)) {
            T result = gson.fromJson(reader, type.getType());
            return result != null ? result : defaultVal;
        } catch (IOException e) {
            e.printStackTrace();
            return defaultVal;
        }
    }

    private static <T> T readJson(String path, Class<T> clazz, T defaultVal) {
        if (!new File(path).exists()) return defaultVal;
        try (Reader reader = new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8)) {
            T result = gson.fromJson(reader, clazz);
            return result != null ? result : defaultVal;
        } catch (IOException e) {
            e.printStackTrace();
            return defaultVal;
        }
    }

    public static class Settings {
        private boolean sendingEnabled = true;
        private List<String> sensitiveWords = new ArrayList<String>();
        private String eventTitle = "Live Danmaku Wall";
        private String welcomeMessage = "Send your message!";
        private String colorTheme = "rainbow";
        private List<String> customColors;
        private String moderatorPassword = "admin123";
        private int speedMin = 8;
        private int speedMax = 14;
        private int fontSize = 28;
        private int trackCount = 12;

        public Settings() {
            sensitiveWords.add("spam");
            sensitiveWords.add("ad");
            sensitiveWords.add("gamble");
        }

        public boolean isSendingEnabled() { return sendingEnabled; }
        public void setSendingEnabled(boolean v) { this.sendingEnabled = v; }

        public List<String> getSensitiveWords() { return sensitiveWords; }
        public void setSensitiveWords(List<String> v) { this.sensitiveWords = v; }

        public String getEventTitle() { return eventTitle; }
        public void setEventTitle(String v) { this.eventTitle = v; }

        public String getWelcomeMessage() { return welcomeMessage; }
        public void setWelcomeMessage(String v) { this.welcomeMessage = v; }

        public String getColorTheme() { return colorTheme; }
        public void setColorTheme(String v) { this.colorTheme = v; }

        public List<String> getCustomColors() { return customColors; }
        public void setCustomColors(List<String> v) { this.customColors = v; }

        public String getModeratorPassword() { return moderatorPassword; }
        public void setModeratorPassword(String v) { this.moderatorPassword = v; }

        public int getSpeedMin() { return speedMin; }
        public void setSpeedMin(int v) { this.speedMin = v; }

        public int getSpeedMax() { return speedMax; }
        public void setSpeedMax(int v) { this.speedMax = v; }

        public int getFontSize() { return fontSize; }
        public void setFontSize(int v) { this.fontSize = v; }

        public int getTrackCount() { return trackCount; }
        public void setTrackCount(int v) { this.trackCount = v; }
    }
}
