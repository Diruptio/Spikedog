package diruptio.spikedog;

import io.netty.handler.codec.http.HttpMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Provides an {@link HttpEndpoint} for a specified path and method. */
@FunctionalInterface
public interface EndpointProvider {
    /**
     * Searches a {@link HttpEndpoint} for the specified path and method.
     *
     * @param path The path
     * @param method The method
     * @return The {@link HttpEndpoint}, or {@code null} if not found.
     */
    @Nullable
    HttpEndpoint getEndpoint(@NotNull String path, @NotNull HttpMethod method);
}
