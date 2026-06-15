package http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class StaticFileHandler implements HttpHandler {
    private final String staticDir;
    private final String canonicalStaticDir;

    public StaticFileHandler(String staticDir) {
        this.staticDir = staticDir;
        String canonical = staticDir;
        try {
            canonical = new File(staticDir).getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.canonicalStaticDir = canonical;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if ("/".equals(path)) {
            path = "/index.html";
        }

        path = URLDecoder.decode(path, "UTF-8");

        if (path.contains("..")) {
            sendResponse(exchange, 403, "text/plain", "403 Forbidden");
            return;
        }

        File file = new File(staticDir, path);
        String canonicalPath = file.getCanonicalPath();

        if (!canonicalPath.startsWith(canonicalStaticDir + File.separator)
                && !canonicalPath.equals(canonicalStaticDir)) {
            sendResponse(exchange, 403, "text/plain", "403 Forbidden");
            return;
        }

        if (!file.exists() || !file.isFile()) {
            sendResponse(exchange, 404, "text/plain", "404 Not Found");
            return;
        }

        String contentType = getContentType(file.getName());
        exchange.getResponseHeaders().set("Content-Type", contentType);

        byte[] content = Files.readAllBytes(file.toPath());
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
        }
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        } else if (fileName.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        } else if (fileName.endsWith(".json")) {
            return "application/json; charset=UTF-8";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else {
            return "application/octet-stream";
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String contentType, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
