package start;

import adapter.ItemHttpAdapter;
import app.DeclutterService;
import com.sun.net.httpserver.HttpServer;
import persist.ItemJsonStore;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class DeclutterServer {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        String baseDir = new File("").getAbsolutePath();
        String dataDir = baseDir + File.separator + "data";
        String uiDir = baseDir + File.separator + "ui";
        String uploadDir = dataDir + File.separator + "uploads";

        System.out.println("========================================");
        System.out.println("  二手物品整理清单 - Declutter Server");
        System.out.println("========================================");
        System.out.println("工作目录:   " + baseDir);
        System.out.println("数据目录:   " + dataDir);
        System.out.println("图片目录:   " + uploadDir);
        System.out.println("UI目录:     " + uiDir);
        System.out.println("监听端口:   " + port);
        System.out.println("========================================");

        ItemJsonStore store = new ItemJsonStore(dataDir);
        DeclutterService service = new DeclutterService(store);
        ItemHttpAdapter adapter = new ItemHttpAdapter(service, uiDir, uploadDir);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", adapter);
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println();
        System.out.println("服务器已启动！请在浏览器中访问:");
        System.out.println("  http://localhost:" + port + "/");
        System.out.println();
        System.out.println("按 Ctrl+C 停止服务器。");
    }
}
