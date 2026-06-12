package com.taskboard;

import io.javalin.Javalin;
import io.javalin.websocket.WsMessageContext;

public class AppServer {
    public static void main(String[] args) {
        TaskService taskService = new TaskService();
        FileStore fileStore = new FileStore(taskService);

        Javalin app = Javalin.create(config -> {
            config.enableCorsForAllOrigins();
        });

        app.ws("/ws", ws -> {
            TaskWebSocket tws = new TaskWebSocket(taskService);
            ws.onConnect(tws::onConnect);
            ws.onClose(tws::onClose);
            ws.onMessage((WsMessageContext ctx) -> {
                tws.onMessage(ctx.message());
            });
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            fileStore.shutdown();
            app.stop();
        }));

        app.start(8080);
        System.out.println("Server started on http://localhost:8080");
    }
}
