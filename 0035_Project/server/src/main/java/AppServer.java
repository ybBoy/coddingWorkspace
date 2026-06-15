import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import controller.MovieController;
import service.MovieService;
import store.JsonMovieStore;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppServer {
    private static final int PORT = 9000;
    private static final String BASE_DIR = System.getProperty("user.dir");
    private static final String WEB_DIR = BASE_DIR.endsWith("server") ? "../web" : "web";
    private static final String DATA_FILE = BASE_DIR.endsWith("server") ? "../data/movies.json" : "data/movies.json";

    public static void main(String[] args) throws IOException {
        JsonMovieStore store = new JsonMovieStore(DATA_FILE);
        MovieService service = new MovieService(store);
        final MovieController controller = new MovieController(service);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();

                if ("OPTIONS".equalsIgnoreCase(method)) {
                    handleCors(exchange);
                    return;
                }

                if (path.startsWith("/api/")) {
                    controller.handleRequest(exchange);
                } else {
                    serveStaticFile(exchange, path);
                }
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("服务器已启动: http://localhost:" + PORT);
        System.out.println("打开浏览器访问: http://localhost:" + PORT + "/");
        System.out.println("按 Ctrl+C 停止服务");
    }

    private static void handleCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(204, -1);
    }

    private static void serveStaticFile(HttpExchange exchange, String path) throws IOException {
        if ("/".equals(path)) {
            path = "/index.html";
        }

        Path filePath = Paths.get(WEB_DIR, path.substring(1));
        File file = filePath.toFile();

        if (!file.exists() || file.isDirectory()) {
            sendError(exchange, 404, "File Not Found");
            return;
        }

        String contentType = getContentType(file.getName());
        byte[] content = Files.readAllBytes(filePath);

        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, content.length);
        OutputStream os = exchange.getResponseBody();
        os.write(content);
        os.close();
    }

    private static String getContentType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".js")) return "application/javascript";
        if (fileName.endsWith(".json")) return "application/json";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".svg")) return "image/svg+xml";
        if (fileName.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

    private static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
