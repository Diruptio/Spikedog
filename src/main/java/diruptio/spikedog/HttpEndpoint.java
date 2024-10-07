package diruptio.spikedog;

import org.jetbrains.annotations.NotNull;

/** A HTTP endpoint. */
@FunctionalInterface
public interface HttpEndpoint {
    /**
     * Handles the HTTP request.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     */
    void handle(@NotNull HttpRequest request, @NotNull HttpResponse response);
}
