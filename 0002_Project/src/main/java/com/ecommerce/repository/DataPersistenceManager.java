package com.ecommerce.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;

/**
 * 数据持久化管理器
 * 负责将数据保存到文件和从文件加载数据
 * 
 * 设计模式：门面模式（Facade）
 * 为复杂的数据持久化操作提供一个统一的接口
 */
@Component
public class DataPersistenceManager {

    private static final Logger logger = LoggerFactory.getLogger(DataPersistenceManager.class);

    @Value("${data.file.path:data.json}")
    private String dataFilePath;

    private final ObjectMapper objectMapper;
    private final DataStore dataStore;

    public DataPersistenceManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.dataStore = DataStore.getInstance();
    }

    /**
     * 应用启动时加载数据
     */
    @PostConstruct
    public void init() {
        loadData();
    }

    /**
     * 应用关闭时保存数据
     */
    @PreDestroy
    public void destroy() {
        saveData();
    }

    /**
     * 保存数据到文件
     */
    public void saveData() {
        try {
            DataWrapper wrapper = new DataWrapper();
            wrapper.setProducts(dataStore.getProducts());
            wrapper.setCartItems(dataStore.getCartItems());
            wrapper.setOrders(dataStore.getOrders());

            File file = new File(dataFilePath);
            objectMapper.writeValue(file, wrapper);
            logger.info("数据已成功保存到: {}", file.getAbsolutePath());
        } catch (IOException e) {
            logger.error("保存数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 从文件加载数据
     */
    public void loadData() {
        try {
            File file = new File(dataFilePath);
            if (!file.exists()) {
                logger.info("数据文件不存在，使用空数据初始化");
                return;
            }

            DataWrapper wrapper = objectMapper.readValue(file, DataWrapper.class);
            
            if (wrapper.getProducts() != null) {
                dataStore.setProducts(wrapper.getProducts());
            }
            if (wrapper.getCartItems() != null) {
                dataStore.setCartItems(wrapper.getCartItems());
            }
            if (wrapper.getOrders() != null) {
                dataStore.setOrders(wrapper.getOrders());
            }

            logger.info("数据已成功从文件加载: {}", file.getAbsolutePath());
        } catch (IOException e) {
            logger.error("加载数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 数据包装类
     * 用于JSON序列化和反序列化
     */
    private static class DataWrapper {
        private java.util.Map<String, com.ecommerce.model.Product> products;
        private java.util.Map<String, com.ecommerce.model.CartItem> cartItems;
        private java.util.Map<String, com.ecommerce.model.Order> orders;

        public java.util.Map<String, com.ecommerce.model.Product> getProducts() {
            return products;
        }

        public void setProducts(java.util.Map<String, com.ecommerce.model.Product> products) {
            this.products = products;
        }

        public java.util.Map<String, com.ecommerce.model.CartItem> getCartItems() {
            return cartItems;
        }

        public void setCartItems(java.util.Map<String, com.ecommerce.model.CartItem> cartItems) {
            this.cartItems = cartItems;
        }

        public java.util.Map<String, com.ecommerce.model.Order> getOrders() {
            return orders;
        }

        public void setOrders(java.util.Map<String, com.ecommerce.model.Order> orders) {
            this.orders = orders;
        }
    }
}
