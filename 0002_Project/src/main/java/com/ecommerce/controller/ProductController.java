package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.model.Product;
import com.ecommerce.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器
 * 提供商品管理相关的REST API接口
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 获取商品列表
     * GET /api/products
     */
    @GetMapping
    public Result<List<Product>> list() {
        List<Product> products = productService.getAllProducts();
        return Result.success(products);
    }

    /**
     * 根据ID获取商品详情
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable String id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            return Result.error("商品不存在");
        }
        return Result.success(product);
    }

    /**
     * 添加商品
     * POST /api/products
     */
    @PostMapping
    public Result<Product> add(@RequestBody Product product) {
        // 参数校验
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            return Result.error("商品名称不能为空");
        }
        if (product.getPrice() == null) {
            return Result.error("商品价格不能为空");
        }
        if (product.getStock() == null) {
            return Result.error("商品库存不能为空");
        }
        
        Product created = productService.addProduct(product);
        return Result.success("商品添加成功", created);
    }

    /**
     * 更新商品库存
     * PUT /api/products/{id}/stock
     */
    @PutMapping("/{id}/stock")
    public Result<Product> updateStock(
            @PathVariable String id,
            @RequestParam Integer stock) {
        try {
            Product updated = productService.updateStock(id, stock);
            return Result.success("库存更新成功", updated);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新商品信息
     * PUT /api/products/{id}
     */
    @PutMapping("/{id}")
    public Result<Product> update(
            @PathVariable String id,
            @RequestBody Product product) {
        try {
            Product updated = productService.updateProduct(id, product);
            return Result.success("商品更新成功", updated);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除商品
     * DELETE /api/products/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        try {
            productService.deleteProduct(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
