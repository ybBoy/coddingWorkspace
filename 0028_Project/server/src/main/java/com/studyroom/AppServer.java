package com.studyroom;

import com.studyroom.service.SeatService;
import com.studyroom.store.JsonStore;
import com.studyroom.ws.SeatSocket;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;

public class AppServer {
    private static final int HTTP_PORT = 8080;
    private static final int WS_PORT = 8081;

    private static Path resolveStaticDir() {
        String[] candidates = {
            System.getProperty("studyroom.staticDir", ""),
            "client/dist",
            "../client/dist",
            "../../client/dist",
        };
        for (String c : candidates) {
            if (c == null || c.isEmpty()) continue;
            Path p = Paths.get(c).toAbsolutePath().normalize();
            if (Files.isDirectory(p)) {
                System.out.println("Static dir: " + p);
                return p;
            }
        }
        return Paths.get("client/dist").toAbsolutePath().normalize();
    }

    public static void main(String[] args) throws Exception {
        JsonStore store = new JsonStore();
        SeatService seatService = new SeatService(store);
        seatService.startAutoSave(30);

        SeatSocket wsServer = new SeatSocket(new InetSocketAddress(WS_PORT), seatService);
        wsServer.start();

        Thread checker = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000);
                    seatService.checkReleasable();
                    wsServer.broadcastUpdate();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "releasable-checker");
        checker.setDaemon(true);
        checker.start();

        final Path staticDir = resolveStaticDir();
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
        httpServer.createContext("/", exchange -> {
            String requestPath = exchange.getRequestURI().getPath();
            if ("/".equals(requestPath)) requestPath = "/index.html";

            if (requestPath.startsWith("/")) requestPath = requestPath.substring(1);
            Path filePath = staticDir.resolve(requestPath).normalize();
            if (!filePath.startsWith(staticDir) || !Files.exists(filePath) || Files.isDirectory(filePath)) {
                filePath = staticDir.resolve("index.html");
            }

            if (!Files.exists(filePath)) {
                String msg = "Not Found";
                exchange.sendResponseHeaders(404, msg.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(msg.getBytes());
                }
                return;
            }

            String mime = guessMime(filePath.toString());
            exchange.getResponseHeaders().set("Content-Type", mime);
            byte[] content = Files.readAllBytes(filePath);
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
        });

        httpServer.setExecutor(null);
        httpServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            httpServer.stop(0);
            try { wsServer.stop(0); } catch (Exception ignored) {}
            seatService.shutdown();
        }));

        System.out.println("===========================================");
        System.out.println("  自习室座位看板已启动");
        System.out.println("  HTTP: http://localhost:" + HTTP_PORT);
        System.out.println("  WebSocket: ws://localhost:" + WS_PORT);
        System.out.println("===========================================");
    }

    private static String guessMime(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        if (path.endsWith(".json")) return "application/json; charset=utf-8";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".ico")) return "image/x-icon";
        if (path.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}
