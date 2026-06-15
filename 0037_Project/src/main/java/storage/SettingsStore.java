package storage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class SettingsStore {
    private final File dataFile;
    private final File tmpFile;
    private final File backupFile;
    private static final int DEFAULT_WEEKLY_GOAL = 150;

    public SettingsStore(String filePath) {
        this.dataFile = new File(filePath);
        this.tmpFile = new File(filePath + ".tmp");
        this.backupFile = new File(filePath + ".bak");
    }

    public int loadWeeklyGoal() {
        if (!dataFile.exists()) {
            return DEFAULT_WEEKLY_GOAL;
        }

        try {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }

            Map<String, Object> settings = parseSettings(content.toString());
            Object goal = settings.get("weeklyGoal");
            if (goal instanceof Number) {
                return ((Number) goal).intValue();
            }
            if (goal instanceof String) {
                try {
                    return Integer.parseInt((String) goal);
                } catch (NumberFormatException e) {
                }
            }
        } catch (Exception e) {
            System.err.println("读取设置失败，使用默认值: " + e.getMessage());
        }

        return DEFAULT_WEEKLY_GOAL;
    }

    public void saveWeeklyGoal(int minutes) {
        String json = "{\"weeklyGoal\":" + minutes + "}";

        synchronized (this) {
            try {
                File parentDir = dataFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                try (BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(tmpFile), StandardCharsets.UTF_8))) {
                    writer.write(json);
                    writer.flush();
                }

                if (tmpFile.length() == 0) {
                    throw new IOException("临时文件为空");
                }

                if (dataFile.exists()) {
                    Files.copy(dataFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                Files.move(tmpFile.toPath(), dataFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            } catch (IOException e) {
                System.err.println("保存设置失败: " + e.getMessage());
                if (tmpFile.exists()) {
                    tmpFile.delete();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSettings(String json) {
        JsonUtil.JsonParser parser = new JsonUtil.JsonParser(json);
        parser.skipWhitespace();
        Map<String, Object> result = (Map<String, Object>) parser.parseObject();
        return result != null ? result : new java.util.HashMap<String, Object>();
    }
}
