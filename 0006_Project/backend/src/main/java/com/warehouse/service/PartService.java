package com.warehouse.service;

import com.warehouse.entity.Part;
import com.warehouse.entity.StockRecord;
import com.warehouse.store.PartDataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PartService {

    @Autowired
    private PartDataStore partDataStore;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private StockRecordService stockRecordService;

    public List<Part> getAllParts() {
        return partDataStore.getAllParts();
    }

    public Part getPartById(String id) {
        return partDataStore.getPartById(id);
    }

    public Part addPart(Part part) {
        if (partDataStore.existsById(part.getId())) {
            throw new IllegalArgumentException("零件编号已存在: " + part.getId());
        }
        partDataStore.savePart(part);
        fileStorageService.saveData();
        return part;
    }

    public Part updatePart(Part part) {
        if (!partDataStore.existsById(part.getId())) {
            throw new IllegalArgumentException("零件不存在: " + part.getId());
        }
        partDataStore.savePart(part);
        fileStorageService.saveData();
        return part;
    }

    public void deletePart(String id) {
        if (!partDataStore.existsById(id)) {
            throw new IllegalArgumentException("零件不存在: " + id);
        }
        partDataStore.deletePart(id);
        fileStorageService.saveData();
    }

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

    public List<Part> searchParts(String keyword) {
        return partDataStore.searchParts(keyword);
    }

    public List<Part> searchPartsByKeywordAndCategory(String keyword, String category) {
        return partDataStore.searchPartsByKeywordAndCategory(keyword, category);
    }

    public List<Part> getPartsNeedRestock() {
        return partDataStore.getPartsNeedRestock();
    }
}
