package diruptio.spikedog;

import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.NotNull;

public class Spikedog {
    public static final String BIND_ADDRESS = "0.0.0.0";
    public static final int PORT = 8080;
    public static final Path MODULES_DIRECTORY = Path.of("modules");
    private static final List<Servlet> servlets = new ArrayList<>();

    public static void main(String[] args) {
        try (Selector selector = Selector.open();
                ServerSocketChannel serverSocket = ServerSocketChannel.open()) {
            // Start server
            serverSocket.bind(new InetSocketAddress(BIND_ADDRESS, PORT));
            System.out.printf("Spikedog started on %s:%s\n", BIND_ADDRESS, PORT);

            // Load modules
            ModuleLoader.loadModules(MODULES_DIRECTORY);

            // Accept connections
            while (true) new ServeThread(serverSocket.accept()).start();
        } catch (Throwable exception) {
            exception.printStackTrace(System.err);
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
