package com.warehouse.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehouse.entity.Part;
import com.warehouse.entity.StockRecord;
import com.warehouse.store.PartDataStore;
import com.warehouse.store.StockRecordDataStore;
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

/**
 * 文件存储服务类
 * 负责将零件数据和库存记录持久化到JSON文件
 * 系统启动时自动加载数据，数据变更时自动保存
 * 
 * 数据存储位置：
 * - 零件数据：{data.directory}/parts.json
 * - 库存记录：{data.directory}/records.json
 */
@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    
    /**
     * 零件数据文件名
     */
    private static final String PARTS_FILE_NAME = "parts.json";
    
    /**
     * 库存记录文件名
     */
    private static final String RECORDS_FILE_NAME = "records.json";

    /**
     * 数据存储目录路径
     * 可通过 application.properties 中的 data.directory 配置
     * 默认值：./data
     */
    @Value("${data.directory:./data}")
    private String dataDirectory;

    /**
     * Jackson对象映射器
     * 用于Java对象与JSON之间的序列化和反序列化
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PartDataStore partDataStore;

    @Autowired
    private StockRecordDataStore stockRecordDataStore;

    /**
     * 初始化方法
     * Spring容器启动后自动执行
     * 执行顺序：创建数据目录 -> 加载零件数据 -> 加载库存记录
     */
    @PostConstruct
    public void init() {
        createDataDirectory();
        loadData();
        loadRecords();
    }

    /**
     * 创建数据存储目录
     * 如果目录不存在则创建
     */
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

    /**
     * 保存零件数据到JSON文件
     * 每次零件数据变更后调用此方法持久化
     */
    public void saveData() {
        try {
            File dataFile = new File(dataDirectory, PARTS_FILE_NAME);
            List<Part> parts = partDataStore.getAllParts();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(dataFile, parts);
            logger.info("零件数据保存成功: 共 {} 条记录", parts.size());
        } catch (IOException e) {
            logger.error("零件数据保存失败", e);
        }
    }

    /**
     * 从JSON文件加载零件数据
     * 如果文件不存在或加载失败，使用示例数据初始化
     */
    public void loadData() {
        try {
            File dataFile = new File(dataDirectory, PARTS_FILE_NAME);
            if (!dataFile.exists()) {
                logger.info("零件数据文件不存在，使用示例数据");
                initSampleData();
                return;
            }

            List<Part> parts = objectMapper.readValue(dataFile, new TypeReference<List<Part>>() {});
            Map<String, Part> partsMap = new HashMap<>();
            for (Part part : parts) {
                partsMap.put(part.getId(), part);
            }
            partDataStore.setPartsMap(partsMap);
            logger.info("零件数据加载成功: 共 {} 条记录", parts.size());
        } catch (IOException e) {
            logger.error("零件数据加载失败，使用示例数据", e);
            initSampleData();
        }
    }

    /**
     * 保存库存记录到JSON文件
     * 每次库存操作后调用此方法持久化
     */
    public void saveRecords() {
        try {
            File recordsFile = new File(dataDirectory, RECORDS_FILE_NAME);
            List<StockRecord> records = stockRecordDataStore.getAllRecords();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(recordsFile, records);
            logger.info("出入库记录保存成功: 共 {} 条记录", records.size());
        } catch (IOException e) {
            logger.error("出入库记录保存失败", e);
        }
    }

    /**
     * 从JSON文件加载库存记录
     * 如果文件不存在则使用空数据
     */
    public void loadRecords() {
        try {
            File recordsFile = new File(dataDirectory, RECORDS_FILE_NAME);
            if (!recordsFile.exists()) {
                logger.info("出入库记录文件不存在，使用空数据");
                return;
            }

            List<StockRecord> records = objectMapper.readValue(recordsFile, new TypeReference<List<StockRecord>>() {});
            Map<String, StockRecord> recordsMap = new HashMap<>();
            for (StockRecord record : records) {
                recordsMap.put(record.getId(), record);
            }
            stockRecordDataStore.setRecordsMap(recordsMap);
            logger.info("出入库记录加载成功: 共 {} 条记录", records.size());
        } catch (IOException e) {
            logger.error("出入库记录加载失败，使用空数据", e);
        }
    }

    /**
     * 初始化示例零件数据
     * 当数据文件不存在或加载失败时调用
     * 包含电动汽车常用零件的示例数据
     */
    private void initSampleData() {
        logger.info("初始化示例零件数据...");
        
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
        logger.info("示例零件数据初始化完成");
    }

    /**
     * 添加单个示例零件
     * @param id 零件编号
     * @param name 零件名称
     * @param category 分类
     * @param spec 规格
     * @param qty 库存数量
     * @param minStock 最低库存预警值
     * @param unit 计量单位
     */
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
