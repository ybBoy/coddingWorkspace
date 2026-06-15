package storage;

import domain.FitnessCheckin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CheckinFileStore {
    private final File dataFile;

    public CheckinFileStore(String filePath) {
        this.dataFile = new File(filePath);
    }

    public List<FitnessCheckin> loadAll() {
        if (!dataFile.exists()) {
            return new java.util.ArrayList<>();
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            return JsonUtil.parseList(content.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    public void saveAll(List<FitnessCheckin> records) {
        try {
            File parentDir = dataFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.UTF_8))) {
            writer.write(JsonUtil.toJson(records));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
