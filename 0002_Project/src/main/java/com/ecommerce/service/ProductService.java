package com.ecommerce.service;

import com.ecommerce.model.Product;
import com.ecommerce.repository.DataPersistenceManager;
import com.ecommerce.repository.DataStore;
import com.ecommerce.util.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品服务类
 * 负责商品相关的业务逻辑处理
 */
@Service
public class ProductService {

    private final DataStore dataStore;
    private final DataPersistenceManager dataPersistenceManager;

    @Autowired
    public ProductService(DataPersistenceManager dataPersistenceManager) {
        this.dataStore = DataStore.getInstance();
        this.dataPersistenceManager = dataPersistenceManager;
    }

    /**
     * 获取所有商品列表
     * @return 商品列表
     */
    public List<Product> getAllProducts() {
        return dataStore.getAllProducts();
    }

    /**
     * 根据ID获取商品
     * @param id 商品ID
     * @return 商品对象
     */
    public Product getProductById(String id) {
        return dataStore.getProductById(id);
    }

    /**
     * 添加商品
     * @param product 商品信息
     * @return 创建的商品对象
     */
    public Product addProduct(Product product) {
        String id = IdGenerator.generateProductId();
        product.setId(id);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        
        dataStore.addProduct(product);
        dataPersistenceManager.saveData();
        
        return product;
    }

    /**
     * 更新商品库存
     * @param id 商品ID
     * @param stock 新的库存数量
     * @return 更新后的商品对象
     */
    public Product updateStock(String id, Integer stock) {
        Product product = dataStore.getProductById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        
        if (stock < 0) {
            throw new RuntimeException("库存不能为负数");
        }
        
        product.setStock(stock);
        product.setUpdatedAt(LocalDateTime.now());
        
        dataStore.updateProduct(product);
        dataPersistenceManager.saveData();
        
        return product;
    }

    /**
     * 扣减库存
     * @param id 商品ID
     * @param quantity 扣减数量
     * @return 更新后的商品对象
     */
    public Product deductStock(String id, Integer quantity) {
        Product product = dataStore.getProductById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        
        if (product.getStock() < quantity) {
            throw new RuntimeException("库存不足");
        }
        
        product.setStock(product.getStock() - quantity);
        product.setUpdatedAt(LocalDateTime.now());
        
        dataStore.updateProduct(product);
        dataPersistenceManager.saveData();
        
        return product;
    }

    /**
     * 增加库存
     * @param id 商品ID
     * @param quantity 增加数量
     * @return 更新后的商品对象
     */
    public Product addStock(String id, Integer quantity) {
        Product product = dataStore.getProductById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        
        product.setStock(product.getStock() + quantity);
        product.setUpdatedAt(LocalDateTime.now());
        
        dataStore.updateProduct(product);
        dataPersistenceManager.saveData();
        
        return product;
    }

    /**
     * 更新商品信息
     * @param id 商品ID
     * @param product 更新的商品信息
     * @return 更新后的商品对象
     */
    public Product updateProduct(String id, Product product) {
        Product existingProduct = dataStore.getProductById(id);
        if (existingProduct == null) {
            throw new RuntimeException("商品不存在");
        }
        
        if (product.getName() != null) {
            existingProduct.setName(product.getName());
        }
        if (product.getDescription() != null) {
            existingProduct.setDescription(product.getDescription());
        }
        if (product.getPrice() != null) {
            existingProduct.setPrice(product.getPrice());
        }
        if (product.getStock() != null) {
            existingProduct.setStock(product.getStock());
        }
        
        existingProduct.setUpdatedAt(LocalDateTime.now());
        dataStore.updateProduct(existingProduct);
        dataPersistenceManager.saveData();
        
        return existingProduct;
    }

    /**
     * 删除商品
     * @param id 商品ID
     */
    public void deleteProduct(String id) {
        Product product = dataStore.getProductById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        
        dataStore.removeProduct(id);
        dataPersistenceManager.saveData();
    }
}
