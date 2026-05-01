package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.model.Order;
import com.ecommerce.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单控制器
 * 提供订单管理相关的REST API接口
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 获取订单列表
     * GET /api/orders
     */
    @GetMapping
    public Result<List<Order>> list() {
        List<Order> orders = orderService.getAllOrders();
        return Result.success(orders);
    }

    /**
     * 根据ID获取订单详情
     * GET /api/orders/{id}
     */
    @GetMapping("/{id}")
    public Result<Order> getById(@PathVariable String id) {
        Order order = orderService.getOrderById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return Result.success(order);
    }

    /**
     * 从购物车创建订单
     * POST /api/orders/create
     */
    @PostMapping("/create")
    public Result<Order> createFromCart(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<String> cartItemIds = (List<String>) params.get("cartItemIds");
        
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return Result.error("请选择要下单的商品");
        }
        
        try {
            Order order = orderService.createOrderFromCart(cartItemIds);
            return Result.success("订单创建成功", order);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 支付订单
     * POST /api/orders/{id}/pay
     */
    @PostMapping("/{id}/pay")
    public Result<Order> pay(@PathVariable String id) {
        try {
            Order order = orderService.payOrder(id);
            return Result.success("支付成功", order);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消订单
     * POST /api/orders/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public Result<Order> cancel(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> params) {
        String reason = "用户取消";
        if (params != null && params.get("reason") != null) {
            reason = (String) params.get("reason");
        }
        
        try {
            Order order = orderService.cancelOrder(id, reason);
            return Result.success("订单已取消", order);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 确认收货，订单完成
     * POST /api/orders/{id}/complete
     */
    @PostMapping("/{id}/complete")
    public Result<Order> complete(@PathVariable String id) {
        try {
            Order order = orderService.completeOrder(id);
            return Result.success("订单已完成", order);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
