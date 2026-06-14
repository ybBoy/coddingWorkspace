package app;

import api.PlantController;
import com.sun.net.httpserver.HttpServer;
import persistence.PlantJsonStore;
import service.PlantCareService;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.security.CodeSource;

public class PlantApp {
    private static final int PORT = 8088;
    private static final String DATA_FILE_NAME = "plants.json";
    private static final String DATA_DIR_NAME = "data";

    public static void main(String[] args) {
        String dataFilePath = getDataFilePath();
        PlantJsonStore jsonStore = new PlantJsonStore(dataFilePath);
        PlantCareService service = new PlantCareService(jsonStore);
        PlantController controller = new PlantController(service);

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/api/plants", controller.new PlantsHandler());
            server.setExecutor(null);
            server.start();

            System.out.println("Plant Care Server is running on port " + PORT);
            System.out.println("Data file: " + new File(dataFilePath).getAbsolutePath());
            System.out.println("Press Ctrl+C to stop.");
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getDataFilePath() {
        try {
            CodeSource codeSource = PlantApp.class.getProtectionDomain().getCodeSource();
            File jarFile = new File(codeSource.getLocation().toURI());
            File baseDir = jarFile.getParentFile();
            if (baseDir == null) {
                baseDir = new File(".");
            }
            File dataDir = new File(baseDir, DATA_DIR_NAME);
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            return new File(dataDir, DATA_FILE_NAME).getAbsolutePath();
        } catch (URISyntaxException e) {
            System.err.println("Warning: Could not determine JAR location, using current directory.");
            File dataDir = new File(DATA_DIR_NAME);
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            return new File(dataDir, DATA_FILE_NAME).getAbsolutePath();
        }
    }
}
