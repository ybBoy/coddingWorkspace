package com.monitoring.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
public class JsonFileStorage {

    @Value("${data.dir:./data}")
    private String dataDir;

    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            Path dataPath = Paths.get(dataDir);
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("无法创建数据目录: " + dataDir, e);
        }
    }

    public <T> void save(String fileName, T data) {
        try {
            File file = getFile(fileName);
            objectMapper.writeValue(file, data);
        } catch (IOException e) {
            throw new RuntimeException("保存文件失败: " + fileName, e);
        }
    }

    public <T> T load(String fileName, Class<T> clazz) {
        File file = getFile(fileName);
        if (!file.exists()) {
            return null;
        }
        try {
            return objectMapper.readValue(file, clazz);
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + fileName, e);
        }
    }

    public <T> List<T> loadList(String fileName, Class<T> clazz) {
        File file = getFile(fileName);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(file,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            throw new RuntimeException("读取列表文件失败: " + fileName, e);
        }
    }

    public boolean exists(String fileName) {
        return getFile(fileName).exists();
    }

    public void delete(String fileName) {
        File file = getFile(fileName);
        if (file.exists()) {
            file.delete();
        }
    }

    private File getFile(String fileName) {
        return new File(dataDir, fileName);
    }
}
