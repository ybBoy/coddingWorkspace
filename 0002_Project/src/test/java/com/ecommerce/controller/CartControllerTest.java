package com.ecommerce.controller;

import com.ecommerce.EcommerceApplication;
import com.ecommerce.common.Result;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Product;
import com.ecommerce.repository.DataStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 购物车控制器单元测试
 * 测试CartController中的所有API接口
 */
@SpringBootTest(classes = EcommerceApplication.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String testProductId;
    private static String testCartItemId;

    /**
     * 测试前清理数据
     */
    @BeforeEach
    public void setUp() {
        DataStore.getInstance().clearAll();
    }

    /**
     * 准备测试商品
     */
    private String createTestProduct() throws Exception {
        Product product = new Product();
        product.setName("Cart Test Product");
        product.setDescription("For cart testing");
        product.setPrice(new BigDecimal("99.99"));
        product.setStock(100);

        MvcResult result = mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk())
                .andReturn();

        Result<Product> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<Result<Product>>() {});

        return response.getData().getId();
    }

    /**
     * 测试1: 添加商品到购物车接口 - POST /api/cart
     */
    @Test
    @org.junit.jupiter.api.Order(1)
    public void testAddToCart() throws Exception {
        String productId = createTestProduct();

        Map<String, Object> params = new HashMap<>();
        params.put("productId", productId);
        params.put("quantity", 2);

        MvcResult result = mockMvc.perform(post("/api/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Result<CartItem> response = objectMapper.readValue(content,
                new TypeReference<Result<CartItem>>() {});

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertNotNull(response.getData().getId());
        assertEquals(productId, response.getData().getProductId());
        assertEquals(2, response.getData().getQuantity());
        assertEquals(new BigDecimal("199.98"), response.getData().getSubtotal());

        testCartItemId = response.getData().getId();
    }

    /**
     * 测试2: 获取购物车列表接口 - GET /api/cart
     */
    @Test
    @org.junit.jupiter.api.Order(2)
    public void testGetCartList() throws Exception {
        String productId = createTestProduct();

        // 添加商品到购物车
        Map<String, Object> params = new HashMap<>();
        params.put("productId", productId);
        params.put("quantity", 1);

        mockMvc.perform(post("/api/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk());

        // 获取购物车列表
        MvcResult result = mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Result<List<CartItem>> response = objectMapper.readValue(content,
                new TypeReference<Result<List<CartItem>>>() {});

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertTrue(response.getData().size() > 0);
    }

    /**
     * 测试3: 更新购物车项数量接口 - PUT /api/cart/{id}
     */
    @Test
    @org.junit.jupiter.api.Order(3)
    public void testUpdateQuantity() throws Exception {
        String productId = createTestProduct();

        // 添加商品到购物车
        Map<String, Object> addParams = new HashMap<>();
        addParams.put("productId", productId);
        addParams.put("quantity", 1);

        MvcResult addResult = mockMvc.perform(post("/api/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addParams)))
                .andExpect(status().isOk())
                .andReturn();

        Result<CartItem> addResponse = objectMapper.readValue(
                addResult.getResponse().getContentAsString(),
                new TypeReference<Result<CartItem>>() {});

        String cartItemId = addResponse.getData().getId();

        // 更新数量
        Map<String, Object> updateParams = new HashMap<>();
        updateParams.put("quantity", 5);

        MvcResult result = mockMvc.perform(put("/api/cart/" + cartItemId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateParams)))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Result<CartItem> response = objectMapper.readValue(content,
                new TypeReference<Result<CartItem>>() {});

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(5, response.getData().getQuantity());
        assertEquals(new BigDecimal("499.95"), response.getData().getSubtotal());
    }

    /**
     * 测试4: 删除购物车项接口 - DELETE /api/cart/{id}
     */
    @Test
    @org.junit.jupiter.api.Order(4)
    public void testRemoveFromCart() throws Exception {
        String productId = createTestProduct();

        // 添加商品到购物车
        Map<String, Object> addParams = new HashMap<>();
        addParams.put("productId", productId);
        addParams.put("quantity", 1);

        MvcResult addResult = mockMvc.perform(post("/api/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addParams)))
                .andExpect(status().isOk())
                .andReturn();

        Result<CartItem> addResponse = objectMapper.readValue(
                addResult.getResponse().getContentAsString(),
                new TypeReference<Result<CartItem>>() {});

        String cartItemId = addResponse.getData().getId();

        // 删除购物车项
        MvcResult deleteResult = mockMvc.perform(delete("/api/cart/" + cartItemId))
                .andExpect(status().isOk())
                .andReturn();

        String deleteContent = deleteResult.getResponse().getContentAsString();
        Result<Void> deleteResponse = objectMapper.readValue(deleteContent,
                new TypeReference<Result<Void>>() {});

        assertEquals(200, deleteResponse.getCode());

        // 验证已删除
        MvcResult listResult = mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andReturn();

        Result<List<CartItem>> listResponse = objectMapper.readValue(
                listResult.getResponse().getContentAsString(),
                new TypeReference<Result<List<CartItem>>>() {});

        assertEquals(0, listResponse.getData().size());
    }

    /**
     * 测试5: 清空购物车接口 - DELETE /api/cart
     */
    @Test
    @org.junit.jupiter.api.Order(5)
    public void testClearCart() throws Exception {
        String productId = createTestProduct();

        // 添加多个商品到购物车
        Map<String, Object> addParams = new HashMap<>();
        addParams.put("productId", productId);
        addParams.put("quantity", 1);

        mockMvc.perform(post("/api/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addParams)))
                .andExpect(status().isOk());

        // 清空购物车
        MvcResult clearResult = mockMvc.perform(delete("/api/cart"))
                .andExpect(status().isOk())
                .andReturn();

        String clearContent = clearResult.getResponse().getContentAsString();
        Result<Void> clearResponse = objectMapper.readValue(clearContent,
                new TypeReference<Result<Void>>() {});

        assertEquals(200, clearResponse.getCode());

        // 验证已清空
        MvcResult listResult = mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andReturn();

        Result<List<CartItem>> listResponse = objectMapper.readValue(
                listResult.getResponse().getContentAsString(),
                new TypeReference<Result<List<CartItem>>>() {});

        assertEquals(0, listResponse.getData().size());
    }

    /**
     * 测试6: 计算购物车总价接口 - GET /api/cart/total
     */
    @Test
    @org.junit.jupiter.api.Order(6)
    public void testCalculateTotal() throws Exception {
        String productId = createTestProduct();

        // 添加商品到购物车
        Map<String, Object> addParams = new HashMap<>();
        addParams.put("productId", productId);
        addParams.put("quantity", 3);

        mockMvc.perform(post("/api/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addParams)))
                .andExpect(status().isOk());

        // 计算总价
        MvcResult result = mockMvc.perform(get("/api/cart/total"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Result<BigDecimal> response = objectMapper.readValue(content,
                new TypeReference<Result<BigDecimal>>() {});

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(new BigDecimal("299.97"), response.getData());
    }

    /**
     * 测试7: 同一个商品多次添加（数量累加）
     */
    @Test
    @org.junit.jupiter.api.Order(7)
    public void testAddSameProductMultipleTimes() throws Exception {
        String productId = createTestProduct();

        // 第一次添加
        Map<String, Object> params1 = new HashMap<>();
        params1.put("productId", productId);
        params1.put("quantity", 2);

        mockMvc.perform(post("/api/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params1)))
                .andExpect(status().isOk());

        // 第二次添加
        Map<String, Object> params2 = new HashMap<>();
        params2.put("productId", productId);
        params2.put("quantity", 3);

        MvcResult result = mockMvc.perform(post("/api/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params2)))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Result<CartItem> response = objectMapper.readValue(content,
                new TypeReference<Result<CartItem>>() {});

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(5, response.getData().getQuantity());
        assertEquals(new BigDecimal("499.95"), response.getData().getSubtotal());
    }

    /**
     * 测试8: 添加商品参数校验
     */
    @Test
    @org.junit.jupiter.api.Order(8)
    public void testAddToCartValidation() throws Exception {
        // 测试空商品ID
        Map<String, Object> params1 = new HashMap<>();
        params1.put("quantity", 1);

        MvcResult result1 = mockMvc.perform(post("/api/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params1)))
                .andExpect(status().isOk())
                .andReturn();

        Result<CartItem> response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(),
                new TypeReference<Result<CartItem>>() {});

        assertEquals(500, response1.getCode());

        // 测试商品不存在
        Map<String, Object> params2 = new HashMap<>();
        params2.put("productId", "non_existent_id");
        params2.put("quantity", 1);

        MvcResult result2 = mockMvc.perform(post("/api/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params2)))
                .andExpect(status().isOk())
                .andReturn();

        Result<CartItem> response2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(),
                new TypeReference<Result<CartItem>>() {});

        assertEquals(500, response2.getCode());

        // 测试数量为0
        String productId = createTestProduct();
        Map<String, Object> params3 = new HashMap<>();
        params3.put("productId", productId);
        params3.put("quantity", 0);

        MvcResult result3 = mockMvc.perform(post("/api/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params3)))
                .andExpect(status().isOk())
                .andReturn();

        Result<CartItem> response3 = objectMapper.readValue(
                result3.getResponse().getContentAsString(),
                new TypeReference<Result<CartItem>>() {});

        assertEquals(500, response3.getCode());
    }
}
