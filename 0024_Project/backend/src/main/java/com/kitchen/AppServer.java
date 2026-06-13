package com.kitchen;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.eclipse.jetty.util.resource.PathResource;

import javax.websocket.server.ServerContainer;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 后端启动入口（main 方法在这里）
 * 职责：
 *  1. 启动一个嵌入式 Jetty（HTTP + WebSocket）
 *  2. 装配各个模块：FileStore -> OrderService/MenuService -> KitchenSocket
 *  3. 启动时从 JSON 恢复未完成订单 + 菜单
 *  4. 提供静态资源（前端 build 后的 dist/ 目录）
 *  5. 暴露 WebSocket 端点 /ws
 *
 * 调用顺序:
 *   AppServer (main)
 *     ├─ new FileStore()
 *     ├─ new OrderService()  ←─ restoreFrom(fileStore.loadOrders())
 *     ├─ new MenuService()   ←─ restoreFrom(fileStore.loadMenu())
 *     ├─ KitchenSocket.setOrderService() / setMenuService()
 *     ├─ fileStore.startAutoSave(orderService, menuService)
 *     └─ start Jetty:
 *         ├─ HTTP/8080 静态资源（前端页面）
 *         └─ WS /ws   （实时通信）
 */
public class AppServer {
    public static void main(String[] args) throws Exception {
        // ---- 1. 工作目录推算（健壮版）
        // 优先级：
        //   ① 命令行指定：java -Dapp.baseDir=./backend -jar xxx.jar
        //   ② JVM 当前工作目录（System.getProperty("user.dir")，即执行命令时所在目录
        //   ③ 从 class 路径回退（兜底，尽量避免走到这一步）
        String workingDir = System.getProperty("app.baseDir");
        if (workingDir == null || workingDir.isEmpty()) {
            workingDir = System.getProperty("user.dir");
            // 如果 user.dir 是项目根目录（含 frontend/backend 子目录），则自动切到 backend/
            Path userDir = Paths.get(workingDir).toAbsolutePath();
            if (java.nio.file.Files.isDirectory(userDir.resolve("backend"))) {
                workingDir = userDir.resolve("backend").toString();
            }
        }
        Path basePath = Paths.get(workingDir).toAbsolutePath();
        System.out.println("[App] baseDir = " + basePath);
        // 校验关键目录是否存在，找不到就给出明确提示
        if (!java.nio.file.Files.isDirectory(basePath.resolve("data"))) {
            System.out.println("[Warn] data/ 目录不存在，自动创建: " + basePath.resolve("data"));
            java.nio.file.Files.createDirectories(basePath.resolve("data"));
        }

        // ---- 2. 初始化业务层
        FileStore fileStore = new FileStore(basePath.toString());

        OrderService orderService = new OrderService();
        orderService.restoreFrom(fileStore.loadOrders());

        MenuService menuService = new MenuService();
        menuService.restoreFrom(fileStore.loadMenu());

        // 双向关联：下单时根据菜单分配工位
        orderService.setMenuService(menuService);

        KitchenSocket.setOrderService(orderService);
        KitchenSocket.setMenuService(menuService);
        KitchenSocket.setFileStore(fileStore);  // 历史查询需要读归档
        KitchenSocket.startHeartBeat();

        fileStore.startAutoSave(orderService, menuService);

        // ---- 3. 启动 Jetty
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(Integer.getInteger("app.port", 8080));
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        // WebSocket - KitchenSocket 已用 @ServerEndpoint("/ws") 标注，直接注册类即可
        ServerContainer wsContainer = WebSocketServerContainerInitializer.configureContext(context);
        wsContainer.addEndpoint(KitchenSocket.class);

        // 静态资源：优先 backend/dist/（前端 build 产物），其次 backend/static/
        ResourceHandler rh = new ResourceHandler();
        rh.setDirectoriesListed(false);
        Path dist = basePath.resolve("dist");
        Path staticDir = basePath.resolve("static");
        if (java.nio.file.Files.isDirectory(dist)) {
            rh.setBaseResource(new PathResource(dist));
        } else {
            rh.setBaseResource(new PathResource(staticDir));
        }

        HandlerList handlers = new HandlerList();
        handlers.addHandler(rh);
        handlers.addHandler(context);
        server.setHandler(handlers);

        // 优雅停机钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("[App] saving before exit...");
                // 停机前把所有（包括<24h的）DONE/CANCELLED 订单都归档一次
                // 把内存里剩下的 DONE/CANCELLED 也归档
                java.util.List<Order> remaining = new java.util.ArrayList<>();
                for (Order o : orderService.listAll()) {
                    if (o.getStatus() == Order.Status.DONE || o.getStatus() == Order.Status.CANCELLED) {
                        remaining.add(o);
                    }
                }
                if (!remaining.isEmpty()) fileStore.appendDailyArchive(remaining);
                fileStore.saveOrders(orderService.listPendingOrders());
                fileStore.saveMenu(menuService.listAll());
                fileStore.shutdown();
                server.stop();
            } catch (Exception e) { e.printStackTrace(); }
        }));

        server.start();
        System.out.println("=======================================");
        System.out.println("  Kitchen Display Server started!");
        System.out.println("  HTTP: http://localhost:8080");
        System.out.println("  WS  : ws://localhost:8080/ws");
        System.out.println("=======================================");
        server.join();
    }
}
