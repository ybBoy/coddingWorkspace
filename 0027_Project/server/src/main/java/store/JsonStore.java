package store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.Booth;
import model.CheckInRecord;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class JsonStore {

    private static final String DATA_PATH = "./data/expo-data.json";
    private final Gson gson;

    public JsonStore() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public static class StoreData {
        public List<Booth> booths;
        public List<CheckInRecord> records;

        public StoreData() {
            this.booths = new ArrayList<>();
            this.records = new ArrayList<>();
        }

        public StoreData(List<Booth> booths, List<CheckInRecord> records) {
            this.booths = booths;
            this.records = records;
        }
    }

    public void save(StoreData data) {
        File file = new File(DATA_PATH);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public StoreData load() {
        File file = new File(DATA_PATH);
        if (!file.exists()) {
            return createDefaultData();
        }
        try (Reader reader = new FileReader(file)) {
            StoreData data = gson.fromJson(reader, StoreData.class);
            if (data == null || data.booths == null || data.booths.isEmpty()) {
                return createDefaultData();
            }
            if (data.records == null) {
                data.records = new ArrayList<>();
            }
            return data;
        } catch (IOException e) {
            e.printStackTrace();
            return createDefaultData();
        }
    }

    private StoreData createDefaultData() {
        StoreData data = new StoreData();
        data.booths = new ArrayList<>();
        data.booths.add(new Booth("B001", "人工智能展", "展示最新的人工智能技术与应用"));
        data.booths.add(new Booth("B002", "智能硬件展", "展示创新的智能硬件产品"));
        data.booths.add(new Booth("B003", "数字文创展", "展示数字文化创意产业成果"));
        data.records = new ArrayList<>();
        return data;
    }

    public void startAutoSave(final Runnable dataSupplier, long intervalMs) {
        Thread autoSaveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(intervalMs);
                        dataSupplier.run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "AutoSaveThread");
        autoSaveThread.setDaemon(true);
        autoSaveThread.start();
    }
}
