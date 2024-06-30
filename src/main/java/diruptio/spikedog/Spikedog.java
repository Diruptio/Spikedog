package diruptio.spikedog;

import diruptio.spikedog.logging.SpikedogLogger;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.logging.*;
import org.jetbrains.annotations.NotNull;

public class Spikedog {
    public static final Logger LOGGER = new SpikedogLogger();
    public static final Supplier<String> VERSION = () -> BuildConstants.VERSION;
    public static final Path MODULES_DIRECTORY = Path.of("modules");
    private static final List<Servlet> servlets = new ArrayList<>();

    public static void main(String[] args) {
        LOGGER.setLevel(Level.INFO);
        listen(8080, "0.0.0.0", true);
    }

    /**
     * Starts the server and listens for incoming connections.
     *
     * @param port The port to listen on
     * @param bindAddress The address to bind to
     * @param loadModules Whether to load modules from the modules directory
     */
    public static void listen(int port, @NotNull String bindAddress, boolean loadModules) {
        WatchdogThread guardianThread = new WatchdogThread();
        guardianThread.setDaemon(true);
        guardianThread.start();

        try (ServerSocketChannel serverSocket = ServerSocketChannel.open()) {
            // Start server
            serverSocket.bind(new InetSocketAddress(bindAddress, port));
            LOGGER.info("Spikedog started on %s:%s".formatted(bindAddress, port));

            // Load modules
            if (loadModules) {
                ModuleLoader.loadModules(MODULES_DIRECTORY);
            }

            // Accept connections
            while (true) new ServeThread(serverSocket.accept()).start();
        } catch (IOException exception) {
            LOGGER.log(Level.SEVERE, "Spikedog crashed", exception);
            System.exit(1);
        }
    }

    public static @NotNull List<Servlet> getServlets() {
        return servlets;
    }

    public static void addServlet(
            @NotNull String path,
            @NotNull BiConsumer<HttpRequest, HttpResponse> servlet,
            @NotNull String... methods) {
        servlets.add(new Servlet(path, servlet, methods));
    }

    public record Servlet(
            String path, BiConsumer<HttpRequest, HttpResponse> servlet, String[] methods) {}
}
