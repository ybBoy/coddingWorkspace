package com.ecommerce.service;

import com.ecommerce.model.CartItem;
import com.ecommerce.model.Product;
import com.ecommerce.repository.DataPersistenceManager;
import com.ecommerce.repository.DataStore;
import com.ecommerce.util.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车服务类
 * 负责购物车相关的业务逻辑处理
 */
@Service
public class CartService {

    private final DataStore dataStore;
    private final DataPersistenceManager dataPersistenceManager;
    private final ProductService productService;

    @Autowired
    public CartService(DataPersistenceManager dataPersistenceManager, ProductService productService) {
        this.dataStore = DataStore.getInstance();
        this.dataPersistenceManager = dataPersistenceManager;
        this.productService = productService;
    }

    /**
     * 获取购物车所有商品
     * @return 购物车项列表
     */
    public List<CartItem> getAllCartItems() {
        return dataStore.getAllCartItems();
    }

    /**
     * 根据ID获取购物车项
     * @param id 购物车项ID
     * @return 购物车项对象
     */
    public CartItem getCartItemById(String id) {
        return dataStore.getCartItemById(id);
    }

    /**
     * 添加商品到购物车
     * @param productId 商品ID
     * @param quantity 数量
     * @return 购物车项对象
     */
    public CartItem addToCart(String productId, Integer quantity) {
        if (quantity <= 0) {
            throw new RuntimeException("添加数量必须大于0");
        }

        Product product = productService.getProductById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        if (product.getStock() < quantity) {
            throw new RuntimeException("库存不足");
        }

        // 检查商品是否已在购物车中
        CartItem existingItem = dataStore.getCartItemByProductId(productId);
        if (existingItem != null) {
            // 已存在，更新数量
            int newQuantity = existingItem.getQuantity() + quantity;
            
            // 检查库存
            if (product.getStock() < newQuantity) {
                throw new RuntimeException("库存不足");
            }
            
            existingItem.setQuantity(newQuantity);
            existingItem.calculateSubtotal();
            dataStore.updateCartItem(existingItem);
            dataPersistenceManager.saveData();
            return existingItem;
        }

        // 不存在，创建新的购物车项
        String id = IdGenerator.generateCartItemId();
        CartItem cartItem = new CartItem(id, productId, product.getName(), product.getPrice(), quantity);
        
        dataStore.addCartItem(cartItem);
        dataPersistenceManager.saveData();
        
        return cartItem;
    }

    /**
     * 更新购物车项数量
     * @param id 购物车项ID
     * @param quantity 新数量
     * @return 更新后的购物车项
     */
    public CartItem updateQuantity(String id, Integer quantity) {
        if (quantity <= 0) {
            throw new RuntimeException("数量必须大于0");
        }

        CartItem cartItem = dataStore.getCartItemById(id);
        if (cartItem == null) {
            throw new RuntimeException("购物车项不存在");
        }

        // 检查库存
        Product product = productService.getProductById(cartItem.getProductId());
        if (product == null) {
            throw new RuntimeException("关联商品不存在");
        }

        if (product.getStock() < quantity) {
            throw new RuntimeException("库存不足");
        }

        cartItem.setQuantity(quantity);
        cartItem.calculateSubtotal();
        dataStore.updateCartItem(cartItem);
        dataPersistenceManager.saveData();

        return cartItem;
    }

    /**
     * 删除购物车项
     * @param id 购物车项ID
     */
    public void removeFromCart(String id) {
        CartItem cartItem = dataStore.getCartItemById(id);
        if (cartItem == null) {
            throw new RuntimeException("购物车项不存在");
        }

        dataStore.removeCartItem(id);
        dataPersistenceManager.saveData();
    }

    /**
     * 清空购物车
     */
    public void clearCart() {
        dataStore.clearCart();
        dataPersistenceManager.saveData();
    }

    /**
     * 计算购物车总价
     * @return 总价
     */
    public BigDecimal calculateTotal() {
        List<CartItem> items = dataStore.getAllCartItems();
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 批量删除购物车项
     * @param ids 购物车项ID列表
     */
    public void removeCartItems(List<String> ids) {
        for (String id : ids) {
            dataStore.removeCartItem(id);
        }
        dataPersistenceManager.saveData();
    }
}
