package com.warehouse.service;

import com.warehouse.entity.Part;
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
        part.setQuantity(part.getQuantity() + quantity);
        partDataStore.savePart(part);
        fileStorageService.saveData();
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
        part.setQuantity(part.getQuantity() - quantity);
        partDataStore.savePart(part);
        fileStorageService.saveData();
        return part;
    }

    public List<Part> searchParts(String keyword) {
        return partDataStore.searchParts(keyword);
    }

    public List<Part> getPartsNeedRestock() {
        return partDataStore.getPartsNeedRestock();
    }
}
