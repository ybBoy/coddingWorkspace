package app;

import api.PlantController;
import com.sun.net.httpserver.HttpServer;
import persistence.PlantJsonStore;
import service.PlantCareService;

import java.io.IOException;
import java.net.InetSocketAddress;

public class PlantApp {
    private static final int PORT = 8088;
    private static final String DATA_FILE = "data/plants.json";

    public static void main(String[] args) {
        PlantJsonStore jsonStore = new PlantJsonStore(DATA_FILE);
        PlantCareService service = new PlantCareService(jsonStore);
        PlantController controller = new PlantController(service);

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/api/plants", controller.new PlantsHandler());
            server.setExecutor(null);
            server.start();

            System.out.println("Plant Care Server is running on port " + PORT);
            System.out.println("Data file: " + DATA_FILE);
            System.out.println("Press Ctrl+C to stop.");
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
