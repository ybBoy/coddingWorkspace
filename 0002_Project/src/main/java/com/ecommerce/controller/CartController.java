package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.model.CartItem;
import com.ecommerce.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 购物车控制器
 * 提供购物车相关的REST API接口
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * 获取购物车列表
     * GET /api/cart
     */
    @GetMapping
    public Result<List<CartItem>> list() {
        List<CartItem> items = cartService.getAllCartItems();
        return Result.success(items);
    }

    /**
     * 添加商品到购物车
     * POST /api/cart
     */
    @PostMapping
    public Result<CartItem> add(@RequestBody Map<String, Object> params) {
        String productId = (String) params.get("productId");
        Object quantityObj = params.get("quantity");
        
        if (productId == null || productId.trim().isEmpty()) {
            return Result.error("商品ID不能为空");
        }
        
        Integer quantity;
        try {
            if (quantityObj instanceof Number) {
                quantity = ((Number) quantityObj).intValue();
            } else {
                quantity = Integer.parseInt(quantityObj.toString());
            }
        } catch (Exception e) {
            return Result.error("数量格式不正确");
        }
        
        if (quantity <= 0) {
            return Result.error("添加数量必须大于0");
        }
        
        try {
            CartItem item = cartService.addToCart(productId, quantity);
            return Result.success("商品已添加到购物车", item);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新购物车项数量
     * PUT /api/cart/{id}
     */
    @PutMapping("/{id}")
    public Result<CartItem> updateQuantity(
            @PathVariable String id,
            @RequestBody Map<String, Object> params) {
        Object quantityObj = params.get("quantity");
        
        Integer quantity;
        try {
            if (quantityObj instanceof Number) {
                quantity = ((Number) quantityObj).intValue();
            } else {
                quantity = Integer.parseInt(quantityObj.toString());
            }
        } catch (Exception e) {
            return Result.error("数量格式不正确");
        }
        
        if (quantity <= 0) {
            return Result.error("数量必须大于0");
        }
        
        try {
            CartItem item = cartService.updateQuantity(id, quantity);
            return Result.success("数量已更新", item);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除购物车项
     * DELETE /api/cart/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable String id) {
        try {
            cartService.removeFromCart(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 清空购物车
     * DELETE /api/cart
     */
    @DeleteMapping
    public Result<Void> clear() {
        cartService.clearCart();
        return Result.success();
    }

    /**
     * 计算购物车总价
     * GET /api/cart/total
     */
    @GetMapping("/total")
    public Result<BigDecimal> getTotal() {
        BigDecimal total = cartService.calculateTotal();
        return Result.success(total);
    }
}
