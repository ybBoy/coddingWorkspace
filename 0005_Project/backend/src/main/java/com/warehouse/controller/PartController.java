package com.warehouse.controller;

import com.warehouse.dto.ApiResponse;
import com.warehouse.dto.StockOperationDto;
import com.warehouse.entity.Part;
import com.warehouse.service.PartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parts")
public class PartController {

    @Autowired
    private PartService partService;

    @GetMapping
    public ApiResponse<List<Part>> getAllParts() {
        return ApiResponse.success(partService.getAllParts());
    }

    @GetMapping("/{id}")
    public ApiResponse<Part> getPartById(@PathVariable String id) {
        Part part = partService.getPartById(id);
        if (part == null) {
            return ApiResponse.error("零件不存在: " + id);
        }
        return ApiResponse.success(part);
    }

    @PostMapping
    public ApiResponse<Part> addPart(@RequestBody Part part) {
        return ApiResponse.success("零件添加成功", partService.addPart(part));
    }

    @PutMapping("/{id}")
    public ApiResponse<Part> updatePart(@PathVariable String id, @RequestBody Part part) {
        if (!id.equals(part.getId())) {
            return ApiResponse.error("零件ID不匹配");
        }
        return ApiResponse.success("零件更新成功", partService.updatePart(part));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePart(@PathVariable String id) {
        partService.deletePart(id);
        return ApiResponse.success("零件删除成功", null);
    }

    @PostMapping("/stock-in")
    public ApiResponse<Part> stockIn(@RequestBody StockOperationDto dto) {
        Part part = partService.stockIn(dto.getPartId(), dto.getQuantity());
        return ApiResponse.success("入库成功", part);
    }

    @PostMapping("/stock-out")
    public ApiResponse<Part> stockOut(@RequestBody StockOperationDto dto) {
        Part part = partService.stockOut(dto.getPartId(), dto.getQuantity());
        return ApiResponse.success("出库成功", part);
    }

    @GetMapping("/search")
    public ApiResponse<List<Part>> searchParts(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(partService.searchParts(keyword));
    }

    @GetMapping("/need-restock")
    public ApiResponse<List<Part>> getPartsNeedRestock() {
        return ApiResponse.success(partService.getPartsNeedRestock());
    }
}
