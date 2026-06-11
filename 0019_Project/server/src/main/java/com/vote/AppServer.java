package com.vote;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import javax.servlet.annotation.WebServlet;

/**
 * AppServer 职责：
 * - 启动内嵌 Jetty 服务器（默认 8080 端口）
 * - 注册 WebSocket 端点到 /ws 路径
 * - 同时提供静态资源服务（可选，直接打开前端 dist 也可）
 * - 集成 FileStore、VoteService、VoteWebSocket
 *
 * 运行方式：
 *   mvn package exec:java  或者打包后 java -jar xxx.jar
 */
public class AppServer {
    private static final int PORT = 8080;
    private static final String DATA_FILE = "vote-data.json";

    public static void main(String[] args) throws Exception {
        // 初始化存储与服务
        FileStore fileStore = new FileStore(DATA_FILE);
        VoteService voteService = new VoteService(fileStore);
        VoteWebSocket.setVoteService(voteService);

        // 注册 JVM 关闭钩子，确保数据落盘
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("正在保存数据并退出...");
                voteService.shutdown();
            }
        }));

        // Jetty 服务器
        Server server = new Server(PORT);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        // WebSocket 配置：通过 WebSocketServlet 注册
        context.addServlet(new ServletHolder(new VoteServlet()), "/ws/*");

        // 静态资源（可选，指向前端 dist 目录）
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase("client-dist");
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});

        HandlerList handlers = new HandlerList();
        handlers.addHandler(resourceHandler);
        handlers.addHandler(context);
        server.setHandler(handlers);

        server.start();
        System.out.println("投票后端已启动: http://localhost:" + PORT);
        System.out.println("WebSocket 端点: ws://localhost:" + PORT + "/ws");
        server.join();
    }

    /**
     * 将 VoteWebSocket 注册为 Servlet
     */
    @WebServlet(name = "VoteWebSocketServlet", urlPatterns = {"/ws/*"}, asyncSupported = true)
    public static class VoteServlet extends WebSocketServlet {
        @Override
        public void configure(WebSocketServletFactory factory) {
            factory.register(VoteWebSocket.class);
        }
    }
}
