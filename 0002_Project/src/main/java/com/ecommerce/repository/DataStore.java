package com.ecommerce.repository;

import com.ecommerce.model.CartItem;
import com.ecommerce.model.Order;
import com.ecommerce.model.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据存储类
 * 使用单例模式管理内存中的所有数据
 * 
 * 设计模式：单例模式（Singleton）
 * 确保在整个应用中只有一个数据存储实例，保证数据的一致性
 */
public class DataStore {

    // 单例实例（饿汉式）
    private static final DataStore INSTANCE = new DataStore();

    // 商品存储：key=商品ID, value=商品对象
    private Map<String, Product> products;

    // 购物车存储：key=购物车项ID, value=购物车项对象
    private Map<String, CartItem> cartItems;

    // 订单存储：key=订单ID, value=订单对象
    private Map<String, Order> orders;

    // 订单编号到订单ID的映射：key=订单编号, value=订单ID
    private Map<String, String> orderNoToIdMap;

    /**
     * 私有构造函数，防止外部实例化
     */
    private DataStore() {
        this.products = new HashMap<>();
        this.cartItems = new HashMap<>();
        this.orders = new HashMap<>();
        this.orderNoToIdMap = new HashMap<>();
    }

    /**
     * 获取单例实例
     * @return DataStore实例
     */
    public static DataStore getInstance() {
        return INSTANCE;
    }

    // ==================== 商品相关操作 ====================

    /**
     * 获取所有商品
     * @return 商品列表
     */
    public List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    /**
     * 根据ID获取商品
     * @param id 商品ID
     * @return 商品对象
     */
    public Product getProductById(String id) {
        return products.get(id);
    }

    /**
     * 添加商品
     * @param product 商品对象
     */
    public void addProduct(Product product) {
        products.put(product.getId(), product);
    }

    /**
     * 更新商品
     * @param product 商品对象
     */
    public void updateProduct(Product product) {
        products.put(product.getId(), product);
    }

    /**
     * 删除商品
     * @param id 商品ID
     */
    public void removeProduct(String id) {
        products.remove(id);
    }

    /**
     * 获取商品Map
     * @return 商品Map
     */
    public Map<String, Product> getProducts() {
        return products;
    }

    /**
     * 设置商品Map（用于数据恢复）
     * @param products 商品Map
     */
    public void setProducts(Map<String, Product> products) {
        this.products = products;
    }

    // ==================== 购物车相关操作 ====================

    /**
     * 获取所有购物车项
     * @return 购物车项列表
     */
    public List<CartItem> getAllCartItems() {
        return new ArrayList<>(cartItems.values());
    }

    /**
     * 根据ID获取购物车项
     * @param id 购物车项ID
     * @return 购物车项对象
     */
    public CartItem getCartItemById(String id) {
        return cartItems.get(id);
    }

    /**
     * 根据商品ID获取购物车项
     * @param productId 商品ID
     * @return 购物车项对象
     */
    public CartItem getCartItemByProductId(String productId) {
        for (CartItem item : cartItems.values()) {
            if (item.getProductId().equals(productId)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 添加购物车项
     * @param cartItem 购物车项对象
     */
    public void addCartItem(CartItem cartItem) {
        cartItems.put(cartItem.getId(), cartItem);
    }

    /**
     * 更新购物车项
     * @param cartItem 购物车项对象
     */
    public void updateCartItem(CartItem cartItem) {
        cartItems.put(cartItem.getId(), cartItem);
    }

    /**
     * 删除购物车项
     * @param id 购物车项ID
     */
    public void removeCartItem(String id) {
        cartItems.remove(id);
    }

    /**
     * 清空购物车
     */
    public void clearCart() {
        cartItems.clear();
    }

    /**
     * 获取购物车项Map
     * @return 购物车项Map
     */
    public Map<String, CartItem> getCartItems() {
        return cartItems;
    }

    /**
     * 设置购物车项Map（用于数据恢复）
     * @param cartItems 购物车项Map
     */
    public void setCartItems(Map<String, CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    // ==================== 订单相关操作 ====================

    /**
     * 获取所有订单
     * @return 订单列表
     */
    public List<Order> getAllOrders() {
        return new ArrayList<>(orders.values());
    }

    /**
     * 根据ID获取订单
     * @param id 订单ID
     * @return 订单对象
     */
    public Order getOrderById(String id) {
        return orders.get(id);
    }

    /**
     * 根据订单编号获取订单
     * @param orderNo 订单编号
     * @return 订单对象
     */
    public Order getOrderByOrderNo(String orderNo) {
        String id = orderNoToIdMap.get(orderNo);
        return id != null ? orders.get(id) : null;
    }

    /**
     * 添加订单
     * @param order 订单对象
     */
    public void addOrder(Order order) {
        orders.put(order.getId(), order);
        orderNoToIdMap.put(order.getOrderNo(), order.getId());
    }

    /**
     * 更新订单
     * @param order 订单对象
     */
    public void updateOrder(Order order) {
        orders.put(order.getId(), order);
    }

    /**
     * 删除订单
     * @param id 订单ID
     */
    public void removeOrder(String id) {
        Order order = orders.get(id);
        if (order != null) {
            orderNoToIdMap.remove(order.getOrderNo());
            orders.remove(id);
        }
    }

    /**
     * 获取订单Map
     * @return 订单Map
     */
    public Map<String, Order> getOrders() {
        return orders;
    }

    /**
     * 设置订单Map（用于数据恢复）
     * @param orders 订单Map
     */
    public void setOrders(Map<String, Order> orders) {
        this.orders = orders;
        // 重建订单编号到ID的映射
        this.orderNoToIdMap.clear();
        for (Order order : orders.values()) {
            this.orderNoToIdMap.put(order.getOrderNo(), order.getId());
        }
    }

    /**
     * 清空所有数据
     */
    public void clearAll() {
        products.clear();
        cartItems.clear();
        orders.clear();
        orderNoToIdMap.clear();
    }
}
