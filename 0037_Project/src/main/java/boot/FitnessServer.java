package boot;

import com.sun.net.httpserver.HttpServer;
import http.CheckinHandler;
import http.StaticFileHandler;
import service.FitnessService;
import storage.CheckinFileStore;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class FitnessServer {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try {
            String projectDir = new File("").getAbsolutePath();
            String staticDir = projectDir + File.separator + "public";
            String dataFile = projectDir + File.separator + "data" + File.separator + "checkins.json";

            System.out.println("项目目录: " + projectDir);
            System.out.println("静态文件目录: " + staticDir);
            System.out.println("数据文件: " + dataFile);

            CheckinFileStore fileStore = new CheckinFileStore(dataFile);
            FitnessService service = new FitnessService(fileStore);

            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            server.createContext("/api/checkin", new CheckinHandler(service));
            server.createContext("/", new StaticFileHandler(staticDir));

            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();

            System.out.println("========================================");
            System.out.println("  运动打卡服务器启动成功！");
            System.out.println("  访问地址: http://localhost:" + PORT);
            System.out.println("  按 Ctrl+C 停止服务器");
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("服务器启动失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
