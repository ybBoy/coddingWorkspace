package persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import domain.Plant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlantJsonStore {
    private final String filePath;
    private final ObjectMapper objectMapper;

    public PlantJsonStore(String filePath) {
        this.filePath = filePath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public List<Plant> loadPlants() {
        File file = new File(filePath);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try {
            List<Plant> plants = objectMapper.readValue(file, new TypeReference<List<Plant>>() {});
            return plants != null ? plants : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Error loading plants from JSON: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void savePlants(List<Plant> plants) {
        try {
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            objectMapper.writeValue(file, plants);
        } catch (IOException e) {
            System.err.println("Error saving plants to JSON: " + e.getMessage());
        }
    }
}
