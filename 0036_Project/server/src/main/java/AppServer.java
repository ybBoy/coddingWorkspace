import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import controller.RecipeController;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AppServer {
    private static final int PORT = 8080;
    private static final String WEB_DIR = "web";

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        RecipeController controller = new RecipeController();

        server.createContext("/api", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (exchange.getRequestMethod().equals("OPTIONS")) {
                    controller.handleOptions(exchange);
                } else {
                    controller.handleRequest(exchange);
                }
            }
        });

        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/")) {
                    path = "/index.html";
                }
                String filePath = WEB_DIR + path;
                File file = new File(filePath);
                if (file.exists() && file.isFile()) {
                    byte[] content = Files.readAllBytes(Paths.get(filePath));
                    String contentType = getContentType(filePath);
                    exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
                    exchange.sendResponseHeaders(200, content.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(content);
                    os.close();
                } else {
                    String response = "404 Not Found";
                    exchange.sendResponseHeaders(404, response.getBytes(StandardCharsets.UTF_8).length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                    os.close();
                }
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Server started on http://localhost:" + PORT);
    }

    private static String getContentType(String filePath) {
        if (filePath.endsWith(".html")) {
            return "text/html";
        } else if (filePath.endsWith(".css")) {
            return "text/css";
        } else if (filePath.endsWith(".js")) {
            return "application/javascript";
        } else if (filePath.endsWith(".json")) {
            return "application/json";
        } else if (filePath.endsWith(".png")) {
            return "image/png";
        } else if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }
}
