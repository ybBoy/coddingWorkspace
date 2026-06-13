package store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import model.CareRecord;
import model.Pet;
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

    private final ObjectMapper objectMapper;
    private final File petsFile;
    private final File recordsFile;

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

    public synchronized List<Pet> loadPets() {
        if (!petsFile.exists()) {
            return new ArrayList<Pet>();
        }
        try {
            Pet[] petsArray = objectMapper.readValue(petsFile, Pet[].class);
            List<Pet> result = new ArrayList<Pet>();
            if (petsArray != null) {
                for (Pet pet : petsArray) {
                    result.add(pet);
                }
            }
            return result;
        } catch (IOException e) {
            logger.error("Failed to load pets data", e);
            return new ArrayList<Pet>();
        }
    }

    public synchronized List<CareRecord> loadCareRecords() {
        if (!recordsFile.exists()) {
            return new ArrayList<CareRecord>();
        }
        try {
            CareRecord[] recordsArray = objectMapper.readValue(recordsFile, CareRecord[].class);
            List<CareRecord> result = new ArrayList<CareRecord>();
            if (recordsArray != null) {
                for (CareRecord record : recordsArray) {
                    result.add(record);
                }
            }
            return result;
        } catch (IOException e) {
            logger.error("Failed to load care records data", e);
            return new ArrayList<CareRecord>();
        }
    }
}
