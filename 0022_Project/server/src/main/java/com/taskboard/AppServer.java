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
            ws.onConnect(ctx -> {
                TaskWebSocket tws = new TaskWebSocket(taskService, ctx);
                ctx.attribute("tws", tws);
                tws.onConnect();
            });
            ws.onClose(ctx -> {
                TaskWebSocket tws = ctx.attribute("tws");
                if (tws != null) tws.onClose();
            });
            ws.onMessage(ctx -> {
                TaskWebSocket tws = ctx.attribute("tws");
                if (tws != null) tws.onMessage(ctx.message());
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
