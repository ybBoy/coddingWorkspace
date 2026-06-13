package app;

import service.ReadingService;
import store.JsonFileStore;
import ws.ReadingSocket;

public class AppServer {
    public static void main(String[] args) {
        int port = 8080;
        String dataFilePath = "data/reading-session.json";

        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }
        if (args.length > 1) {
            dataFilePath = args[1];
        }

        System.out.println("=====================================");
        System.out.println("  读书会共读标注板 - 后端服务");
        System.out.println("=====================================");
        System.out.println("  WebSocket Port : " + port);
        System.out.println("  数据文件      : " + dataFilePath);
        System.out.println("=====================================");

        JsonFileStore store = new JsonFileStore(dataFilePath, null);
        ReadingService service = new ReadingService(store);

        JsonFileStore storeWithHook = new JsonFileStore(dataFilePath, () ->
                store.save(service.getArticle(), service.getAllNotes()));

        ReadingSocket socket = new ReadingSocket(port, service);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Shutdown] Saving final state...");
            store.save(service.getArticle(), service.getAllNotes());
            try {
                socket.stop(1000, "Server shutting down");
            } catch (Exception ignored) {}
            store.shutdown();
            storeWithHook.shutdown();
            System.out.println("[Shutdown] Goodbye.");
        }));

        socket.start();
        System.out.println("[Server] Ready. Press Ctrl+C to stop.");
    }
}
