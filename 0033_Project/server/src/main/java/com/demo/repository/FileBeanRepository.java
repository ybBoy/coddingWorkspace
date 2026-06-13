package com.demo.repository;

import com.demo.model.CoffeeBean;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class FileBeanRepository {

    @Value("${inventory.data.file:./data/beans.json}")
    private String dataFilePath;

    private ObjectMapper objectMapper;
    private List<CoffeeBean> beans;

    @PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        loadFromFile();
    }

    private void loadFromFile() {
        File file = new File(dataFilePath);
        if (file.exists()) {
            try {
                beans = objectMapper.readValue(file, new TypeReference<List<CoffeeBean>>() {});
            } catch (IOException e) {
                beans = new ArrayList<>();
                e.printStackTrace();
            }
        } else {
            beans = new ArrayList<>();
            saveToFile();
        }
    }

    private void saveToFile() {
        try {
            File file = new File(dataFilePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            objectMapper.writeValue(file, beans);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<CoffeeBean> findAll() {
        return new ArrayList<>(beans);
    }

    public Optional<CoffeeBean> findById(String id) {
        return beans.stream()
                .filter(bean -> bean.getId().equals(id))
                .findFirst();
    }

    public CoffeeBean save(CoffeeBean bean) {
        int index = -1;
        for (int i = 0; i < beans.size(); i++) {
            if (beans.get(i).getId().equals(bean.getId())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            beans.set(index, bean);
        } else {
            beans.add(bean);
        }
        saveToFile();
        return bean;
    }

    public boolean deleteById(String id) {
        boolean removed = beans.removeIf(bean -> bean.getId().equals(id));
        if (removed) {
            saveToFile();
        }
        return removed;
    }
}
