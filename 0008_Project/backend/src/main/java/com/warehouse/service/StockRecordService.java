package com.warehouse.service;

import com.warehouse.entity.StockRecord;
import com.warehouse.store.StockRecordDataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 库存记录服务类
 * 提供出入库记录的查询、统计和生成等功能
 * 用于追踪所有库存操作的历史记录
 */
@Service
public class StockRecordService {

    @Autowired
    private StockRecordDataStore stockRecordDataStore;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 记录编号计数器
     * 用于生成自增的记录序号
     */
    private int recordCounter = 0;

    /**
     * 获取所有库存记录
     * @return 所有库存记录列表
     */
    public List<StockRecord> getAllRecords() {
        return stockRecordDataStore.getAllRecords();
    }

    /**
     * 根据操作类型获取库存记录
     * @param type 操作类型（"入库" 或 "出库"）
     * @return 匹配的库存记录列表
     */
    public List<StockRecord> getRecordsByType(String type) {
        return stockRecordDataStore.getRecordsByType(type);
    }

    /**
     * 根据零件分类获取库存记录
     * @param category 零件分类
     * @return 匹配的库存记录列表
     */
    public List<StockRecord> getRecordsByCategory(String category) {
        return stockRecordDataStore.getRecordsByCategory(category);
    }

    /**
     * 根据零件编号获取库存记录
     * @param partId 零件编号
     * @return 该零件的所有库存操作记录
     */
    public List<StockRecord> getRecordsByPartId(String partId) {
        return stockRecordDataStore.getRecordsByPartId(partId);
    }

    /**
     * 根据IP地址获取库存记录
     * 用于追踪特定操作人的操作历史
     * @param ipAddress IP地址
     * @return 该IP地址发起的所有库存操作记录
     */
    public List<StockRecord> getRecordsByIpAddress(String ipAddress) {
        return stockRecordDataStore.getRecordsByIpAddress(ipAddress);
    }

    /**
     * 添加新的库存记录
     * 自动生成记录编号并保存到文件
     * @param record 库存记录对象
     */
    public void addRecord(StockRecord record) {
        record.setId(generateRecordId());
        stockRecordDataStore.saveRecord(record);
        fileStorageService.saveRecords();
    }

    /**
     * 获取按分类汇总的库存统计信息
     * 统计各分类的入库总数、出库总数和结余
     * @return 包含汇总信息的Map
     *         - categorySummary: 各分类详细统计列表
     *         - totalStockIn: 总入库数量
     *         - totalStockOut: 总出库数量
     *         - totalRecords: 总记录数
     */
    public Map<String, Object> getSummaryByCategory() {
        Map<String, Object> summary = new HashMap<>();
        List<StockRecord> allRecords = stockRecordDataStore.getAllRecords();
        
        Map<String, Integer> stockInByCategory = new HashMap<>();
        Map<String, Integer> stockOutByCategory = new HashMap<>();
        
        for (StockRecord record : allRecords) {
            String category = record.getCategory();
            int quantity = record.getQuantity();
            
            if ("入库".equals(record.getType())) {
                stockInByCategory.put(category, stockInByCategory.getOrDefault(category, 0) + quantity);
            } else {
                stockOutByCategory.put(category, stockOutByCategory.getOrDefault(category, 0) + quantity);
            }
        }
        
        List<Map<String, Object>> categorySummary = new ArrayList<>();
        Set<String> allCategories = new HashSet<>();
        allCategories.addAll(stockInByCategory.keySet());
        allCategories.addAll(stockOutByCategory.keySet());
        
        for (String category : allCategories) {
            Map<String, Object> item = new HashMap<>();
            item.put("category", category);
            item.put("stockInQuantity", stockInByCategory.getOrDefault(category, 0));
            item.put("stockOutQuantity", stockOutByCategory.getOrDefault(category, 0));
            item.put("balance", stockInByCategory.getOrDefault(category, 0) - stockOutByCategory.getOrDefault(category, 0));
            categorySummary.add(item);
        }
        
        summary.put("categorySummary", categorySummary);
        summary.put("totalStockIn", stockInByCategory.values().stream().mapToInt(Integer::intValue).sum());
        summary.put("totalStockOut", stockOutByCategory.values().stream().mapToInt(Integer::intValue).sum());
        summary.put("totalRecords", allRecords.size());
        
        return summary;
    }

    /**
     * 生成唯一的记录编号
     * 格式：REC-yyyyMMddHHmmss-0001
     * 使用synchronized保证线程安全
     * @return 生成的记录编号
     */
    private synchronized String generateRecordId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = sdf.format(new Date());
        recordCounter++;
        return "REC-" + timestamp + "-" + String.format("%04d", recordCounter);
    }
}
