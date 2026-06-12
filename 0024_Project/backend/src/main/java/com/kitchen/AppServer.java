package com.kitchen;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.eclipse.jetty.util.resource.PathResource;

import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 后端启动入口（main 方法在这里）
 * 职责：
 *  1. 启动一个嵌入式 Jetty（HTTP + WebSocket）
 *  2. 装配各个模块：FileStore -> OrderService -> KitchenSocket
 *  3. 启动时从 JSON 恢复未完成订单
 *  4. 提供静态资源（前端 build 后的 dist/ 目录）
 *  5. 暴露 WebSocket 端点 /ws
 *
 * 调用顺序:
 *   AppServer (main)
 *     ├─ new FileStore()
 *     ├─ new OrderService()      <── restoreFrom(fileStore.load())
 *     ├─ KitchenSocket.setOrderService()
 *     ├─ fileStore.startAutoSave(orderService)
 *     └─ start Jetty:
 *         ├─ HTTP/8080 静态资源（前端页面）
 *         └─ WS /ws   （实时通信
 */
public class AppServer {
    public static void main(String[] args) throws Exception {
        // ---- 1. 工作目录：取 backend/ 作为根（data/、dist/ 都在这个目录下
        String baseDir = new File(AppServer.class.getProtectionDomain().getCodeSource().getLocation().getPath())
                .getParentFile().getParentFile().getParentFile().getParentFile().getParent();
        // 上面在 IDE/打包后路径不一，回退到 backend/
        // 简单起见：从系统属性或默认值：
        String workingDir = System.getProperty("app.baseDir", baseDir);
        Path basePath = Paths.get(workingDir).toAbsolutePath();
        System.out.println("[App] baseDir = " + basePath);

        // ---- 2. 初始化业务层
        FileStore fileStore = new FileStore(basePath.toString());
        OrderService orderService = new OrderService();
        orderService.restoreFrom(fileStore.load());
        KitchenSocket.setOrderService(orderService);
        KitchenSocket.startHeartBeat();
        fileStore.startAutoSave(orderService);

        // ---- 3. 启动 Jetty
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(Integer.getInteger("app.port", 8080));
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        // WebSocket
        ServerContainer wsContainer = WebSocketServerContainerInitializer.configureContext(context);
        wsContainer.addEndpoint(ServerEndpointConfig.Builder
                .create(KitchenSocket.class, "/ws")
                .build());

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
                fileStore.save(orderService.listPendingOrders());
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
