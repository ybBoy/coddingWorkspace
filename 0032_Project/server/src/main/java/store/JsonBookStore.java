package store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.Book;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonBookStore {
    private static final String DATA_FILE = "books.json";
    private final ObjectMapper objectMapper;
    private final File file;

    public JsonBookStore() {
        this.objectMapper = new ObjectMapper();
        this.file = new File(DATA_FILE);
    }

    public synchronized List<Book> loadAll() {
        if (!file.exists()) {
            return new ArrayList<Book>();
        }
        try {
            return objectMapper.readValue(file, new TypeReference<List<Book>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<Book>();
        }
    }

    public synchronized void saveAll(List<Book> books) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, books);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
