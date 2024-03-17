package diruptio.spikedog;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public class Spikedog {
    public static final String BIND_ADDRESS = "0.0.0.0";
    public static final int PORT = 8080;
    //private static final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private static final List<Servlet> servlets = new ArrayList<>();

    public static void main(String[] args) {
        try (Selector selector = Selector.open();
                ServerSocketChannel serverSocket = ServerSocketChannel.open()) {
            // Start server
            serverSocket.bind(new InetSocketAddress(BIND_ADDRESS, PORT));
            //serverSocket.configureBlocking(false);
            //serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            System.out.printf("Spikedog started on %s:%s\n", BIND_ADDRESS, PORT);

            ModuleLoader.loadModules(Path.of("modules"));

            while (true) new ServeThread(serverSocket.accept()).start();
        } catch (Throwable exception) {
            exception.printStackTrace(System.err);
            System.exit(1);
        }
    }

    public static List<Servlet> getServlets() {
        return servlets;
    }

    public static void addServlet(
            String path, BiConsumer<HttpRequest, HttpResponse> servlet, String... methods) {
        servlets.add(new Servlet(path, servlet, methods));
    }

    public record Servlet(
            String path, BiConsumer<HttpRequest, HttpResponse> servlet, String[] methods) {}
}
