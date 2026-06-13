package store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import model.CareRecord;
import model.Pet;
import model.ReminderConfig;
import model.StatusChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonStore {
    private static final Logger logger = LoggerFactory.getLogger(JsonStore.class);
    private static final String DATA_DIR = "data";
    private static final String PETS_FILE = "pets.json";
    private static final String RECORDS_FILE = "care_records.json";
    private static final String STATUS_CHANGES_FILE = "status_changes.json";
    private static final String REMINDER_CONFIGS_FILE = "reminder_configs.json";

    private final ObjectMapper objectMapper;
    private final File petsFile;
    private final File recordsFile;
    private final File statusChangesFile;
    private final File reminderConfigsFile;

    public JsonStore() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.setDateFormat(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        this.objectMapper.setTimeZone(java.util.TimeZone.getTimeZone("GMT+8"));

        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        this.petsFile = new File(dataDir, PETS_FILE);
        this.recordsFile = new File(dataDir, RECORDS_FILE);
        this.statusChangesFile = new File(dataDir, STATUS_CHANGES_FILE);
        this.reminderConfigsFile = new File(dataDir, REMINDER_CONFIGS_FILE);
    }

    public synchronized void savePets(List<Pet> pets) {
        try {
            objectMapper.writeValue(petsFile, pets);
        } catch (IOException e) {
            logger.error("Failed to save pets data", e);
        }
    }

    public synchronized void saveCareRecords(List<CareRecord> records) {
        try {
            objectMapper.writeValue(recordsFile, records);
        } catch (IOException e) {
            logger.error("Failed to save care records data", e);
        }
    }

    public synchronized void saveStatusChanges(List<StatusChange> changes) {
        try {
            objectMapper.writeValue(statusChangesFile, changes);
        } catch (IOException e) {
            logger.error("Failed to save status changes data", e);
        }
    }

    public synchronized void saveReminderConfigs(List<ReminderConfig> configs) {
        try {
            objectMapper.writeValue(reminderConfigsFile, configs);
        } catch (IOException e) {
            logger.error("Failed to save reminder configs data", e);
        }
    }

    public synchronized List<Pet> loadPets() {
        return loadList(petsFile, Pet[].class);
    }

    public synchronized List<CareRecord> loadCareRecords() {
        return loadList(recordsFile, CareRecord[].class);
    }

    public synchronized List<StatusChange> loadStatusChanges() {
        return loadList(statusChangesFile, StatusChange[].class);
    }

    public synchronized List<ReminderConfig> loadReminderConfigs() {
        return loadList(reminderConfigsFile, ReminderConfig[].class);
    }

    private <T> List<T> loadList(File file, Class<T[]> arrayClass) {
        if (!file.exists()) {
            return new ArrayList<T>();
        }
        try {
            T[] array = objectMapper.readValue(file, arrayClass);
            List<T> result = new ArrayList<T>();
            if (array != null) {
                for (T item : array) {
                    result.add(item);
                }
            }
            return result;
        } catch (IOException e) {
            logger.error("Failed to load data from " + file.getName(), e);
            return new ArrayList<T>();
        }
    }
}
