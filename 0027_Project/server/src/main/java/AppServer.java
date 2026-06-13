import ws.ExpoSocket;

public class AppServer {

    public static void main(String[] args) {
        try {
            ExpoSocket server = ExpoSocket.getInstance();
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
