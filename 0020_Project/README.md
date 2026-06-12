# 实时排队叫号系统

一个轻量级的实时排队叫号系统，适用于诊所、奶茶店、办事窗口等场景。

## 功能特性

- 用户取号，支持选择业务类型（咨询、办理、售后）
- 工作人员叫号、完成、过号、重新叫号
- 实时显示当前叫号、等待队列、窗口状态
- 最近 10 条叫号记录
- 多浏览器实时同步（WebSocket）
- 数据定时持久化到本地 JSON 文件，重启可恢复
- 响应式设计，适配电脑和 iPad

## 技术栈

### 前端
- React 18 + TypeScript
- Vite 构建工具
- EventBus 组件间通信（非全局变量）
- WebSocket 实时通信（支持待发送队列、自动重连、手动关闭不再重连）

### 后端
- Java 8（JDK8）
- Java-WebSocket 库
- Jackson JSON 序列化
- 内存存储 + JSON 文件持久化（原子写入，安全可靠）

## 项目结构

```
0020_Project/
├── client/                          # 前端源码
│   ├── App.tsx                      # 主组件，事件中转，布局整合
│   ├── Header.tsx                   # 顶部：当前叫号展示
│   ├── WaitingQueue.tsx             # 中间左侧：等待队列
│   ├── CallRecords.tsx              # 底部：最近叫号记录
│   ├── CounterPanel.tsx             # 中间右侧：窗口操作面板
│   ├── TicketForm.tsx               # 中间右侧：取号表单
│   ├── EventBus.ts                  # 事件总线
│   ├── types.ts                     # TypeScript 类型定义
│   ├── websocket.ts                 # WebSocket 连接管理
│   ├── main.tsx                     # React 入口
│   └── styles.css                   # 全局样式（响应式布局）
├── server/                          # 后端源码
│   ├── pom.xml                      # Maven 配置
│   └── src/main/java/com/queue/
│       ├── AppServer.java           # 主服务器启动类
│       ├── QueueWebSocket.java      # WebSocket 处理类（含字段校验）
│       ├── QueueService.java        # 业务逻辑层（synchronized 同步）
│       ├── FileStore.java           # 文件持久化（原子写入、可配置路径）
│       ├── Ticket.java              # 号票实体
│       ├── Counter.java             # 窗口实体
│       ├── CallRecord.java          # 叫号记录实体
│       ├── QueueState.java          # 队列状态
│       └── WsMessage.java           # WebSocket 消息
├── package.json                     # 前端依赖
├── vite.config.js                   # Vite 配置
├── tsconfig.json                    # TypeScript 配置
├── tsconfig.node.json               # TypeScript Node 配置
├── index.html                       # HTML 入口
└── README.md                        # 说明文档
```

## 各文件职责说明

### 前端文件

| 文件 | 职责 |
|------|------|
| [App.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/App.tsx) | 主组件，初始化 WebSocket（StrictMode 防重复），订阅 EventBus 事件并转发到后端，整合布局：顶部Header + 中间左队列/右操作 + 底部CallRecords |
| [Header.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/Header.tsx) | 顶部组件：显示系统标题、当前叫号号码、窗口、业务类型、等待人数 |
| [WaitingQueue.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/WaitingQueue.tsx) | 中间左侧组件：显示等待队列，按取号时间排序 |
| [CallRecords.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/CallRecords.tsx) | 底部组件：显示最近 10 条叫号记录（叫号/完成/过号/重叫） |
| [CounterPanel.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/CounterPanel.tsx) | 中间右侧组件：窗口操作（选择窗口、叫号、完成、过号、重新叫号） |
| [TicketForm.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/TicketForm.tsx) | 中间右侧组件：取号表单，选择业务类型后取号 |
| [EventBus.ts](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/EventBus.ts) | 事件总线：发布-订阅模式，组件间通信，不使用全局变量 |
| [types.ts](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/types.ts) | TypeScript 类型定义：号票、窗口、队列状态、WebSocket 消息等 |
| [websocket.ts](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/websocket.ts) | WebSocket 连接管理：支持 VITE_WS_URL 配置、ws/wss 自动选择、待发送队列、手动关闭不重连 |
| [styles.css](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/styles.css) | 全局样式：响应式布局（电脑/iPad/手机）、颜色主题 |

### 后端文件

| 文件 | 职责 |
|------|------|
| [AppServer.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/AppServer.java) | 主启动类：初始化各模块、启动 WebSocket 服务器、注册关闭钩子 |
| [QueueWebSocket.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/QueueWebSocket.java) | WebSocket 处理：连接管理、payload 校验、字段非空校验、状态广播 |
| [QueueService.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/QueueService.java) | 业务逻辑：synchronized 同步保护，取号、叫号、完成、过号、重新叫号 |
| [FileStore.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/FileStore.java) | 文件持久化：可配置路径，先写临时文件再原子替换，防写坏数据 |
| [Ticket.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/Ticket.java) | 号票实体：号码、业务类型、状态、时间等 |
| [Counter.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/Counter.java) | 窗口实体：ID、名称、状态、当前办理号票 |
| [CallRecord.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/CallRecord.java) | 叫号记录：号票、窗口、操作类型、时间 |
| [QueueState.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/QueueState.java) | 队列状态：等待队列、窗口列表、当前叫号、叫号记录 |
| [WsMessage.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/WsMessage.java) | WebSocket 消息：动作类型 + 载荷数据 |

## 数据流说明

```
用户操作 (取号/叫号/完成/过号/重叫)
    ↓
组件发布 EventBus 事件
    (TicketForm / CounterPanel)
    ↓
App.tsx 订阅事件（StrictMode 下 useRef 防重复）
    ↓
WebSocket.send() 发送到 Java 后端
    (未连接时进入 pendingQueue，连接成功后统一 flush)
    (ws/wss 根据页面协议自动选择，支持 VITE_WS_URL 覆盖)
    ↓
QueueWebSocket 接收并解析消息
    (payload 非空校验 + 字段合法性校验，非法消息只打日志不抛异常)
    ↓
QueueService 更新内存队列
    (所有 public 方法 synchronized 同步保护，避免读到不一致数据)
    ↓
FileStore 定时保存到本地 JSON 文件
    (先写临时文件 .tmp，成功后原子替换正式文件)
    (默认路径 ~/.queue-system/queue-data.json，支持 -D 和环境变量配置)
    ↓
QueueWebSocket 广播 STATE_UPDATE
    给所有连接的前端
    ↓
前端 websocket.ts 接收消息
    ↓
EventBus 发布 QUEUE_STATE_UPDATED
    ↓
各组件订阅事件更新 UI
    (Header / WaitingQueue / CallRecords / CounterPanel)
```

## 启动说明

### 前置要求

- JDK 8+
- Node.js 16+
- Maven 3.6+

### 启动后端

```bash
cd server
mvn clean package
java -jar target/queue-system-server-jar-with-dependencies.jar
```

**自定义数据文件路径（可选）：**

```bash
# 方式1：系统属性
java -Dqueue.data.file=/var/data/queue.json -jar target/queue-system-server-jar-with-dependencies.jar

# 方式2：环境变量（Linux/macOS）
export QUEUE_DATA_FILE=/var/data/queue.json
java -jar target/queue-system-server-jar-with-dependencies.jar

# 方式2：环境变量（Windows PowerShell）
$env:QUEUE_DATA_FILE = "D:\data\queue.json"
java -jar target/queue-system-server-jar-with-dependencies.jar
```

后端 WebSocket 服务地址：`ws://localhost:8080/ws`

**默认数据文件位置：**
- Windows：`C:\Users\<用户名>\.queue-system\queue-data.json`
- Linux/macOS：`~/.queue-system/queue-data.json`

### 启动前端

```bash
npm install
npm run dev
```

前端开发服务器将在 `http://localhost:5173` 启动。

**自定义 WebSocket 地址（可选）：**

创建 `.env.local` 文件或设置环境变量：

```bash
# .env.local
VITE_WS_URL=ws://192.168.1.100:8080/ws

# 或者 HTTPS 场景
VITE_WS_URL=wss://your-domain.com/ws
```

**WebSocket 地址选择优先级：**
1. `VITE_WS_URL` 环境变量（最高优先级）
2. 页面协议自动推断：
   - 开发环境：`ws://localhost:8080/ws` 或 `wss://localhost:8080/ws`（根据页面是 http 还是 https）
   - 生产环境：`ws(s)://<当前域名>/ws`

### 生产构建

```bash
npm run build
```

构建产物在 `dist` 目录，可以部署到任意静态文件服务器（Nginx、Apache 等）。

如果前后端不同端口或不同域名，需要设置 `VITE_WS_URL` 指向正确的后端地址。

## 使用说明

### 页面布局

```
┌─────────────────────────────────────────────────────┐
│  Header: 系统标题 / 当前叫号 / 等待人数              │
├──────────────────────────────────┬──────────────────┤
│  WaitingQueue: 等待队列          │  TicketForm:     │
│  (号码 + 业务类型 + 时间)        │  取号表单        │
│                                  ├──────────────────┤
│                                  │  CounterPanel:   │
│                                  │  窗口选择 +      │
│                                  │  叫号/完成/过号  │
│                                  │  /重新叫号按钮   │
├──────────────────────────────────┴──────────────────┤
│  CallRecords: 最近 10 条叫号记录                     │
└─────────────────────────────────────────────────────┘
```

### 用户取号
1. 打开网页
2. 在中间右侧"取号"面板选择业务类型（咨询/办理/售后）
3. 点击"取号"按钮
4. 号码会出现在中间左侧等待队列中

### 工作人员操作
1. 在中间右侧"窗口操作"面板选择一个窗口
2. 点击"叫下一个号"呼叫等待队列中的第一位
3. 办理完成后点击"完成"（绿色）
4. 如果用户不在，点击"过号"（橙色）
5. 需要重新呼叫当前号码时点击"重新叫号"

### 查看状态
- 顶部 Header：显示当前正在叫号的号码、窗口、业务类型、等待人数
- 中间左侧 WaitingQueue：显示等待队列，按取号时间排序
- 中间右侧：上半部分取号，下半部分窗口操作
- 底部 CallRecords：显示最近 10 条叫号记录

## 界面配色

- 主色：蓝色 `#2563eb` - 当前叫号、叫号操作、叫号记录
- 完成：绿色 `#16a34a` - 完成操作、完成记录
- 过号：橙色 `#f97316` - 过号操作、过号记录
- 重叫：紫色 `#4f46e5` - 重新叫号记录
- 背景：浅色 `#f8fafc` - 页面背景

## 增强点说明

### 前端增强

1. **布局修复**：拆分为 Header、WaitingQueue、CallRecords 三个独立组件，App.tsx 整合为顶部-中间(左/右)-底部布局
2. **WebSocket 地址**：支持 `VITE_WS_URL` 环境变量，根据页面协议自动选择 `ws` / `wss`
3. **StrictMode 防重复**：`useRef(boolean)` + 内部 `connecting` 状态双重保护，不会重复创建连接
4. **手动关闭不重连**：区分 `manuallyClosed` 标记，组件卸载调用 `close()` 后不再自动重连
5. **待发送队列**：连接未就绪时消息暂存 `pendingQueue`，`onopen` 后统一 `flushPendingQueue()` 发送，避免用户刚打开就点取号失败

### 后端增强

1. **同步保护**：`restoreState()`、`getQueueState()`、`getCounters()` 全部加 `synchronized`，与其他修改方法共用同一把锁，防止保存/广播时读到半更新数据
2. **Payload 校验**：`QueueWebSocket` 所有 handler 前先校验 `payload` 非空、字段存在、类型正确、值合法（如 businessType 必须是咨询/办理/售后），非法消息仅打日志返回，不抛异常
3. **数据文件路径**：
   - 默认固定在用户目录 `~/.queue-system/queue-data.json`，不再依赖运行目录
   - 支持 `-Dqueue.data.file=xxx` 系统属性
   - 支持 `QUEUE_DATA_FILE` 环境变量
4. **原子写入**：保存时先写 `queue-data.json.tmp` 临时文件，成功后用 `Files.move(ATOMIC_MOVE)` 原子替换正式文件；加载时若正式文件不存在，会尝试从临时文件恢复

## 注意事项

1. 数据保存在内存中，定时（30秒）持久化到 JSON 文件
2. 服务重启后会自动从数据文件恢复状态
3. 默认有 3 个窗口（1号、2号、3号），可在 `QueueService.initCounters()` 修改
4. 叫号记录最多保留 50 条，前端显示最近 10 条
5. 多个浏览器同时打开时，所有操作都会通过 WebSocket 实时同步
6. 生产环境若前后端不同端口或使用 HTTPS，务必设置 `VITE_WS_URL` 指向正确的 `ws(s)://...` 地址
