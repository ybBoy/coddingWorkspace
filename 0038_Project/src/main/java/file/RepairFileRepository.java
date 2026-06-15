package file;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import entity.RepairItem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RepairFileRepository {
    private static final String DATA_FILE = "data/repairs.json";
    private final ObjectMapper objectMapper;

    public RepairFileRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void saveAll(List<RepairItem> items) throws IOException {
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        objectMapper.writeValue(new File(DATA_FILE), items);
    }

    public List<RepairItem> loadAll() throws IOException {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        return objectMapper.readValue(file, new TypeReference<List<RepairItem>>() {});
    }
}
