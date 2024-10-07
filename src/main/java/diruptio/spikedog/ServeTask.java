package diruptio.spikedog;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Serves HTTP requests. */
public class ServeTask implements Runnable {
    private static final Set<ServeTask> tasks = new HashSet<>();
    private final Channel channel;
    private final HttpRequest request;
    private final CompletableFuture<HttpResponse> future;

    /**
     * Creates a new {@link ServeTask}.
     *
     * @param channel The client's channel
     * @param request The request
     * @param future The callback to return a {@link HttpResponse}
     */
    public ServeTask(
            @NotNull Channel channel, @NotNull HttpRequest request, @NotNull CompletableFuture<HttpResponse> future) {
        this.channel = channel;
        this.request = request;
        this.future = future;
        tasks.add(this);
    }

    private void complete(HttpResponse response) {
        CharSequence contentType = response.header(HttpHeaderNames.CONTENT_TYPE);
        if (contentType != null && response.charset() != null) {
            response.header(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=" + response.charset());
        }
        response.header(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(response.getContentLength()));
        response.header(HttpHeaderNames.SERVER, "Spikedog/" + Spikedog.VERSION.get());
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
                    HttpResponse response = new HttpResponse(request.version());
                    response.status(HttpResponseStatus.FORBIDDEN);
                    response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_HTML);
                    response.content("<h1>403 Forbidden</h1>");
                    complete(response);
                    return;
                }
            }
        }

        String address = ((InetSocketAddress) channel.remoteAddress()).getHostString();
        Spikedog.LOGGER.info("Request from %s: %s %s"
                .formatted(address, request.method(), request.queryString().path()));

        // Search for servlet
        for (EndpointProvider provider : Spikedog.getEndpointProviders()) {
            HttpEndpoint endpoint = provider.getEndpoint(request.queryString().path(), request.method());
            if (endpoint != null) {
                try {
                    HttpResponse response = new HttpResponse(request.version());
                    endpoint.handle(request, response);
                    complete(response);
                } catch (Throwable exception) {
                    exception.printStackTrace(System.err);
                    HttpResponse response = new HttpResponse(request.version());
                    response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                    response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_HTML);
                    response.content("<h1>500 Internal Server Error</h1>");
                    complete(response);
                }
                return;
            }
        }

        // No servlet found
        HttpResponse response = new HttpResponse(request.version());
        response.status(HttpResponseStatus.NOT_FOUND);
        response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_HTML);
        response.content("<h1>404 Not Found</h1>");
        complete(response);
    }

    /**
     * Gets the client's channel.
     *
     * @return The channel
     */
    public @NotNull Channel getChannel() {
        return channel;
    }

    /**
     * Gets the request.
     *
     * @return The request
     */
    public @NotNull CompletableFuture<HttpResponse> getFuture() {
        return future;
    }

    /**
     * Gets all running tasks.
     *
     * @return The tasks
     */
    public static @NotNull Set<ServeTask> getTasks() {
        return tasks;
    }

    /**
     * Gets a task by its request.
     *
     * @param request The request
     * @return The task or {@code null} if not found
     */
    public static @Nullable ServeTask getTaskByRequest(@NotNull HttpRequest request) {
        for (ServeTask task : tasks) {
            if (task.request == request) {
                return task;
            }
        }
        return null;
    }
}
