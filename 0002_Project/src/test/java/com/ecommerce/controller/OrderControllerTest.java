package com.ecommerce.controller;

import com.ecommerce.EcommerceApplication;
import com.ecommerce.common.Result;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderStatus;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 订单控制器单元测试
 * 测试OrderController中的所有API接口
 */
@SpringBootTest(classes = EcommerceApplication.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String testOrderId;
    private static String testCartItemId;
    private static String testProductId;

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
        product.setName("Order Test Product");
        product.setDescription("For order testing");
        product.setPrice(new BigDecimal("100.00"));
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
     * 准备测试购物车项
     */
    private String createTestCartItem(String productId) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("productId", productId);
        params.put("quantity", 2);

        MvcResult result = mockMvc.perform(post("/api/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andReturn();

        Result<CartItem> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<Result<CartItem>>() {});

        return response.getData().getId();
    }

    /**
     * 测试1: 从购物车创建订单接口 - POST /api/orders/create
     */
    @Test
    @org.junit.jupiter.api.Order(1)
    public void testCreateOrderFromCart() throws Exception {
        String productId = createTestProduct();
        
        String cartItemId = createTestCartItem(productId);
        testProductId = productId;
        testCartItemId = cartItemId;

        List<String> cartItemIds = Collections.singletonList(cartItemId);
        Map<String, Object> params = new HashMap<>();
        params.put("cartItemIds", cartItemIds);

        MvcResult result = mockMvc.perform(post("/api/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Result<Order> response = objectMapper.readValue(content,
                new TypeReference<Result<Order>>() {});

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertNotNull(response.getData().getId());
        assertNotNull(response.getData().getOrderNo());
        assertEquals(OrderStatus.PENDING, response.getData().getStatus());
        assertEquals(new BigDecimal("200.00"), response.getData().getTotalAmount());
        assertEquals(1, response.getData().getItems().size());

        testOrderId = response.getData().getId();

        MvcResult cartResult = mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andReturn();

        Result<List<CartItem>> cartResponse = objectMapper.readValue(
                cartResult.getResponse().getContentAsString(),
                new TypeReference<Result<List<CartItem>>>() {});

        assertEquals(0, cartResponse.getData().size());
    }

    /**
     * 测试2: 获取订单列表接口 - GET /api/orders
     */
    @Test
    @org.junit.jupiter.api.Order(2)
    public void testGetOrderList() throws Exception {
        String productId = createTestProduct();
        String cartItemId = createTestCartItem(productId);

        List<String> cartItemIds = Collections.singletonList(cartItemId);
        Map<String, Object> params = new HashMap<>();
        params.put("cartItemIds", cartItemIds);

        mockMvc.perform(post("/api/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Result<List<Order>> response = objectMapper.readValue(content,
                new TypeReference<Result<List<Order>>>() {});

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertTrue(response.getData().size() > 0);
    }

    /**
     * 测试3: 根据ID获取订单接口 - GET /api/orders/{id}
     */
    @Test
    @org.junit.jupiter.api.Order(3)
    public void testGetOrderById() throws Exception {
        String productId = createTestProduct();
        String cartItemId = createTestCartItem(productId);

        List<String> cartItemIds = Collections.singletonList(cartItemId);
        Map<String, Object> createParams = new HashMap<>();
        createParams.put("cartItemIds", cartItemIds);

        MvcResult createResult = mockMvc.perform(post("/api/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createParams)))
                .andExpect(status().isOk())
                .andReturn();

        Result<Order> createResponse = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                new TypeReference<Result<Order>>() {});

        String orderId = createResponse.getData().getId();

        MvcResult result = mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Result<Order> response = objectMapper.readValue(content,
                new TypeReference<Result<Order>>() {});

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(orderId, response.getData().getId());
        assertEquals(OrderStatus.PENDING, response.getData().getStatus());
    }

    /**
     * 测试4: 支付订单接口 - POST /api/orders/{id}/pay
     */
    @Test
    @org.junit.jupiter.api.Order(4)
    public void testPayOrder() throws Exception {
        String productId = createTestProduct();
        String cartItemId = createTestCartItem(productId);

        List<String> cartItemIds = Collections.singletonList(cartItemId);
        Map<String, Object> createParams = new HashMap<>();
        createParams.put("cartItemIds", cartItemIds);

        MvcResult createResult = mockMvc.perform(post("/api/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createParams)))
                .andExpect(status().isOk())
                .andReturn();

        Result<Order> createResponse = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                new TypeReference<Result<Order>>() {});

        String orderId = createResponse.getData().getId();

        MvcResult result = mockMvc.perform(post("/api/orders/" + orderId + "/pay"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Result<Order> response = objectMapper.readValue(content,
                new TypeReference<Result<Order>>() {});

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(OrderStatus.PAID, response.getData().getStatus());
        assertNotNull(response.getData().getPaidAt());
    }

    /**
     * 测试5: 取消订单接口 - POST /api/orders/{id}/cancel
     */
    @Test
    @org.junit.jupiter.api.Order(5)
    public void testCancelOrder() throws Exception {
        String productId = createTestProduct();
        String cartItemId = createTestCartItem(productId);

        List<String> cartItemIds = Collections.singletonList(cartItemId);
        Map<String, Object> createParams = new HashMap<>();
        createParams.put("cartItemIds", cartItemIds);

        MvcResult createResult = mockMvc.perform(post("/api/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createParams)))
                .andExpect(status().isOk())
                .andReturn();

        Result<Order> createResponse = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                new TypeReference<Result<Order>>() {});

        String orderId = createResponse.getData().getId();

        Map<String, Object> cancelParams = new HashMap<>();
        cancelParams.put("reason", "Test cancel reason");

        MvcResult result = mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelParams)))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Result<Order> response = objectMapper.readValue(content,
                new TypeReference<Result<Order>>() {});

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(OrderStatus.CANCELLED, response.getData().getStatus());
        assertEquals("Test cancel reason", response.getData().getCancelReason());
        assertNotNull(response.getData().getCancelledAt());
    }

    /**
     * 测试6: 确认收货（订单完成）接口 - POST /api/orders/{id}/complete
     */
    @Test
    @org.junit.jupiter.api.Order(6)
    public void testCompleteOrder() throws Exception {
        String productId = createTestProduct();
        String cartItemId = createTestCartItem(productId);

        List<String> cartItemIds = Collections.singletonList(cartItemId);
        Map<String, Object> createParams = new HashMap<>();
        createParams.put("cartItemIds", cartItemIds);

        MvcResult createResult = mockMvc.perform(post("/api/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createParams)))
                .andExpect(status().isOk())
                .andReturn();

        Result<Order> createResponse = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                new TypeReference<Result<Order>>() {});

        String orderId = createResponse.getData().getId();

        mockMvc.perform(post("/api/orders/" + orderId + "/pay"))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(post("/api/orders/" + orderId + "/complete"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Result<Order> response = objectMapper.readValue(content,
                new TypeReference<Result<Order>>() {});

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(OrderStatus.COMPLETED, response.getData().getStatus());
        assertNotNull(response.getData().getCompletedAt());
    }

    /**
     * 测试7: 订单状态流转校验 - 已支付订单不能取消
     */
    @Test
    @org.junit.jupiter.api.Order(7)
    public void testPaidOrderCannotBeCancelled() throws Exception {
        String productId = createTestProduct();
        String cartItemId = createTestCartItem(productId);

        List<String> cartItemIds = Collections.singletonList(cartItemId);
        Map<String, Object> createParams = new HashMap<>();
        createParams.put("cartItemIds", cartItemIds);

        MvcResult createResult = mockMvc.perform(post("/api/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createParams)))
                .andExpect(status().isOk())
                .andReturn();

        Result<Order> createResponse = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                new TypeReference<Result<Order>>() {});

        String orderId = createResponse.getData().getId();

        mockMvc.perform(post("/api/orders/" + orderId + "/pay"))
                .andExpect(status().isOk());

        Map<String, Object> cancelParams = new HashMap<>();
        cancelParams.put("reason", "Test cancel paid order");

        MvcResult result = mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelParams)))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Result<Order> response = objectMapper.readValue(content,
                new TypeReference<Result<Order>>() {});

        assertEquals(500, response.getCode());
    }

    /**
     * 测试8: 创建订单参数校验
     */
    @Test
    @org.junit.jupiter.api.Order(8)
    public void testCreateOrderValidation() throws Exception {
        Map<String, Object> params1 = new HashMap<>();
        params1.put("cartItemIds", new ArrayList<>());

        MvcResult result1 = mockMvc.perform(post("/api/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params1)))
                .andExpect(status().isOk())
                .andReturn();

        Result<Order> response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(),
                new TypeReference<Result<Order>>() {});

        assertEquals(500, response1.getCode());

        Map<String, Object> params2 = new HashMap<>();

        MvcResult result2 = mockMvc.perform(post("/api/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params2)))
                .andExpect(status().isOk())
                .andReturn();

        Result<Order> response2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(),
                new TypeReference<Result<Order>>() {});

        assertEquals(500, response2.getCode());
    }
}
