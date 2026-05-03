package com.warehouse.service;

import com.warehouse.entity.Part;
import com.warehouse.entity.StockRecord;
import com.warehouse.store.PartDataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 零件服务类
 * 提供零件的增删改查、入库出库、搜索等核心业务逻辑
 * 支持管理员和普通用户两种权限视图
 */
@Service
public class PartService {

    @Autowired
    private PartDataStore partDataStore;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private StockRecordService stockRecordService;

    /**
     * 获取所有零件（管理员权限）
     * @return 所有零件列表
     */
    public List<Part> getAllParts() {
        return partDataStore.getAllParts();
    }

    /**
     * 获取所有可见零件（普通用户权限）
     * 只返回 visible = true 的零件
     * @return 可见零件列表
     */
    public List<Part> getVisibleParts() {
        return partDataStore.getVisibleParts();
    }

    /**
     * 根据编号获取零件（管理员权限）
     * @param id 零件编号
     * @return 零件对象，不存在则返回null
     */
    public Part getPartById(String id) {
        return partDataStore.getPartById(id);
    }

    /**
     * 根据编号获取可见零件（普通用户权限）
     * @param id 零件编号
     * @return 零件对象，不存在或不可见则返回null
     */
    public Part getVisiblePartById(String id) {
        return partDataStore.getVisiblePartById(id);
    }

    /**
     * 添加新零件
     * 检查编号是否已存在，存在则抛出异常
     * @param part 零件对象
     * @return 添加后的零件对象
     * @throws IllegalArgumentException 零件编号已存在时抛出
     */
    public Part addPart(Part part) {
        if (partDataStore.existsById(part.getId())) {
            throw new IllegalArgumentException("零件编号已存在: " + part.getId());
        }
        partDataStore.savePart(part);
        fileStorageService.saveData();
        return part;
    }

    /**
     * 更新零件信息
     * 检查零件是否存在，不存在则抛出异常
     * @param part 零件对象
     * @return 更新后的零件对象
     * @throws IllegalArgumentException 零件不存在时抛出
     */
    public Part updatePart(Part part) {
        if (!partDataStore.existsById(part.getId())) {
            throw new IllegalArgumentException("零件不存在: " + part.getId());
        }
        partDataStore.savePart(part);
        fileStorageService.saveData();
        return part;
    }

    /**
     * 删除零件
     * @param id 零件编号
     * @throws IllegalArgumentException 零件不存在时抛出
     */
    public void deletePart(String id) {
        if (!partDataStore.existsById(id)) {
            throw new IllegalArgumentException("零件不存在: " + id);
        }
        partDataStore.deletePart(id);
        fileStorageService.saveData();
    }

    /**
     * 零件入库操作
     * 增加库存数量并记录操作
     * @param id 零件编号
     * @param quantity 入库数量（必须大于0）
     * @return 更新后的零件对象
     * @throws IllegalArgumentException 数量不合法或零件不存在时抛出
     */
    public Part stockIn(String id, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("入库数量必须大于0");
        }
        Part part = partDataStore.getPartById(id);
        if (part == null) {
            throw new IllegalArgumentException("零件不存在: " + id);
        }
        
        int beforeQuantity = part.getQuantity();
        part.setQuantity(beforeQuantity + quantity);
        int afterQuantity = part.getQuantity();
        
        partDataStore.savePart(part);
        fileStorageService.saveData();
        
        StockRecord record = new StockRecord();
        record.setPartId(part.getId());
        record.setPartName(part.getName());
        record.setCategory(part.getCategory());
        record.setType("入库");
        record.setQuantity(quantity);
        record.setUnit(part.getUnit());
        record.setBeforeQuantity(beforeQuantity);
        record.setAfterQuantity(afterQuantity);
        stockRecordService.addRecord(record);
        
        return part;
    }

    /**
     * 零件出库操作
     * 减少库存数量并记录操作，检查库存是否充足
     * @param id 零件编号
     * @param quantity 出库数量（必须大于0）
     * @return 更新后的零件对象
     * @throws IllegalArgumentException 数量不合法、零件不存在或库存不足时抛出
     */
    public Part stockOut(String id, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("出库数量必须大于0");
        }
        Part part = partDataStore.getPartById(id);
        if (part == null) {
            throw new IllegalArgumentException("零件不存在: " + id);
        }
        if (part.getQuantity() < quantity) {
            throw new IllegalArgumentException("库存不足，当前库存: " + part.getQuantity() + ", 出库数量: " + quantity);
        }
        
        int beforeQuantity = part.getQuantity();
        part.setQuantity(beforeQuantity - quantity);
        int afterQuantity = part.getQuantity();
        
        partDataStore.savePart(part);
        fileStorageService.saveData();
        
        StockRecord record = new StockRecord();
        record.setPartId(part.getId());
        record.setPartName(part.getName());
        record.setCategory(part.getCategory());
        record.setType("出库");
        record.setQuantity(quantity);
        record.setUnit(part.getUnit());
        record.setBeforeQuantity(beforeQuantity);
        record.setAfterQuantity(afterQuantity);
        stockRecordService.addRecord(record);
        
        return part;
    }

    /**
     * 搜索零件（管理员权限）
     * 按零件编号或名称模糊搜索
     * @param keyword 搜索关键词
     * @return 匹配的零件列表
     */
    public List<Part> searchParts(String keyword) {
        return partDataStore.searchParts(keyword);
    }

    /**
     * 搜索零件（管理员权限）
     * 按关键词和分类组合搜索
     * @param keyword 搜索关键词（可选）
     * @param category 分类（可选）
     * @return 匹配的零件列表
     */
    public List<Part> searchPartsByKeywordAndCategory(String keyword, String category) {
        return partDataStore.searchPartsByKeywordAndCategory(keyword, category);
    }

    /**
     * 搜索可见零件（普通用户权限）
     * @param keyword 搜索关键词
     * @return 匹配的可见零件列表
     */
    public List<Part> searchVisibleParts(String keyword) {
        return partDataStore.searchVisibleParts(keyword);
    }

    /**
     * 搜索可见零件（普通用户权限）
     * 按关键词和分类组合搜索
     * @param keyword 搜索关键词（可选）
     * @param category 分类（可选）
     * @return 匹配的可见零件列表
     */
    public List<Part> searchVisiblePartsByKeywordAndCategory(String keyword, String category) {
        return partDataStore.searchVisiblePartsByKeywordAndCategory(keyword, category);
    }

    /**
     * 获取需要补货的零件列表（管理员权限）
     * 返回库存数量 <= 最低库存预警值的零件
     * @return 需要补货的零件列表
     */
    public List<Part> getPartsNeedRestock() {
        return partDataStore.getPartsNeedRestock();
    }

    /**
     * 获取需要补货的可见零件列表（普通用户权限）
     * @return 需要补货的可见零件列表
     */
    public List<Part> getVisiblePartsNeedRestock() {
        return partDataStore.getVisiblePartsNeedRestock();
    }

    /**
     * 更新零件可见性
     * 管理员可设置零件是否对普通用户可见
     * @param id 零件编号
     * @param visible 是否可见
     * @return 更新后的零件对象
     * @throws IllegalArgumentException 零件不存在时抛出
     */
    public Part updateVisibility(String id, boolean visible) {
        Part part = partDataStore.getPartById(id);
        if (part == null) {
            throw new IllegalArgumentException("零件不存在: " + id);
        }
        part.setVisible(visible);
        partDataStore.savePart(part);
        fileStorageService.saveData();
        return part;
    }

    /**
     * 零件入库操作（带IP记录）
     * 增加库存数量并记录操作人IP地址
     * @param id 零件编号
     * @param quantity 入库数量（必须大于0）
     * @param ipAddress 操作人IP地址
     * @return 更新后的零件对象
     * @throws IllegalArgumentException 数量不合法或零件不存在时抛出
     */
    public Part stockIn(String id, int quantity, String ipAddress) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("入库数量必须大于0");
        }
        Part part = partDataStore.getPartById(id);
        if (part == null) {
            throw new IllegalArgumentException("零件不存在: " + id);
        }
        
        int beforeQuantity = part.getQuantity();
        part.setQuantity(beforeQuantity + quantity);
        int afterQuantity = part.getQuantity();
        
        partDataStore.savePart(part);
        fileStorageService.saveData();
        
        StockRecord record = new StockRecord();
        record.setPartId(part.getId());
        record.setPartName(part.getName());
        record.setCategory(part.getCategory());
        record.setType("入库");
        record.setQuantity(quantity);
        record.setUnit(part.getUnit());
        record.setBeforeQuantity(beforeQuantity);
        record.setAfterQuantity(afterQuantity);
        record.setIpAddress(ipAddress);
        stockRecordService.addRecord(record);
        
        return part;
    }

    /**
     * 零件出库操作（带IP记录）
     * 减少库存数量并记录操作人IP地址，检查库存是否充足
     * @param id 零件编号
     * @param quantity 出库数量（必须大于0）
     * @param ipAddress 操作人IP地址
     * @return 更新后的零件对象
     * @throws IllegalArgumentException 数量不合法、零件不存在或库存不足时抛出
     */
    public Part stockOut(String id, int quantity, String ipAddress) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("出库数量必须大于0");
        }
        Part part = partDataStore.getPartById(id);
        if (part == null) {
            throw new IllegalArgumentException("零件不存在: " + id);
        }
        if (part.getQuantity() < quantity) {
            throw new IllegalArgumentException("库存不足，当前库存: " + part.getQuantity() + ", 出库数量: " + quantity);
        }
        
        int beforeQuantity = part.getQuantity();
        part.setQuantity(beforeQuantity - quantity);
        int afterQuantity = part.getQuantity();
        
        partDataStore.savePart(part);
        fileStorageService.saveData();
        
        StockRecord record = new StockRecord();
        record.setPartId(part.getId());
        record.setPartName(part.getName());
        record.setCategory(part.getCategory());
        record.setType("出库");
        record.setQuantity(quantity);
        record.setUnit(part.getUnit());
        record.setBeforeQuantity(beforeQuantity);
        record.setAfterQuantity(afterQuantity);
        record.setIpAddress(ipAddress);
        stockRecordService.addRecord(record);
        
        return part;
    }
}
