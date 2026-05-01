# 电商小系统

一个基于 Spring Boot + Vue 的简单电商小系统

## 项目简介

这是一个功能完整、代码简洁的电商演示系统，采用分层架构设计，使用内存存储配合JSON文件持久化，支持程序重启后数据不丢失。

## 技术栈

### 后端
- **语言**: Java 8 (JDK 1.8)
- **框架**: Spring Boot 2.7.18
- **构建工具**: Maven
- **数据持久化**: 内存存储 + JSON文件
- **JSON处理**: Jackson
- **代码简化**: Lombok

### 前端
- **框架**: Vue 2.7 (CDN方式)
- **HTTP客户端**: Axios
- **样式**: 原生CSS (Grid布局)

## 项目结构

```
ecommerce-system/
├── pom.xml                          # Maven配置文件
├── data.json                          # 数据持久化文件（运行时自动生成）
├── frontend/
│   └── index.html                    # Vue前端页面（CDN方式）
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/
│   │   │   ├── EcommerceApplication.java    # Spring Boot启动类
│   │   │   ├── common/
│   │   │   │   ├── Result.java              # 通用响应结果类
│   │   │   │   └── GlobalExceptionHandler.java  # 全局异常处理器
│   │   │   ├── config/
│   │   │   │   └── CorsConfig.java         # 跨域配置
│   │   │   ├── controller/
│   │   │   │   ├── ProductController.java   # 商品控制器
│   │   │   │   ├── CartController.java      # 购物车控制器
│   │   │   │   └── OrderController.java    # 订单控制器
│   │   │   ├── service/
│   │   │   │   ├── ProductService.java    # 商品服务
│   │   │   │   ├── CartService.java       # 购物车服务
│   │   │   │   └── OrderService.java      # 订单服务
│   │   │   ├── repository/
│   │   │   │   ├── DataStore.java          # 数据存储（单例模式）
│   │   │   │   └── DataPersistenceManager.java  # 数据持久化管理器
│   │   │   ├── model/
│   │   │   │   ├── Product.java              # 商品实体
│   │   │   │   ├── CartItem.java             # 购物车项实体
│   │   │   │   ├── Order.java                # 订单实体
│   │   │   │   ├── OrderItem.java            # 订单项实体
│   │   │   │   └── OrderStatus.java          # 订单状态枚举
│   │   │   └── util/
│   │   │       └── IdGenerator.java          # ID生成器（工厂模式）
│   │   └── resources/
│   │       └── application.properties        # 应用配置
│   └── test/
│       └── java/com/ecommerce/controller/
│           ├── ProductControllerTest.java    # 商品控制器单元测试
│           ├── CartControllerTest.java       # 购物车控制器单元测试
│           └── OrderControllerTest.java      # 订单控制器单元测试
└── README.md                                  # 本文档
```

## 功能模块

### 1. 商品管理
- **添加商品**: 支持添加商品名称、描述、价格、库存
- **查询商品列表**: 获取所有商品信息
- **修改库存**: 支持直接修改库存数量

### 2. 购物车
- **添加商品到购物车**: 将商品加入购物车，支持数量累加
- **修改数量**: 增减购物车中商品的数量
- **删除商品**: 从购物车移除商品
- **计算总价**: 自动计算购物车总金额
- **批量操作**: 支持全选、批量删除、批量下单

### 3. 订单功能
- **从购物车下单**: 选择购物车中的商品创建订单
- **支付订单**: 模拟支付，状态从"待支付"变为"已支付"
- **取消订单**: 只有"待支付"状态的订单可以取消，取消后恢复库存
- **确认收货**: 订单状态从"已支付"变为"已完成"
- **查看订单列表**: 查看所有订单，支持状态统计

## 订单状态流转

```
┌─────────────┐
│   待支付    │ (PENDING)
└──────┬──────┘
       │ │
       │ └──────────────────┐
       ▼                        ▼
┌─────────────┐         ┌─────────────┐
│   已支付    │         │   已取消    │
│   (PAID)    │         │ (CANCELLED) │
└──────┬──────┘         └─────────────┘
       │
       ▼
┌─────────────┐
│   已完成    │ (COMPLETED)
└─────────────┘
```

**状态说明：
- **待支付 (PENDING)：订单创建后的初始状态，可以支付或取消
- **已支付 (PAID)：订单已支付，可以确认收货
- **已取消 (CANCELLED)：订单已取消，终态
- **已完成 (COMPLETED)：订单已完成，终态

**规则**：
- 只有**待支付**状态的订单才能取消
- 已取消的订单会恢复商品库存

## 设计模式应用

### 1. 单例模式 (Singleton)
- **位置**: `com.ecommerce.repository.DataStore`
- **说明**: 使用饿汉式单例模式，确保整个应用中只有一个数据存储实例，保证数据一致性

```java
public class DataStore {
    // 单例实例（饿汉式）
    private static final DataStore INSTANCE = new DataStore();
    
    // 私有构造函数
    private DataStore() {}
    
    // 获取单例实例
    public static DataStore getInstance() {
        return INSTANCE;
    }
}
```

### 2. 工厂模式 (Factory)
- **位置**: `com.ecommerce.util.IdGenerator`
- **说明**: 使用工厂模式生成各种类型的唯一ID，包括商品ID、购物车项ID、订单编号等

```java
public class IdGenerator {
    // 生成商品ID
    public static String generateProductId() {
        return "PRD" + generateUUID().substring(0, 8);
    }
    
    // 生成订单编号
    public static String generateOrderNo() {
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000);
        return String.format("ORD%d%04d", timestamp, random);
    }
}
```

### 3. 状态模式思想 (State Pattern)
- **位置**: `com.ecommerce.model.OrderStatus`
- **说明**: 通过枚举的`canTransitionTo`方法控制订单状态流转，确保状态转换的合法性

```java
public enum OrderStatus {
    PENDING("待支付"),
    PAID("已支付"),
    CANCELLED("已取消"),
    COMPLETED("已完成");
    
    // 检查是否可以从当前状态转换到目标状态
    public boolean canTransitionTo(OrderStatus targetStatus) {
        switch (this) {
            case PENDING:
                return targetStatus == PAID || targetStatus == CANCELLED;
            case PAID:
                return targetStatus == COMPLETED;
            default:
                return false;
        }
    }
}
```

### 4. 门面模式 (Facade)
- **位置**: `com.ecommerce.repository.DataPersistenceManager`
- **说明**: 为复杂的数据持久化操作提供统一的接口，简化上层调用

## API接口文档

### 商品接口

#### 1. 获取商品列表
- **URL**: `GET /api/products`
- **响应示例**:
```json
{
    "code": 200,
    "message": "success",
    "data": [
        {
            "id": "PRD12345678",
            "name": "商品名称",
            "description": "商品描述",
            "price": 99.99,
            "stock": 100,
            "createdAt": "2026-05-01T10:00:00",
            "updatedAt": "2026-05-01T10:00:00"
        }
    ]
}
```

#### 2. 根据ID获取商品
- **URL**: `GET /api/products/{id}`

#### 3. 添加商品
- **URL**: `POST /api/products`
- **请求体**:
```json
{
    "name": "商品名称",
    "description": "商品描述",
    "price": 99.99,
    "stock": 100
}
```

#### 4. 更新库存
- **URL**: `PUT /api/products/{id}/stock?stock=50`

#### 5. 更新商品信息
- **URL**: `PUT /api/products/{id}`

#### 6. 删除商品
- **URL**: `DELETE /api/products/{id}`

---

### 购物车接口

#### 1. 获取购物车列表
- **URL**: `GET /api/cart`

#### 2. 添加商品到购物车
- **URL**: `POST /api/cart`
- **请求体**:
```json
{
    "productId": "PRD12345678",
    "quantity": 2
}
```

#### 3. 更新购物车项数量
- **URL**: `PUT /api/cart/{id}`
- **请求体**:
```json
{
    "quantity": 5
}
```

#### 4. 删除购物车项
- **URL**: `DELETE /api/cart/{id}`

#### 5. 清空购物车
- **URL**: `DELETE /api/cart`

#### 6. 计算购物车总价
- **URL**: `GET /api/cart/total`

---

### 订单接口

#### 1. 获取订单列表
- **URL**: `GET /api/orders`

#### 2. 根据ID获取订单
- **URL**: `GET /api/orders/{id}`

#### 3. 从购物车创建订单
- **URL**: `POST /api/orders/create`
- **请求体**:
```json
{
    "cartItemIds": ["CIT12345678", "CIT87654321"]
}
```

#### 4. 支付订单
- **URL**: `POST /api/orders/{id}/pay`

#### 5. 取消订单
- **URL**: `POST /api/orders/{id}/cancel`
- **请求体**:
```json
{
    "reason": "用户取消"
}
```

#### 6. 确认收货
- **URL**: `POST /api/orders/{id}/complete`

---

### 通用响应格式

```json
{
    "code": 200,
    "message": "success",
    "data": {}
}
```

- **code**: 200表示成功，其他表示失败
- **message**: 响应消息
- **data**: 响应数据

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+

### 运行步骤

#### 1. 编译项目

```bash
cd e:\work\SOLO Coder\solo_project\0002_Project
mvn clean package
```

#### 2. 启动后端服务

```bash
mvn spring-boot:run
```

或运行编译后的jar包：

```bash
java -jar target/ecommerce-system-1.0.0.jar
```

服务启动后访问：`http://localhost:8080`

#### 3. 访问前端页面

直接用浏览器打开 `frontend/index.html` 文件即可。

> 注意：由于使用CDN方式的前端页面无需安装任何依赖，直接打开即可使用。

### 运行单元测试

```bash
mvn test
```

测试类位置：
- `ProductControllerTest`: 商品相关接口测试
- `CartControllerTest`: 购物车相关接口测试
- `OrderControllerTest`: 订单相关接口测试

### 数据持久化

数据默认保存在项目根目录下的 `data.json` 文件中。

- **自动保存**: 应用正常关闭时自动保存数据
- **自动加载**: 应用启动时自动加载数据
- **手动触发**: 也可以通过调用接口间接触发

修改后都会自动保存

## 代码分层说明

### Controller层（控制器层）
- 负责接收HTTP请求
- 参数校验
- 调用Service层处理业务逻辑
- 返回统一的响应结果

### Service层（服务层）
- 业务逻辑处理
- 数据校验
- 调用Repository层访问数据

### Repository层（数据访问层）
- 数据的增删改查
- 数据持久化（文件读写）

### Model层（模型层）
- 数据实体类
- 数据结构定义

## 扩展建议

如果需要扩展功能，可以考虑：

1. **数据库替换**: 将内存存储改为MySQL/PostgreSQL等关系型数据库
2. **用户系统**: 添加用户注册登录功能
3. **权限控制**: 添加Spring Security进行权限管理
4. **支付集成**: 集成支付宝、微信支付等真实支付
5. **缓存优化**: 添加Redis缓存提高性能
6. **消息队列**: 使用RabbitMQ处理订单异步处理
7. **前端框架**: 使用Vue CLI创建完整的Vue项目
8. **API文档**: 集成Swagger/OpenAPI文档

## 许可证

MIT License

## 联系方式

如有问题，欢迎提交Issue或Pull Request。
