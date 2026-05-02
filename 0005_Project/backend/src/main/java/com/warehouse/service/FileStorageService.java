package com.warehouse.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehouse.entity.Part;
import com.warehouse.store.PartDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private static final String DATA_FILE_NAME = "parts.json";

    @Value("${data.directory:./data}")
    private String dataDirectory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PartDataStore partDataStore;

    @PostConstruct
    public void init() {
        createDataDirectory();
        loadData();
    }

    private void createDataDirectory() {
        File directory = new File(dataDirectory);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                logger.info("数据目录创建成功: {}", dataDirectory);
            } else {
                logger.error("数据目录创建失败: {}", dataDirectory);
            }
        }
    }

    public void saveData() {
        try {
            File dataFile = new File(dataDirectory, DATA_FILE_NAME);
            List<Part> parts = partDataStore.getAllParts();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(dataFile, parts);
            logger.info("数据保存成功: 共 {} 条记录", parts.size());
        } catch (IOException e) {
            logger.error("数据保存失败", e);
        }
    }

    public void loadData() {
        try {
            File dataFile = new File(dataDirectory, DATA_FILE_NAME);
            if (!dataFile.exists()) {
                logger.info("数据文件不存在，使用空数据");
                initSampleData();
                return;
            }

            List<Part> parts = objectMapper.readValue(dataFile, new TypeReference<List<Part>>() {});
            Map<String, Part> partsMap = new HashMap<>();
            for (Part part : parts) {
                partsMap.put(part.getId(), part);
            }
            partDataStore.setPartsMap(partsMap);
            logger.info("数据加载成功: 共 {} 条记录", parts.size());
        } catch (IOException e) {
            logger.error("数据加载失败，使用空数据", e);
            initSampleData();
        }
    }

    private void initSampleData() {
        logger.info("初始化示例数据...");
        
        addSamplePart("BAT-001", "锂离子电池", "电池", "50kWh", 100, 20, "块");
        addSamplePart("BAT-002", "锂离子电池", "电池", "75kWh", 80, 15, "块");
        addSamplePart("BAT-003", "磷酸铁锂电池", "电池", "100kWh", 50, 10, "块");
        
        addSamplePart("MOT-001", "永磁同步电机", "电机", "150kW", 60, 10, "台");
        addSamplePart("MOT-002", "永磁同步电机", "电机", "200kW", 40, 8, "台");
        addSamplePart("MOT-003", "异步电机", "电机", "120kW", 30, 5, "台");
        
        addSamplePart("TIR-001", "子午线轮胎", "轮胎", "225/55 R18", 200, 50, "条");
        addSamplePart("TIR-002", "子午线轮胎", "轮胎", "235/60 R19", 150, 40, "条");
        addSamplePart("TIR-003", "防爆轮胎", "轮胎", "245/45 R20", 80, 20, "条");
        
        addSamplePart("CTR-001", "整车控制器", "控制器", "VCU-100", 30, 5, "个");
        addSamplePart("CTR-002", "电机控制器", "控制器", "MCU-200", 25, 5, "个");
        addSamplePart("CTR-003", "电池管理系统", "控制器", "BMS-300", 20, 3, "个");
        
        addSamplePart("GLS-001", "前挡风玻璃", "玻璃", "福耀/180*120cm", 15, 5, "块");
        addSamplePart("GLS-002", "后挡风玻璃", "玻璃", "福耀/160*100cm", 12, 3, "块");
        addSamplePart("GLS-003", "侧窗玻璃", "玻璃", "信义/60*80cm", 30, 10, "块");
        
        addSamplePart("OIL-001", "齿轮油", "机油", "75W-90", 100, 20, "升");
        addSamplePart("OIL-002", "防冻液", "机油", "-40℃", 80, 15, "升");
        addSamplePart("OIL-003", "刹车油", "机油", "DOT4", 50, 10, "升");
        
        saveData();
        logger.info("示例数据初始化完成");
    }

    private void addSamplePart(String id, String name, String category, String spec, int qty, int minStock, String unit) {
        Part part = new Part();
        part.setId(id);
        part.setName(name);
        part.setCategory(category);
        part.setSpecification(spec);
        part.setQuantity(qty);
        part.setMinStock(minStock);
        part.setUnit(unit);
        partDataStore.savePart(part);
    }
}
