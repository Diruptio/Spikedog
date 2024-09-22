package diruptio.spikedog;

import io.netty.channel.Channel;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServeTask implements Runnable {
    private static final Set<ServeTask> tasks = new HashSet<>();
    private final Channel channel;
    private final HttpRequest request;
    private final CompletableFuture<HttpResponse> future;

    public ServeTask(
            @NotNull Channel channel, @NotNull HttpRequest request, @NotNull CompletableFuture<HttpResponse> future) {
        this.channel = channel;
        this.request = request;
        this.future = future;
        tasks.add(this);
    }

    private void complete(HttpResponse response) {
        String contentType = response.getHeader("Content-Type");
        if (contentType != null && response.getCharset() != null) {
            response.setHeader("Content-Type", contentType + "; charset=" + response.getCharset());
        }
        response.setHeader("Content-Length", String.valueOf(response.getContentLength()));
        response.setHeader("Server", "Spikedog/" + Spikedog.VERSION.get());
        response.setHeader("Access-Control-Allow-Origin", "*");
        try {
            future.complete(response);
        } catch (Throwable exception) {
            exception.printStackTrace(System.err);
        }
    }

    @Override
    public void run() {
        if (!channel.isOpen()) return;

        // Check if connection is allowed
        for (Module module : ModuleLoader.getModules()) {
            for (Listener listener : module.listeners()) {
                if (!listener.allowConnection(channel)) {
                    HttpResponse response = new HttpResponse();
                    response.setStatus(403, "Forbidden");
                    response.setContent("<h1>403 Forbidden</h1>");
                    complete(response);
                    return;
                }
            }
        }

        String address = ((InetSocketAddress) channel.remoteAddress()).getHostString();
        Spikedog.LOGGER.info(
                "Received request from %s: %s %s".formatted(address, request.getMethod(), request.getPath()));

        // Search for servlet
        for (Spikedog.Servlet servlet : Spikedog.getServlets()) {
            if (servlet.path().equals(request.getPath())
                    && (servlet.methods().length == 0
                            || List.of(servlet.methods()).contains(request.getMethod()))) {
                HttpResponse response = new HttpResponse();
                try {
                    servlet.servlet().accept(request, response);
                    complete(response);
                } catch (Throwable exception) {
                    exception.printStackTrace(System.err);
                    response = new HttpResponse();
                    response.setStatus(500, "Internal Server Error");
                    response.setHeader("Content-Type", "text/html");
                    response.setContent("<h1>500 Internal Server Error</h1>");
                    complete(response);
                }
                return;
            }
        }

        // No servlet found
        HttpResponse response = new HttpResponse();
        response.setStatus(404, "Not Found");
        response.setHeader("Content-Type", "text/html");
        response.setContent("<h1>404 Not Found</h1>");
        complete(response);
    }

    public @NotNull Channel getChannel() {
        return channel;
    }

    public @NotNull CompletableFuture<HttpResponse> getFuture() {
        return future;
    }

    public static @NotNull Set<ServeTask> getTasks() {
        return tasks;
    }

    public static @Nullable ServeTask getTaskByRequest(@NotNull HttpRequest request) {
        for (ServeTask task : tasks) {
            if (task.request == request) {
                return task;
            }
        }
        return null;
    }
}
