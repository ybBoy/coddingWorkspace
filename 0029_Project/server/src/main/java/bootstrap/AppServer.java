package bootstrap;

import application.LedgerService;
import domain.LedgerSnapshot;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.xnio.OptionMap;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import storage.JsonLedgerStore;
import transport.LedgerSocket;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AppServer {
    private static final int PORT = 8088;
    private static final String DATA_DIR = "data";
    private static final int SAVE_INTERVAL_SECONDS = 30;

    private final LedgerService ledgerService;
    private final JsonLedgerStore ledgerStore;
    private final ScheduledExecutorService scheduler;

    public AppServer() {
        this.ledgerService = new LedgerService();
        this.ledgerStore = new JsonLedgerStore(DATA_DIR);
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() throws ServletException, IOException {
        System.out.println("Starting Family Ledger Server...");

        restoreData();
        LedgerSocket.setLedgerService(ledgerService);
        startPersistenceScheduler();

        XnioWorker xnioWorker = Xnio.getInstance().createWorker(OptionMap.EMPTY);

        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(AppServer.class.getClassLoader())
                .setContextPath("/")
                .setDeploymentName("ledger.war")
                .addServletContextAttribute(
                        WebSocketDeploymentInfo.ATTRIBUTE_NAME,
                        new WebSocketDeploymentInfo()
                                .addEndpoint(LedgerSocket.class)
                                .setWorker(xnioWorker)
                );

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();

        Undertow server = Undertow.builder()
                .addHttpListener(PORT, "0.0.0.0")
                .setHandler(Handlers.path()
                        .addPrefixPath("/", Handlers.resource(
                                new ClassPathResourceManager(AppServer.class.getClassLoader(), "static")
                        ).setDirectoryListingEnabled(false))
                        .addPrefixPath("/", manager.start())
                )
                .build();

        server.start();
        System.out.println("Server started on http://localhost:" + PORT);
        System.out.println("WebSocket endpoint: ws://localhost:" + PORT + "/ledger");
        System.out.println("Data will be saved every " + SAVE_INTERVAL_SECONDS + " seconds to " + DATA_DIR);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            scheduler.shutdown();
            try {
                saveData();
            } catch (IOException e) {
                e.printStackTrace();
            }
            server.stop();
            System.out.println("Server stopped.");
        }));
    }

    private void restoreData() throws IOException {
        System.out.println("Restoring data from " + DATA_DIR + "...");
        LedgerSnapshot snapshot = ledgerStore.load();
        ledgerService.restoreFromSnapshot(snapshot);
        System.out.println("Restored " + snapshot.getExpenses().size() + " expenses and "
                + snapshot.getBudgets().size() + " budgets.");
    }

    private void saveData() throws IOException {
        LedgerSnapshot snapshot = ledgerService.createSnapshot();
        ledgerStore.save(snapshot);
        System.out.println("Saved " + snapshot.getExpenses().size() + " expenses and "
                + snapshot.getBudgets().size() + " budgets.");
    }

    private void startPersistenceScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                saveData();
            } catch (IOException e) {
                System.err.println("Failed to save data: " + e.getMessage());
            }
        }, SAVE_INTERVAL_SECONDS, SAVE_INTERVAL_SECONDS, TimeUnit.SECONDS);
        System.out.println("Auto-save scheduler started (every " + SAVE_INTERVAL_SECONDS + " seconds).");
    }

    public static void main(String[] args) throws Exception {
        new AppServer().start();
    }
}
