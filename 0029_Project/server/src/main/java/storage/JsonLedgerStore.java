package storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import domain.LedgerSnapshot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonLedgerStore {
    private final Path dataPath;
    private final ObjectMapper objectMapper;

    public JsonLedgerStore(String dataDir) {
        this.dataPath = Paths.get(dataDir, "ledger.json");
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void save(LedgerSnapshot snapshot) throws IOException {
        File parentDir = dataPath.getParent().toFile();
        if (!parentDir.exists()) {
            Files.createDirectories(dataPath.getParent());
        }
        objectMapper.writeValue(dataPath.toFile(), snapshot);
    }

    public LedgerSnapshot load() throws IOException {
        File file = dataPath.toFile();
        if (!file.exists()) {
            return new LedgerSnapshot();
        }
        return objectMapper.readValue(file, LedgerSnapshot.class);
    }
}
