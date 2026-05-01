package com.ecommerce.service;

import com.ecommerce.model.CartItem;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.repository.DataPersistenceManager;
import com.ecommerce.repository.DataStore;
import com.ecommerce.util.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单服务类
 * 负责订单相关的业务逻辑处理
 * 
 * 设计模式：状态模式（State Pattern）的思想
 * 订单状态的流转通过OrderStatus枚举的canTransitionTo方法控制
 */
@Service
public class OrderService {

    private final DataStore dataStore;
    private final DataPersistenceManager dataPersistenceManager;
    private final CartService cartService;
    private final ProductService productService;

    @Autowired
    public OrderService(DataPersistenceManager dataPersistenceManager, 
                        CartService cartService, 
                        ProductService productService) {
        this.dataStore = DataStore.getInstance();
        this.dataPersistenceManager = dataPersistenceManager;
        this.cartService = cartService;
        this.productService = productService;
    }

    /**
     * 获取所有订单
     * @return 订单列表
     */
    public List<Order> getAllOrders() {
        List<Order> orders = dataStore.getAllOrders();
        // 按创建时间倒序排列
        orders.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
        return orders;
    }

    /**
     * 根据ID获取订单
     * @param id 订单ID
     * @return 订单对象
     */
    public Order getOrderById(String id) {
        return dataStore.getOrderById(id);
    }

    /**
     * 根据订单编号获取订单
     * @param orderNo 订单编号
     * @return 订单对象
     */
    public Order getOrderByOrderNo(String orderNo) {
        return dataStore.getOrderByOrderNo(orderNo);
    }

    /**
     * 从购物车创建订单
     * @param cartItemIds 购物车项ID列表
     * @return 创建的订单
     */
    public Order createOrderFromCart(List<String> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw new RuntimeException("请选择要下单的商品");
        }

        // 获取购物车项并验证
        List<CartItem> selectedItems = new ArrayList<>();
        for (String cartItemId : cartItemIds) {
            CartItem item = cartService.getCartItemById(cartItemId);
            if (item == null) {
                throw new RuntimeException("购物车项不存在: " + cartItemId);
            }
            selectedItems.add(item);
        }

        // 创建订单
        String orderId = IdGenerator.generateUUID();
        String orderNo = IdGenerator.generateOrderNo();
        Order order = new Order(orderId, orderNo);

        // 创建订单项
        for (CartItem cartItem : selectedItems) {
            String orderItemId = IdGenerator.generateOrderItemId();
            OrderItem orderItem = new OrderItem(
                    orderItemId,
                    orderId,
                    cartItem.getProductId(),
                    cartItem.getProductName(),
                    cartItem.getProductPrice(),
                    cartItem.getQuantity()
            );
            order.addItem(orderItem);

            // 扣减库存
            productService.deductStock(cartItem.getProductId(), cartItem.getQuantity());
        }

        order.calculateTotalAmount();
        dataStore.addOrder(order);

        // 从购物车移除已下单的商品
        cartService.removeCartItems(cartItemIds);

        dataPersistenceManager.saveData();

        return order;
    }

    /**
     * 支付订单
     * @param orderId 订单ID
     * @return 更新后的订单
     */
    public Order payOrder(String orderId) {
        Order order = dataStore.getOrderById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 检查状态流转
        if (!order.getStatus().canTransitionTo(OrderStatus.PAID)) {
            throw new RuntimeException("当前订单状态无法支付");
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        dataStore.updateOrder(order);
        dataPersistenceManager.saveData();

        return order;
    }

    /**
     * 取消订单
     * @param orderId 订单ID
     * @param reason 取消原因
     * @return 更新后的订单
     */
    public Order cancelOrder(String orderId, String reason) {
        Order order = dataStore.getOrderById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 只有待支付状态才能取消
        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw new RuntimeException("当前订单状态无法取消，只有待支付订单可以取消");
        }

        // 恢复库存
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                productService.addStock(item.getProductId(), item.getQuantity());
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(reason);
        order.setUpdatedAt(LocalDateTime.now());

        dataStore.updateOrder(order);
        dataPersistenceManager.saveData();

        return order;
    }

    /**
     * 确认收货，订单完成
     * @param orderId 订单ID
     * @return 更新后的订单
     */
    public Order completeOrder(String orderId) {
        Order order = dataStore.getOrderById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 检查状态流转
        if (!order.getStatus().canTransitionTo(OrderStatus.COMPLETED)) {
            throw new RuntimeException("当前订单状态无法完成");
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        dataStore.updateOrder(order);
        dataPersistenceManager.saveData();

        return order;
    }

    /**
     * 根据状态查询订单
     * @param status 订单状态
     * @return 订单列表
     */
    public List<Order> getOrdersByStatus(OrderStatus status) {
        List<Order> allOrders = dataStore.getAllOrders();
        List<Order> filteredOrders = new ArrayList<>();
        
        for (Order order : allOrders) {
            if (order.getStatus() == status) {
                filteredOrders.add(order);
            }
        }
        
        // 按创建时间倒序排列
        filteredOrders.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
        return filteredOrders;
    }
}
