package com.warehouse.controller;

import com.warehouse.dto.ApiResponse;
import com.warehouse.entity.Part;
import com.warehouse.service.PartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/parts")
public class AdminPartController {

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

    @PutMapping("/{id}/visibility")
    public ApiResponse<Part> updateVisibility(@PathVariable String id, @RequestBody PartVisibilityDto dto) {
        try {
            Part part = partService.updateVisibility(id, dto.isVisible());
            return ApiResponse.success("可见性更新成功", part);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ApiResponse<List<Part>> searchParts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return ApiResponse.success(partService.searchPartsByKeywordAndCategory(keyword, category));
    }

    @GetMapping("/need-restock")
    public ApiResponse<List<Part>> getPartsNeedRestock() {
        return ApiResponse.success(partService.getPartsNeedRestock());
    }

    public static class PartVisibilityDto {
        private boolean visible;

        public boolean isVisible() {
            return visible;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }
    }
}
