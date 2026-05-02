package com.warehouse.controller;

import com.warehouse.dto.ApiResponse;
import com.warehouse.dto.StockOperationDto;
import com.warehouse.entity.Part;
import com.warehouse.service.PartService;
import com.warehouse.util.IpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/user/parts")
public class UserPartController {

    @Autowired
    private PartService partService;

    @GetMapping
    public ApiResponse<List<Part>> getVisibleParts() {
        return ApiResponse.success(partService.getVisibleParts());
    }

    @GetMapping("/{id}")
    public ApiResponse<Part> getVisiblePartById(@PathVariable String id) {
        Part part = partService.getVisiblePartById(id);
        if (part == null) {
            return ApiResponse.error("零件不存在或不可见: " + id);
        }
        return ApiResponse.success(part);
    }

    @GetMapping("/search")
    public ApiResponse<List<Part>> searchVisibleParts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return ApiResponse.success(partService.searchVisiblePartsByKeywordAndCategory(keyword, category));
    }

    @GetMapping("/need-restock")
    public ApiResponse<List<Part>> getVisiblePartsNeedRestock() {
        return ApiResponse.success(partService.getVisiblePartsNeedRestock());
    }

    @PostMapping("/stock-in")
    public ApiResponse<Part> stockIn(@RequestBody StockOperationDto dto, HttpServletRequest request) {
        String ipAddress = IpUtils.getClientIpAddress(request);
        Part part = partService.stockIn(dto.getPartId(), dto.getQuantity(), ipAddress);
        return ApiResponse.success("入库成功", part);
    }

    @PostMapping("/stock-out")
    public ApiResponse<Part> stockOut(@RequestBody StockOperationDto dto, HttpServletRequest request) {
        String ipAddress = IpUtils.getClientIpAddress(request);
        Part part = partService.stockOut(dto.getPartId(), dto.getQuantity(), ipAddress);
        return ApiResponse.success("出库成功", part);
    }
}
