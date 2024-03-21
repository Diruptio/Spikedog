package diruptio.spikedog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class ServeThread extends Thread {
    private static final List<ServeThread> serveThreads = new ArrayList<>();
    private final SocketChannel client;
    private final List<Runnable> afterServe = new ArrayList<>();

    public ServeThread(SocketChannel client) {
        this.client = client;
        serveThreads.add(this);
    }

    @Override
    public void run() {
        try {
            if (!client.isOpen()) return;
            SocketAddress socketAddress = client.getRemoteAddress();
            String address = ((InetSocketAddress) socketAddress).getHostString();

            ByteBuffer buffer = ByteBuffer.allocate(1024);
            if (!client.isOpen()) return;
            client.read(buffer);
            buffer.flip();
            HttpRequest request =
                    HttpRequest.parse(new String(buffer.array(), StandardCharsets.UTF_8));
            buffer.clear();

            HttpResponse response = new HttpResponse();
            if (request == null) {
                response.setStatus(400, "Bad Request");
                response.setHeader("Content-Type", "text/html");
                response.setContent("<h1>400 Bad Request</h1>");
            } else {
                System.out.printf(
                        "Received request from %s: %s %s\n",
                        address, request.getMethod(), request.getPath());
                boolean found = false;
                for (Spikedog.Servlet servlet : Spikedog.getServlets()) {
                    if (servlet.path().equals(request.getPath())
                            && (servlet.methods().length == 0
                                    || List.of(servlet.methods()).contains(request.getMethod()))) {
                        found = true;
                        try {
                            servlet.servlet().accept(request, response);
                        } catch (Throwable exception) {
                            exception.printStackTrace(System.err);
                            response.setStatus(500, "Internal Server Error");
                            response.setHeader("Content-Type", "text/html");
                            response.setContent("<h1>500 Internal Server Error</h1>");
                        }
                    }
                }
                if (!found) {
                    response.setStatus(404, "Not Found");
                    response.setHeader("Content-Type", "text/html");
                    response.setContent("<h1>404 Not Found</h1>");
                }
            }

            response.setHeader("Content-Length", String.valueOf(response.getContent().length()));
            response.setHeader("Server", "Spikedog " + BuildConstants.VERSION);
            buffer = ByteBuffer.wrap(response.toString().getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) client.write(buffer);
            buffer.clear();
            client.close();
        } catch (IOException ignored) {
        } catch (Throwable exception) {
            exception.printStackTrace(System.err);
        }
        try {
            afterServe.forEach(Runnable::run);
        } catch (Throwable exception) {
            exception.printStackTrace(System.err);
        }
        serveThreads.remove(this);
    }

    public static void runAfterServe(@NotNull Runnable runnable) {
        serveThreads.stream()
                .filter(thread -> thread == Thread.currentThread())
                .forEach(thread -> thread.afterServe.add(runnable));
    }
}
