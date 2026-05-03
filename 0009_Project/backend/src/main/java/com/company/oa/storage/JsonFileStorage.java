package com.company.oa.storage;

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
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class JsonFileStorage {

    @Value("${data.storage.path:data/}")
    private String storagePath;

    private ObjectMapper objectMapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            Path path = Paths.get(storagePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new RuntimeException("无法创建数据存储目录", e);
        }
    }

    public <T> void save(String fileName, List<T> data, Class<T> clazz) {
        lock.writeLock().lock();
        try {
            File file = new File(storagePath, fileName);
            objectMapper.writeValue(file, data);
        } catch (IOException e) {
            throw new RuntimeException("保存数据失败: " + fileName, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public <T> List<T> load(String fileName, Class<T> clazz) {
        lock.readLock().lock();
        try {
            File file = new File(storagePath, fileName);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(file, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            throw new RuntimeException("加载数据失败: " + fileName, e);
        } finally {
            lock.readLock().unlock();
        }
    }
}