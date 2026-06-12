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
- EventBus 组件间通信
- WebSocket 实时通信

### 后端
- Java 8
- Java-WebSocket 库
- Jackson JSON 序列化
- 内存存储 + JSON 文件持久化

## 项目结构

```
0020_Project/
├── client/                          # 前端源码
│   ├── App.tsx                      # 主组件，事件中转
│   ├── QueueBoard.tsx               # 队列展示组件（顶部+左侧+底部）
│   ├── CounterPanel.tsx             # 窗口操作面板组件
│   ├── TicketForm.tsx               # 取号表单组件
│   ├── EventBus.ts                  # 事件总线
│   ├── types.ts                     # TypeScript 类型定义
│   ├── websocket.ts                 # WebSocket 连接管理
│   ├── main.tsx                     # React 入口
│   └── styles.css                   # 全局样式
├── server/                          # 后端源码
│   ├── pom.xml                      # Maven 配置
│   └── src/main/java/com/queue/
│       ├── AppServer.java           # 主服务器启动类
│       ├── QueueWebSocket.java      # WebSocket 处理类
│       ├── QueueService.java        # 业务逻辑层
│       ├── FileStore.java           # 文件持久化模块
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
| [App.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/App.tsx) | 主组件，初始化 WebSocket，订阅 EventBus 事件并转发到后端，整合所有子组件 |
| [QueueBoard.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/QueueBoard.tsx) | 队列展示：顶部当前叫号、左侧等待队列、底部叫号记录 |
| [CounterPanel.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/CounterPanel.tsx) | 窗口操作：选择窗口、叫号、完成、过号、重新叫号 |
| [TicketForm.tsx](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/TicketForm.tsx) | 取号表单：选择业务类型后取号 |
| [EventBus.ts](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/EventBus.ts) | 事件总线：发布-订阅模式，组件间通信，不使用全局变量 |
| [types.ts](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/types.ts) | TypeScript 类型定义：号票、窗口、队列状态、WebSocket 消息等 |
| [websocket.ts](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/websocket.ts) | WebSocket 连接管理：连接、消息收发、断线重连 |
| [styles.css](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/client/styles.css) | 全局样式：响应式布局、颜色主题 |

### 后端文件

| 文件 | 职责 |
|------|------|
| [AppServer.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/AppServer.java) | 主启动类：初始化各模块、启动 WebSocket 服务器、注册关闭钩子 |
| [QueueWebSocket.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/QueueWebSocket.java) | WebSocket 处理：连接管理、消息解析、状态广播 |
| [QueueService.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/QueueService.java) | 业务逻辑：取号、叫号、完成、过号、重新叫号，管理内存队列 |
| [FileStore.java](file:///e:/work/SOLO%20Coder/solo_project/0020_Project/server/src/main/java/com/queue/FileStore.java) | 文件持久化：定时保存队列数据到 JSON，启动时恢复 |
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
App.tsx 订阅事件
    ↓
WebSocket.send() 发送到 Java 后端
    (ws://localhost:8080/ws)
    ↓
QueueWebSocket 接收并解析消息
    ↓
QueueService 更新内存队列
    (synchronized 保证线程安全)
    ↓
FileStore 定时保存到本地 JSON 文件
    (queue-data.json，每 30 秒)
    ↓
QueueWebSocket 广播 STATE_UPDATE
    给所有连接的前端
    ↓
前端 websocket.ts 接收消息
    ↓
EventBus 发布 QUEUE_STATE_UPDATED
    ↓
各组件订阅事件更新 UI
    (QueueBoard / CounterPanel)
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

后端服务将在 `ws://localhost:8080/ws` 启动。

数据文件保存在 `server/queue-data.json`。

### 启动前端

```bash
npm install
npm run dev
```

前端开发服务器将在 `http://localhost:5173` 启动。

Vite 已配置代理，将 `/ws` 路径转发到后端 `ws://localhost:8080`。

### 生产构建

```bash
npm run build
```

构建产物在 `dist` 目录，可以部署到任意静态文件服务器。

## 使用说明

### 用户取号
1. 打开网页
2. 在右侧"取号"面板选择业务类型（咨询/办理/售后）
3. 点击"取号"按钮
4. 号码会出现在左侧等待队列中

### 工作人员操作
1. 在右侧"窗口操作"面板选择一个窗口
2. 点击"叫下一个号"呼叫等待队列中的第一位
3. 办理完成后点击"完成"
4. 如果用户不在，点击"过号"
5. 需要重新呼叫当前号码时点击"重新叫号"

### 查看状态
- 顶部：显示当前正在叫号的号码和窗口
- 左侧：显示等待队列，按取号时间排序
- 底部：显示最近 10 条叫号记录

## 界面配色

- 主色：蓝色 `#2563eb` - 当前叫号、叫号操作
- 完成：绿色 `#16a34a` - 完成操作、完成记录
- 过号：橙色 `#f97316` - 过号操作、过号记录
- 背景：浅色 `#f8fafc` - 页面背景

## 注意事项

1. 数据保存在内存中，定时（30秒）持久化到 `queue-data.json`
2. 服务重启后会自动从 `queue-data.json` 恢复数据
3. 默认有 3 个窗口（1号、2号、3号），可在 `QueueService.initCounters()` 修改
4. 叫号记录最多保留 50 条，前端显示最近 10 条
5. 多个浏览器同时打开时，所有操作都会实时同步
