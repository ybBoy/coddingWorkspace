package main;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import core.RepairManager;
import web.RepairApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RepairApplication {
    private static final int PORT = 8081;
    private static final String STATIC_DIR = "static";

    public static void main(String[] args) throws IOException {
        RepairManager manager = new RepairManager();
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/api", new RepairApi(manager));
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("========================================");
        System.out.println("  家庭维修记录本服务已启动");
        System.out.println("  访问地址: http://localhost:" + PORT + "/home.html");
        System.out.println("  按 Ctrl+C 停止服务");
        System.out.println("========================================");
    }

    static class StaticFileHandler implements HttpHandler {
        private final Path staticRootPath;

        StaticFileHandler() {
            this.staticRootPath = Paths.get(STATIC_DIR).toAbsolutePath().normalize();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            String method = exchange.getRequestMethod();
            if (!"GET".equals(method) && !"HEAD".equals(method)) {
                sendForbidden(exchange, "Method Not Allowed");
                return;
            }

            String requestPath = exchange.getRequestURI().getPath();
            if ("/".equals(requestPath)) {
                requestPath = "/home.html";
            }

            if (requestPath.contains("..") || requestPath.contains("\\0")) {
                sendForbidden(exchange, "Forbidden");
                return;
            }

            File file;
            try {
                Path filePath = staticRootPath.resolve(requestPath.substring(1)).normalize();
                if (!filePath.startsWith(staticRootPath)) {
                    sendForbidden(exchange, "Forbidden");
                    return;
                }
                file = filePath.toFile();
            } catch (Exception e) {
                sendForbidden(exchange, "Forbidden");
                return;
            }

            if (!file.exists() || !file.isFile()) {
                String notFound = "<html><body><h1>404 - 文件不存在</h1><p>请访问 <a href='/home.html'>/home.html</a></p></body></html>";
                exchange.sendResponseHeaders(404, notFound.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes(StandardCharsets.UTF_8));
                }
                return;
            }

            if (!isAllowedExtension(file.getName())) {
                sendForbidden(exchange, "Forbidden");
                return;
            }

            String contentType = getContentType(file.getName());
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
            exchange.sendResponseHeaders(200, file.length());

            if ("HEAD".equals(method)) {
                exchange.close();
                return;
            }

            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = exchange.getResponseBody()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = fis.read(buffer)) > 0) {
                    os.write(buffer, 0, count);
                }
            }
        }

        private boolean isAllowedExtension(String filename) {
            String lower = filename.toLowerCase();
            return lower.endsWith(".html")
                    || lower.endsWith(".css")
                    || lower.endsWith(".js")
                    || lower.endsWith(".json")
                    || lower.endsWith(".png")
                    || lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg")
                    || lower.endsWith(".gif")
                    || lower.endsWith(".svg")
                    || lower.endsWith(".ico")
                    || lower.endsWith(".woff")
                    || lower.endsWith(".woff2")
                    || lower.endsWith(".ttf");
        }

        private void sendForbidden(HttpExchange exchange, String message) throws IOException {
            String body = "<html><body><h1>403 - " + message + "</h1></body></html>";
            exchange.sendResponseHeaders(403, body.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        private String getContentType(String filename) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
            if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
            if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".gif")) return "image/gif";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".ico")) return "image/x-icon";
            if (lower.endsWith(".woff")) return "font/woff";
            if (lower.endsWith(".woff2")) return "font/woff2";
            if (lower.endsWith(".ttf")) return "font/ttf";
            return "application/octet-stream";
        }
    }
}
