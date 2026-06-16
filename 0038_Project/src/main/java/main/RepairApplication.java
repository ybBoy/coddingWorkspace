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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class RepairApplication {
    private static final String STATIC_DIR = "static";
    private static final String UPLOAD_DIR = "uploads";
    private static final int DEFAULT_PORT = 8081;
    private static final int MAX_PORT_ATTEMPTS = 10;

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get(UPLOAD_DIR));

        int port = DEFAULT_PORT;
        if (args != null && args.length > 0) {
            try {
                port = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException e) {
                System.err.println("Invalid port argument '" + args[0] + "', using default " + DEFAULT_PORT);
                port = DEFAULT_PORT;
            }
        }

        String envPort = System.getenv("HOME_REPAIR_PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        RepairManager manager = new RepairManager();

        HttpServer server = null;
        int boundPort = -1;
        for (int attempt = 0; attempt < MAX_PORT_ATTEMPTS; attempt++) {
            try {
                int candidate = port + attempt;
                server = HttpServer.create(new InetSocketAddress(candidate), 0);
                boundPort = candidate;
                break;
            } catch (java.net.BindException e) {
                System.out.println("端口 " + (port + attempt) + " 被占用，尝试下一个...");
            }
        }
        if (server == null) {
            throw new IOException("无法绑定端口，已尝试 " + MAX_PORT_ATTEMPTS + " 个端口");
        }

        server.createContext("/api", new RepairApi(manager));
        server.createContext("/uploads", new FileHandler(UPLOAD_DIR, "/uploads",
                Arrays.asList("jpg", "jpeg", "png", "gif", "webp")));
        server.createContext("/", new FileHandler(STATIC_DIR, "/",
                Arrays.asList("html", "css", "js", "json", "png", "jpg", "jpeg",
                        "gif", "svg", "ico", "woff", "woff2", "ttf")));

        server.setExecutor(null);
        server.start();

        System.out.println("========================================");
        System.out.println("  家庭维修记录本服务已启动");
        System.out.println("  访问地址: http://localhost:" + boundPort + "/home.html");
        System.out.println("  按 Ctrl+C 停止服务");
        System.out.println("========================================");
    }

    static class FileHandler implements HttpHandler {
        private final Path rootPath;
        private final String contextPrefix;
        private final List<String> allowedExts;

        FileHandler(String dir, String contextPrefix, List<String> allowedExts) {
            this.rootPath = Paths.get(dir).toAbsolutePath().normalize();
            this.contextPrefix = contextPrefix == null ? "/" : contextPrefix;
            this.allowedExts = allowedExts;
            try {
                Files.createDirectories(this.rootPath);
            } catch (IOException e) {
                System.err.println("Failed to create directory " + dir + ": " + e.getMessage());
            }
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
            if ("/".equals(requestPath) && "/".equals(contextPrefix)) {
                requestPath = "/home.html";
            }

            if (!contextPrefix.equals("/") && requestPath.startsWith(contextPrefix)) {
                String stripped = requestPath.substring(contextPrefix.length());
                requestPath = stripped.startsWith("/") ? stripped : ("/" + stripped);
            }

            if (requestPath.contains("..") || requestPath.contains("\\0")) {
                sendForbidden(exchange, "Forbidden");
                return;
            }

            File file;
            try {
                String relative = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
                if (relative.isEmpty()) {
                    relative = "home.html";
                }
                Path filePath = rootPath.resolve(relative).normalize();
                if (!filePath.startsWith(rootPath)) {
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
            for (String ext : allowedExts) {
                if (lower.endsWith("." + ext)) return true;
            }
            return false;
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
            if (lower.endsWith(".webp")) return "image/webp";
            if (lower.endsWith(".ico")) return "image/x-icon";
            if (lower.endsWith(".woff")) return "font/woff";
            if (lower.endsWith(".woff2")) return "font/woff2";
            if (lower.endsWith(".ttf")) return "font/ttf";
            return "application/octet-stream";
        }
    }
}
