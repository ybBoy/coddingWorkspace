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

/**
 * 通用零件REST API控制器
 * 提供零件的基础CRUD操作和库存操作
 * 此控制器不区分用户权限，供前端基础路由使用
 * 
 * 权限说明：
 * - 普通用户应使用 UserPartController（只访问可见零件）
 * - 管理员应使用 AdminPartController（访问所有零件）
 */
@RestController
@RequestMapping("/api/parts")
public class PartController {

    @Autowired
    private PartService partService;

    /**
     * 获取所有零件列表
     * @return 所有零件列表
     */
    @GetMapping
    public ApiResponse<List<Part>> getAllParts() {
        return ApiResponse.success(partService.getAllParts());
    }

    /**
     * 根据编号获取单个零件
     * @param id 零件编号
     * @return 零件详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Part> getPartById(@PathVariable String id) {
        Part part = partService.getPartById(id);
        if (part == null) {
            return ApiResponse.error("零件不存在: " + id);
        }
        return ApiResponse.success(part);
    }

    /**
     * 添加新零件
     * @param part 零件对象
     * @return 添加后的零件
     */
    @PostMapping
    public ApiResponse<Part> addPart(@RequestBody Part part) {
        return ApiResponse.success("零件添加成功", partService.addPart(part));
    }

    /**
     * 更新零件信息
     * @param id 零件编号（路径参数）
     * @param part 零件对象（请求体）
     * @return 更新后的零件
     */
    @PutMapping("/{id}")
    public ApiResponse<Part> updatePart(@PathVariable String id, @RequestBody Part part) {
        if (!id.equals(part.getId())) {
            return ApiResponse.error("零件ID不匹配");
        }
        return ApiResponse.success("零件更新成功", partService.updatePart(part));
    }

    /**
     * 删除零件
     * @param id 零件编号
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePart(@PathVariable String id) {
        partService.deletePart(id);
        return ApiResponse.success("零件删除成功", null);
    }

    /**
     * 零件入库操作
     * 增加库存数量并记录操作人IP
     * @param dto 入库操作参数（包含零件编号和数量）
     * @param request HTTP请求对象（用于获取客户端IP）
     * @return 更新后的零件
     */
    @PostMapping("/stock-in")
    public ApiResponse<Part> stockIn(@RequestBody StockOperationDto dto, HttpServletRequest request) {
        String ipAddress = IpUtils.getClientIpAddress(request);
        Part part = partService.stockIn(dto.getPartId(), dto.getQuantity(), ipAddress);
        return ApiResponse.success("入库成功", part);
    }

    /**
     * 零件出库操作
     * 减少库存数量并记录操作人IP，检查库存是否充足
     * @param dto 出库操作参数（包含零件编号和数量）
     * @param request HTTP请求对象（用于获取客户端IP）
     * @return 更新后的零件
     */
    @PostMapping("/stock-out")
    public ApiResponse<Part> stockOut(@RequestBody StockOperationDto dto, HttpServletRequest request) {
        String ipAddress = IpUtils.getClientIpAddress(request);
        Part part = partService.stockOut(dto.getPartId(), dto.getQuantity(), ipAddress);
        return ApiResponse.success("出库成功", part);
    }

    /**
     * 搜索零件
     * 支持按关键词（编号或名称）和分类组合搜索
     * @param keyword 搜索关键词（可选）
     * @param category 分类筛选（可选）
     * @return 匹配的零件列表
     */
    @GetMapping("/search")
    public ApiResponse<List<Part>> searchParts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return ApiResponse.success(partService.searchPartsByKeywordAndCategory(keyword, category));
    }

    /**
     * 获取需要补货的零件列表
     * 返回库存数量 <= 最低库存预警值的零件
     * @return 需要补货的零件列表
     */
    @GetMapping("/need-restock")
    public ApiResponse<List<Part>> getPartsNeedRestock() {
        return ApiResponse.success(partService.getPartsNeedRestock());
    }
}
