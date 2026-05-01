package com.ecommerce.controller;

import com.ecommerce.EcommerceApplication;
import com.ecommerce.common.Result;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 商品控制器单元测试
 * 测试ProductController中的所有API接口
 */
@SpringBootTest(classes = EcommerceApplication.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String testProductId;

    /**
     * 测试前清理数据
     */
    @BeforeEach
    public void setUp() {
        DataStore.getInstance().clearAll();
    }

    /**
     * 测试1: 添加商品接口 - POST /api/products
     */
    @Test
    @org.junit.jupiter.api.Order(1)
    public void testAddProduct() throws Exception {
        Product product = new Product();
        product.setName("Test Product");
        product.setDescription("This is a test product");
        product.setPrice(new BigDecimal("99.99"));
        product.setStock(100);

        MvcResult result = mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Result<Product> response = objectMapper.readValue(content, 
                new TypeReference<Result<Product>>() {});

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertNotNull(response.getData().getId());
        assertEquals("Test Product", response.getData().getName());
        assertEquals(new BigDecimal("99.99"), response.getData().getPrice());
        assertEquals(100, response.getData().getStock());

        testProductId = response.getData().getId();
    }

    /**
     * 测试2: 获取商品列表接口 - GET /api/products
     */
    @Test
    @org.junit.jupiter.api.Order(2)
    public void testGetProductList() throws Exception {
        Product product = new Product();
        product.setName("List Test Product");
        product.setDescription("Test list");
        product.setPrice(new BigDecimal("50.00"));
        product.setStock(50);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Result<List<Product>> response = objectMapper.readValue(content,
                new TypeReference<Result<List<Product>>>() {});

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertTrue(response.getData().size() > 0);
    }

    /**
     * 测试3: 根据ID获取商品接口 - GET /api/products/{id}
     */
    @Test
    @org.junit.jupiter.api.Order(3)
    public void testGetProductById() throws Exception {
        Product product = new Product();
        product.setName("GetById Test Product");
        product.setDescription("Test get by id");
        product.setPrice(new BigDecimal("199.99"));
        product.setStock(20);

        MvcResult addResult = mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk())
                .andReturn();

        Result<Product> addResponse = objectMapper.readValue(
                addResult.getResponse().getContentAsString(),
                new TypeReference<Result<Product>>() {});

        String id = addResponse.getData().getId();

        MvcResult result = mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Result<Product> response = objectMapper.readValue(content,
                new TypeReference<Result<Product>>() {});

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(id, response.getData().getId());
        assertEquals("GetById Test Product", response.getData().getName());
    }

    /**
     * 测试4: 更新商品库存接口 - PUT /api/products/{id}/stock
     */
    @Test
    @org.junit.jupiter.api.Order(4)
    public void testUpdateStock() throws Exception {
        Product product = new Product();
        product.setName("Stock Update Test");
        product.setPrice(new BigDecimal("88.88"));
        product.setStock(100);

        MvcResult addResult = mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk())
                .andReturn();

        Result<Product> addResponse = objectMapper.readValue(
                addResult.getResponse().getContentAsString(),
                new TypeReference<Result<Product>>() {});

        String id = addResponse.getData().getId();

        MvcResult result = mockMvc.perform(put("/api/products/" + id + "/stock")
                .param("stock", "50"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Result<Product> response = objectMapper.readValue(content,
                new TypeReference<Result<Product>>() {});

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(50, response.getData().getStock());
    }

    /**
     * 测试5: 更新商品信息接口 - PUT /api/products/{id}
     */
    @Test
    @org.junit.jupiter.api.Order(5)
    public void testUpdateProduct() throws Exception {
        Product product = new Product();
        product.setName("Original Name");
        product.setDescription("Original description");
        product.setPrice(new BigDecimal("100.00"));
        product.setStock(10);

        MvcResult addResult = mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk())
                .andReturn();

        Result<Product> addResponse = objectMapper.readValue(
                addResult.getResponse().getContentAsString(),
                new TypeReference<Result<Product>>() {});

        String id = addResponse.getData().getId();

        Product updateProduct = new Product();
        updateProduct.setName("Updated Name");
        updateProduct.setDescription("Updated description");
        updateProduct.setPrice(new BigDecimal("200.00"));

        MvcResult result = mockMvc.perform(put("/api/products/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProduct)))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Result<Product> response = objectMapper.readValue(content,
                new TypeReference<Result<Product>>() {});

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals("Updated Name", response.getData().getName());
        assertEquals("Updated description", response.getData().getDescription());
        assertEquals(new BigDecimal("200.00"), response.getData().getPrice());
    }

    /**
     * 测试6: 删除商品接口 - DELETE /api/products/{id}
     */
    @Test
    @org.junit.jupiter.api.Order(6)
    public void testDeleteProduct() throws Exception {
        Product product = new Product();
        product.setName("Delete Test Product");
        product.setPrice(new BigDecimal("10.00"));
        product.setStock(1);

        MvcResult addResult = mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk())
                .andReturn();

        Result<Product> addResponse = objectMapper.readValue(
                addResult.getResponse().getContentAsString(),
                new TypeReference<Result<Product>>() {});

        String id = addResponse.getData().getId();

        MvcResult deleteResult = mockMvc.perform(delete("/api/products/" + id))
                .andExpect(status().isOk())
                .andReturn();

        String deleteContent = deleteResult.getResponse().getContentAsString();
        Result<Void> deleteResponse = objectMapper.readValue(deleteContent,
                new TypeReference<Result<Void>>() {});

        assertEquals(200, deleteResponse.getCode());

        MvcResult getResult = mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andReturn();

        String getContent = getResult.getResponse().getContentAsString();
        Result<Product> getResponse = objectMapper.readValue(getContent,
                new TypeReference<Result<Product>>() {});

        assertEquals(500, getResponse.getCode());
    }

    /**
     * 测试7: 添加商品参数校验
     */
    @Test
    @org.junit.jupiter.api.Order(7)
    public void testAddProductValidation() throws Exception {
        Product product1 = new Product();
        product1.setName("");
        product1.setPrice(new BigDecimal("100.00"));
        product1.setStock(10);

        MvcResult result1 = mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product1)))
                .andExpect(status().isOk())
                .andReturn();

        Result<Product> response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(),
                new TypeReference<Result<Product>>() {});

        assertEquals(500, response1.getCode());

        Product product2 = new Product();
        product2.setName("Test Product");
        product2.setStock(10);

        MvcResult result2 = mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product2)))
                .andExpect(status().isOk())
                .andReturn();

        Result<Product> response2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(),
                new TypeReference<Result<Product>>() {});

        assertEquals(500, response2.getCode());
    }
}
