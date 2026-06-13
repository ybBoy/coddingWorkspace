package boot;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import service.PetCareService;
import store.JsonStore;
import ws.PetSocket;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.ServerContainer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class AppServer {
    private static int PORT = 8080;
    private static PetCareService petCareService;
    private static Server server;

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            try {
                PORT = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port argument, using default 8080");
            }
        }
        String portEnv = System.getenv("PETBOARD_PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                PORT = Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        String portProp = System.getProperty("petboard.port");
        if (portProp != null && !portProp.isEmpty()) {
            try {
                PORT = Integer.parseInt(portProp);
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        JsonStore jsonStore = new JsonStore();
        petCareService = new PetCareService(jsonStore);
        PetSocket.setPetCareService(petCareService);

        server = new Server(PORT);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        context.addServlet(new ServletHolder(new StaticFileServlet()), "/");

        server.setHandler(context);

        ServerContainer wsContainer = WebSocketServerContainerInitializer.configureContext(context);
        wsContainer.addEndpoint(PetSocket.class);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Shutting down server...");
                if (petCareService != null) {
                    petCareService.shutdown();
                }
                if (server != null) {
                    try {
                        server.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("Server stopped.");
            }
        });

        server.start();
        System.out.println("Pet Care Board server started on port " + PORT);
        server.join();
    }

    public static class StaticFileServlet extends HttpServlet {
        private static final String[] CLIENT_DIRS = new String[] {
            "webapp",
            "client/dist",
            "../client/dist"
        };
        private File resolvedBaseDir;

        private File resolveBaseDir() {
            for (String candidate : CLIENT_DIRS) {
                File dir = new File(candidate);
                if (dir.exists() && dir.isDirectory() && new File(dir, "index.html").exists()) {
                    return dir;
                }
            }
            return null;
        }

        private File getBaseDir() {
            if (resolvedBaseDir == null) {
                resolvedBaseDir = resolveBaseDir();
            }
            return resolvedBaseDir;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            File baseDir = getBaseDir();
            if (baseDir == null) {
                resp.setContentType("text/html; charset=utf-8");
                resp.getWriter().write(
                    "<!DOCTYPE html><html><head><meta charset='utf-8'><title>萌宠寄养看板</title>" +
                    "<style>body{font-family:system-ui;display:flex;align-items:center;justify-content:center;" +
                    "min-height:100vh;background:#faf8f5;color:#3d3d3d;margin:0}" +
                    ".box{background:#fff;padding:32px 40px;border-radius:16px;box-shadow:0 4px 12px rgba(0,0,0,.06);text-align:center}" +
                    "h2{color:#4a8a92;margin:0 0 12px}p{margin:4px 0;color:#7a7a7a;font-size:14px}" +
                    ".emoji{font-size:48px;margin-bottom:16px}</style></head><body>" +
                    "<div class='box'><div class='emoji'>🐾</div><h2>前端未构建</h2>" +
                    "<p>请先运行 <code style='background:#f0ede8;padding:2px 6px;border-radius:4px'>cd client &amp;&amp; npm install &amp;&amp; npm run build</code></p>" +
                    "<p>或使用开发模式：<code style='background:#f0ede8;padding:2px 6px;border-radius:4px'>cd client &amp;&amp; npm run dev</code> （访问 http://localhost:5173）</p>" +
                    "</div></body></html>"
                );
                return;
            }

            String path = req.getPathInfo();
            if (path == null || path.equals("/")) {
                path = "/index.html";
            }

            File file = new File(baseDir, path);
            if (!file.exists() || file.isDirectory()) {
                file = new File(baseDir, "index.html");
            }

            if (file.exists()) {
                String contentType = getContentType(file.getName());
                resp.setContentType(contentType);
                Files.copy(file.toPath(), resp.getOutputStream());
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("404 Not Found");
            }
        }

        private String getContentType(String fileName) {
            if (fileName.endsWith(".html")) return "text/html; charset=utf-8";
            if (fileName.endsWith(".js")) return "application/javascript; charset=utf-8";
            if (fileName.endsWith(".css")) return "text/css; charset=utf-8";
            if (fileName.endsWith(".json")) return "application/json; charset=utf-8";
            if (fileName.endsWith(".png")) return "image/png";
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
            if (fileName.endsWith(".svg")) return "image/svg+xml";
            if (fileName.endsWith(".ico")) return "image/x-icon";
            return "application/octet-stream";
        }
    }
}
