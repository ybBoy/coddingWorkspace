# 实时排队叫号系统

一个轻量级的实时排队叫号系统，适用于诊所、奶茶店、办事窗口等场景。

## 功能特性

### 核心功能
- 用户取号，支持选择业务类型（咨询、办理、售后）
- 工作人员叫号、完成、过号、重新叫号
- 实时显示当前叫号、等待队列、窗口状态
- 最近 10 条叫号记录
- 多浏览器实时同步（WebSocket）
- 数据定时持久化到本地 JSON 文件，重启可恢复
- 响应式设计，适配电脑和 iPad

### 迭代新增（v2.0）
- **连接状态与操作反馈**：右上角显示 WebSocket 在线/重连中/离线状态，所有操作（取号/叫号/完成等）给出 Toast 成功/失败提示
- **过号管理**：过号号码独立保留在过号列表，支持重新入队、直接重叫、标记结束
- **按业务类型筛选叫号**：可选择全部业务或指定类型，优先从对应类型中按时间叫号
- **窗口配置能力**：支持从 counters.json 加载配置，前端可新增、编辑名称、配置支持的业务类型、启用/停用窗口，不再硬编码
- **今日统计面板**：实时展示今日取号总数、等待、办理中、完成、过号、平均等待时长

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
│   ├── Header.tsx                   # 顶部：当前叫号展示 + 连接状态
│   ├── ConnectionStatus.tsx         # 连接状态显示组件
│   ├── ToastContainer.tsx           # Toast 操作反馈组件
│   ├── TodayStats.tsx               # 今日统计面板
│   ├── WaitingQueue.tsx             # 中间左侧：等待队列
│   ├── MissedTickets.tsx            # 中间左侧：过号列表管理
│   ├── CallRecords.tsx              # 底部：最近叫号记录
│   ├── CounterPanel.tsx             # 中间右侧：窗口操作（含业务筛选）
│   ├── CounterConfig.tsx            # 中间右侧：窗口配置管理
│   ├── TicketForm.tsx               # 中间右侧：取号表单
│   ├── EventBus.ts                  # 事件总线
│   ├── types.ts                     # TypeScript 类型定义
│   ├── websocket.ts                 # WebSocket 连接管理
│   ├── main.tsx                     # React 入口
│   └── styles.css                   # 全局样式（响应式布局）
├── server/                          # 后端源码
│   ├── pom.xml                      # Maven 配置
│   ├── counters.json                # 窗口配置文件（默认3个窗口）
│   └── src/main/java/com/queue/
│       ├── AppServer.java           # 主服务器启动类
│       ├── QueueWebSocket.java      # WebSocket 处理类（含字段校验）
│       ├── QueueService.java        # 业务逻辑层（synchronized 同步）
│       ├── FileStore.java           # 文件持久化（原子写入、可配置路径）
│       ├── Ticket.java              # 号票实体
│       ├── Counter.java             # 窗口实体（含业务类型、启用状态）
│       ├── TodayStats.java          # 今日统计实体
│       ├── CallRecord.java          # 叫号记录实体
│       ├── QueueState.java          # 队列状态（含过号队列、统计）
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
| [App.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/App.tsx) | 主组件，初始化 WebSocket（StrictMode 防重复），订阅 EventBus 事件并转发到后端，整合布局 |
| [Header.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/Header.tsx) | 顶部组件：系统标题、当前叫号、等待人数、集成连接状态 |
| [ConnectionStatus.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/ConnectionStatus.tsx) | 连接状态组件：在线/重连中/离线，带闪烁动画 |
| [ToastContainer.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/ToastContainer.tsx) | Toast 提示：操作成功/失败自动消失，滑入滑出动画 |
| [TodayStats.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/TodayStats.tsx) | 今日统计：取号总数、等待、办理中、完成、过号、平均等待时长 |
| [WaitingQueue.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/WaitingQueue.tsx) | 等待队列：按取号时间排序，显示号码、业务类型、时间 |
| [MissedTickets.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/MissedTickets.tsx) | 过号管理：重新入队、直接重叫、标记结束 |
| [CallRecords.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/CallRecords.tsx) | 叫号记录：最近 10 条，不同操作不同颜色 |
| [CounterPanel.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/CounterPanel.tsx) | 窗口操作：选择窗口、业务类型筛选、叫号、完成、过号、重叫 |
| [CounterConfig.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/CounterConfig.tsx) | 窗口配置：新增窗口、编辑名称和业务类型、启用/停用 |
| [TicketForm.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/TicketForm.tsx) | 取号表单：选择业务类型后取号 |
| [EventBus.ts](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/EventBus.ts) | 事件总线：发布-订阅模式，含连接状态、Toast 等新事件 |
| [types.ts](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/types.ts) | 类型定义：新增过号、统计、窗口配置等类型 |
| [websocket.ts](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/websocket.ts) | WebSocket 管理：支持 VITE_WS_URL、ws/wss 自动选择、待发送队列、操作结果转 Toast |
| [styles.css](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/styles.css) | 全局样式：新增 Toast、统计卡片、过号列表、窗口配置等样式 |

### 后端文件

| 文件 | 职责 |
|------|------|
| [AppServer.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/AppServer.java) | 主启动类：从 counters.json 加载窗口配置，初始化各模块 |
| [QueueWebSocket.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/QueueWebSocket.java) | WebSocket 处理：新增消息类型、操作结果反馈、payload 校验 |
| [QueueService.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/QueueService.java) | 业务逻辑：过号列表、业务筛选叫号、窗口 CRUD、今日统计计算 |
| [FileStore.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/FileStore.java) | 文件持久化：新增 counters.json 加载/保存，原子写入 |
| [Ticket.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/Ticket.java) | 号票实体：新增 completedAt 完成时间 |
| [Counter.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/Counter.java) | 窗口实体：新增 enabled 启用状态、supportedBusinessTypes 支持的业务类型 |
| [TodayStats.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/TodayStats.java) | 今日统计实体：6 个统计指标 |
| [CallRecord.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/CallRecord.java) | 叫号记录实体 |
| [QueueState.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/QueueState.java) | 队列状态：新增 missedQueue 过号队列、todayStats 统计 |
| [WsMessage.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/WsMessage.java) | WebSocket 消息：新增消息类型常量 |

## 数据流说明

```
用户操作 (取号/叫号/完成/过号/配置...)
    ↓
组件发布 EventBus 事件
    ↓
App.tsx 订阅事件（StrictMode 下 useRef 防重复）
    ↓
WebSocket.send() 发送到 Java 后端
    (未连接时进入 pendingQueue，连接成功后统一 flush)
    ↓
QueueWebSocket 接收解析 + payload 校验
    → 返回 OPERATION_RESULT 供前端 Toast 提示
    ↓
QueueService 更新内存队列（synchronized 同步）
    → 过号列表、统计数据同步更新
    ↓
FileStore 定时保存到本地 JSON 文件
    (队列数据 queue-data.json + 窗口配置 counters.json)
    ↓
QueueWebSocket 广播 STATE_UPDATE 给所有前端
    ↓
前端 websocket.ts 接收消息
    → STATE_UPDATE → EventBus → 各组件更新 UI
    → OPERATION_RESULT → Toast 提示用户
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

**自定义数据目录（可选）：**

```bash
# 系统属性
java -Dqueue.data.dir=/var/data/queue -jar target/queue-system-server-jar-with-dependencies.jar

# 环境变量
export QUEUE_DATA_DIR=/var/data/queue
java -jar target/queue-system-server-jar-with-dependencies.jar
```

后端 WebSocket 服务地址：`ws://localhost:8080/ws`

**默认文件位置：**
- Windows：`C:\Users\<用户名>\.queue-system\queue-data.json`
- Linux/macOS：`~/.queue-system/queue-data.json`
- 窗口配置：同目录下的 `counters.json`

### 启动前端

```bash
npm install
npm run dev
```

前端开发服务器将在 `http://localhost:5173` 启动。

**自定义 WebSocket 地址（可选）：**

```bash
# .env.local
VITE_WS_URL=ws://192.168.1.100:8080/ws
# HTTPS 场景
VITE_WS_URL=wss://your-domain.com/ws
```

### 生产构建

```bash
npm run build
```

构建产物在 `dist` 目录。

## 使用说明

### 页面布局

```
┌─────────────────────────────────────────────────────────────┐
│  Header: 系统标题 / 连接状态 / 当前叫号 / 等待人数           │
├───────────────────────────────┬─────────────────────────────┤
│  TodayStats: 今日统计          │  TicketForm: 取号表单       │
├───────────────────────────────┼─────────────────────────────┤
│  WaitingQueue: 等待队列       │  CounterPanel: 窗口操作      │
│  (号码 + 业务类型 + 时间)     │  选择窗口                   │
│                               │  业务类型筛选（全部/咨询/...）│
│                               │  叫号/完成/过号/重叫按钮     │
├───────────────────────────────┼─────────────────────────────┤
│  MissedTickets: 过号列表      │  CounterConfig: 窗口配置     │
│  重新入队/直接重叫/标记结束   │  新增/编辑/启用/停用窗口     │
├───────────────────────────────┴─────────────────────────────┤
│  CallRecords: 最近 10 条叫号记录                              │
└─────────────────────────────────────────────────────────────┘
```

### 用户取号
1. 打开网页
2. 在右侧"取号"面板选择业务类型（咨询/办理/售后）
3. 点击"取号"按钮
4. 收到 Toast 成功提示，号码出现在等待队列

### 工作人员操作
1. 在右侧"窗口操作"面板选择一个窗口
2. （可选）选择"叫号业务类型"：全部业务 / 咨询 / 办理 / 售后（仅显示窗口支持的类型）
3. 点击"叫下一个号"（或"叫下一个XX号"）
4. 办理完成后点击"完成"（绿色）
5. 如果用户不在，点击"过号"（橙色）→ 号码进入过号列表
6. 需要重新呼叫当前号码时点击"重新叫号"

### 过号管理
1. 中间左侧"过号列表"显示所有过号号码
2. 对过号号码可以执行 3 种操作：
   - **重新入队**：放回等待队列末尾
   - **直接重叫**：立即在指定窗口叫号
   - **结束**：标记为完成，不加入历史记录

### 窗口配置
1. 右侧"窗口配置"面板显示所有窗口
2. **新增窗口**：输入名称，勾选支持的业务类型，点击"新增窗口"
3. **编辑窗口**：点击"编辑"修改名称和支持的业务类型
4. **启用/停用**：停用后窗口不可选，正在办理的窗口无法停用

### 查看状态
- 右上角：WebSocket 连接状态（绿色在线 / 橙色重连中 / 红色离线）
- 顶部：当前叫号号码、窗口、业务类型、等待人数
- 左侧上方：今日统计 6 项指标，实时更新
- 左侧中间：等待队列
- 左侧下方：过号列表
- 右侧：取号 + 窗口操作 + 窗口配置
- 底部：最近 10 条叫号记录

## 界面配色

- 主色：蓝色 `#2563eb` - 当前叫号、叫号操作
- 完成：绿色 `#16a34a` - 完成操作、完成记录
- 过号：橙色 `#f97316` - 过号操作、过号记录
- 重叫：紫色 `#4f46e5` - 重新叫号记录
- 办理中：青色 `#0891b2` - 今日统计办理中
- 平均等待：紫色 `#7c3aed` - 今日统计平均等待
- 连接在线：绿色 `#22c55e`（脉冲动画）
- 连接重连中：橙色 `#f97316`（闪烁动画）
- 连接离线：红色 `#ef4444`
- 背景：浅色 `#f8fafc`

## 增强点说明

### 前端增强

1. **连接状态显示**：右上角实时显示 WebSocket 连接状态，带动画效果
2. **Toast 操作反馈**：所有操作成功/失败都有 Toast 提示，自动滑入滑出
3. **过号管理**：独立过号列表，支持重新入队、直接重叫、标记结束 3 种操作
4. **业务类型筛选**：叫号时可指定业务类型，仅从对应类型队列中按时间取号
5. **窗口配置**：支持动态新增、编辑、启用/停用窗口，配置支持的业务类型
6. **今日统计**：6 项指标实时更新，含平均等待时长计算
7. **WebSocket 增强**：支持 VITE_WS_URL 配置，ws/wss 自动选择，待发送队列，操作结果转 Toast

### 后端增强

1. **过号列表**：missedQueue 独立队列，不再只进入历史记录
2. **业务筛选叫号**：callNextByType() 支持指定业务类型，窗口支持的业务类型会作为过滤条件
3. **窗口 CRUD**：addCounter() / updateCounter() / toggleCounter() 完整支持
4. **今日统计**：calculateTodayStats() 实时计算 6 项指标，平均等待基于 completedWithWait 统计
5. **操作结果反馈**：所有操作返回 OPERATION_RESULT，前端可显示明确的成功/失败原因
6. **字段校验增强**：新增 supportedBusinessTypes 列表校验、enabled 布尔类型校验
7. **窗口配置持久化**：counters.json 与 queue-data.json 分离，启动时从配置文件加载

---

## 测试验证说明

### 1. 前端构建验证

```bash
cd 0020_Project
npm install
npm run build
```

**预期结果：**
- 无 TypeScript 编译错误
- 产物输出到 dist/ 目录
- dist/index.html 存在
- dist/assets/index-*.js 和 index-*.css 存在
- 构建日志显示 `✓ built in xxxms`

### 2. 后端打包验证

```bash
cd 0020_Project/server
mvn clean package
```

**预期结果：**
- 无 Java 编译错误
- `target/queue-system-server-jar-with-dependencies.jar` 生成
- 构建日志显示 `BUILD SUCCESS`
- jar 包大小应在 1-2MB 左右（含依赖）

### 3. 关键业务流程手动测试

#### 测试用例 1：基础取号叫号流程
| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 启动后端服务 | 控制台显示窗口配置、数据文件位置、WebSocket 端口 8080 |
| 2 | 启动前端，访问 http://localhost:5173 | 右上角显示绿色"在线"，显示 3 个窗口 |
| 3 | 选择"咨询"→ 点击"取号" | Toast 提示"取号成功：001"，等待队列出现 001 号，今日统计"今日取号"=1 |
| 4 | 选择"1号窗口"→ 点击"叫下一个号" | Toast 提示"叫号成功：001"，顶部显示当前叫号 001，窗口状态变"忙碌" |
| 5 | 点击"完成" | Toast 提示"办理完成"，窗口状态变"空闲"，今日统计"完成"=1，底部记录新增"001 完成" |

#### 测试用例 2：过号管理
| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 取号"办理"002 → 叫号 → 点击"过号" | Toast 提示"已标记过号"，过号列表出现 002，今日统计"过号"=1 |
| 2 | 对 002 点击"重新入队" | Toast 提示"已重新加入等待队列"，002 回到等待队列末尾 |
| 3 | 叫号 → 过号 → 点击"直接重叫" | Toast 提示"重新叫号成功"，002 立即成为当前叫号 |
| 4 | 点击"结束" | Toast 提示"已标记结束"，002 从过号列表消失 |

#### 测试用例 3：业务类型筛选叫号
| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 依次取号：咨询 003、办理 004、售后 005 | 等待队列有 3 个号，业务类型不同 |
| 2 | 选择"3号窗口"（仅支持售后）→ 叫号 | 应叫 005（售后），而不是 003（咨询） |
| 3 | 选择"1号窗口"→ 业务类型选"办理"→ 叫号 | Toast 提示"叫号成功：004（办理）"，跳过了前面的 003 |
| 4 | 业务类型选"全部业务"→ 叫号 | 应叫 003（最前面的） |

#### 测试用例 4：窗口配置
| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 右侧"窗口配置"→ 输入名称"4号窗口"→ 只勾选"售后"→ 点击"新增窗口" | Toast 提示"已新增窗口：4号窗口"，窗口列表新增 4 号，仅支持售后 |
| 2 | 对"4号窗口"点击"编辑"→ 改名为"VIP窗口"→ 勾选"咨询"+"售后"→ 保存 | 窗口名称变为"VIP窗口"，支持的业务类型变为咨询和售后 |
| 3 | 对"VIP窗口"点击"停用" | Toast 提示"窗口已停用"，窗口变灰，窗口操作面板中不再显示 |
| 4 | 对正在办理的窗口点击"停用" | Toast 提示"操作失败，窗口可能正在办理业务" |
| 5 | 刷新页面 | 窗口配置保持不变（从 counters.json 加载） |

#### 测试用例 5：多端实时同步
| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 浏览器 A 和浏览器 B 同时打开 http://localhost:5173 | 两者连接状态都是"在线"，队列状态一致 |
| 2 | 浏览器 A 取号"咨询" | 浏览器 B 等待队列立即出现新号码（< 1 秒延迟） |
| 3 | 浏览器 B 叫号 → 完成 | 浏览器 A 顶部叫号、窗口状态、统计数据同步更新 |

#### 测试用例 6：数据持久化与恢复
| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 取若干号，叫号完成几个，过号几个 | 确认队列状态 |
| 2 | 等待 30 秒或 Ctrl+C 停止后端 | ~/.queue-system/queue-data.json 存在，内容非空 |
| 3 | 重新启动后端 | 控制台输出"队列数据已恢复..."，队列状态与关闭前一致 |
| 4 | 重启前端 | 显示恢复后的队列状态 |

#### 测试用例 7：连接状态与错误处理
| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 正常启动，右上角显示"在线" | 绿色脉冲动画 |
| 2 | 停止后端服务 | 3 秒内右上角变为橙色"重连中"，闪烁动画 |
| 3 | 此时在前端点击"取号" | 消息暂存队列，不报错 |
| 4 | 重新启动后端 | 自动重连成功，待发送的取号自动执行，Toast 提示成功，队列中出现号码 |
| 5 | 未选择窗口直接点击"叫号" | Toast 红色提示"未选择窗口" |
| 6 | 等待队列空时点击"叫号" | Toast 红色提示"没有等待的号票或窗口忙" |

### 4. 响应式适配测试

| 设备 | 视口宽度 | 预期布局 |
|------|----------|----------|
| 电脑 | ≥ 1025px | 左右两栏布局：左侧统计+队列+过号，右侧取号+操作+配置 |
| iPad | 768-1024px | 右侧组件变为水平排列（取号、操作、配置并排） |
| 手机 | ≤ 640px | 全部垂直堆叠，单栏布局，Toast 占满宽度 |

### 测试失败排查

- **前端构建失败**：检查 Node.js 版本是否 ≥ 16，`npm install` 是否成功
- **后端打包失败**：检查 JDK 版本是否为 8，Maven 是否能下载依赖（可能需要配置国内镜像）
- **WebSocket 连接不上**：确认后端 8080 端口未被占用，防火墙是否放行
- **生产环境 HTTPS 连不上**：确保 `VITE_WS_URL` 用 `wss://` 开头，后端 WebSocket 需要配 SSL 或走反向代理
