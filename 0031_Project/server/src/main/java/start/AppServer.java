package start;

import core.RoomService;
import io.RoomJsonStore;
import socket.RoomSocket;

public class AppServer {

    public static final int WS_PORT = 8765;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  悦享民宿 · 房态管理服务器");
        System.out.println("  WebSocket Port: " + WS_PORT);
        System.out.println("========================================");

        RoomJsonStore store = new RoomJsonStore();
        store.start();

        RoomService roomService = new RoomService(store);

        RoomSocket socket = new RoomSocket(WS_PORT, roomService);
        socket.setReuseAddr(true);

        try {
            socket.start();
            System.out.println("[Server] Server is running. Press Ctrl+C to stop.");

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("[Server] Shutting down...");
                    try {
                        socket.stop(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    store.forceSave();
                    System.out.println("[Server] Server stopped.");
                }
            }));

            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("[Server] Failed to start: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
