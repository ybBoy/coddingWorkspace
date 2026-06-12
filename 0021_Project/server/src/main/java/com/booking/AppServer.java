package com.booking;

import com.booking.service.BookingService;
import com.booking.store.FileStore;
import com.booking.websocket.BookingWebSocket;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.websocket.server.ServerContainer;

/**
 * AppServer 应用服务器启动类
 * 职责：
 *   1. 启动嵌入式 Jetty 服务器
 *   2. 配置 WebSocket endpoint
 *   3. 初始化 BookingService 和 FileStore
 *   4. 启动定时数据保存
 *   5. 注册 JVM 关闭钩子，确保退出时保存数据
 *
 * 服务端口：8080
 * WebSocket 路径：/ws
 *
 * 启动方式：
 *   - 开发：在 server 目录下运行 mvn compile exec:java -Dexec.mainClass="com.booking.AppServer"
 *   - 打包：mvn package 后运行 java -jar target/booking-server.jar
 */
public class AppServer {

    private static final int PORT = 8080;
    private static BookingService bookingService;
    private static FileStore fileStore;

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("  实时预约签到系统 - 后端服务");
        System.out.println("========================================");

        // 1. 初始化业务服务
        bookingService = new BookingService();
        fileStore = new FileStore(bookingService);

        // 2. 尝试从文件加载数据，如果没有则初始化默认场次
        boolean loaded = fileStore.load();
        if (!loaded) {
            bookingService.initDefaultSessions();
            System.out.println("[AppServer] 已初始化默认场次数据");
        }

        // 3. 注入服务到 WebSocket
        BookingWebSocket.setServices(bookingService, fileStore);

        // 4. 启动 Jetty 服务器
        Server server = new Server(PORT);

        // 配置 Servlet 上下文
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // 启用 JSR 356 WebSocket 支持
        ServerContainer wsContainer = org.eclipse.jetty.websocket.jsr356.server.deploy
                .WebSocketServerContainerInitializer.configureContext(context);

        // 注册 WebSocket endpoint
        wsContainer.addEndpoint(BookingWebSocket.class);

        // 5. 启动定时保存
        fileStore.startAutoSave();

        // 6. 注册 JVM 关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("\n[AppServer] 正在关闭服务...");
                try {
                    if (fileStore != null) {
                        fileStore.stopAutoSave();
                        fileStore.save();
                        System.out.println("[AppServer] 数据已保存");
                    }
                } catch (Exception e) {
                    System.err.println("[AppServer] 关闭时保存数据失败: " + e.getMessage());
                }
            }
        }));

        // 7. 启动服务器
        server.start();
        System.out.println("[AppServer] 服务器已启动，端口: " + PORT);
        System.out.println("[AppServer] WebSocket 地址: ws://localhost:" + PORT + "/ws");
        System.out.println("[AppServer] 按 Ctrl+C 停止服务");

        server.join();
    }

    /**
     * 获取 BookingService 实例（供测试或其他类使用）
     */
    public static BookingService getBookingService() {
        return bookingService;
    }
}
