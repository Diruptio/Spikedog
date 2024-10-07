package diruptio.spikedog;

import io.netty.handler.codec.http.HttpMethod;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** The default implementation of the {@link EndpointProvider}. */
public class DefaultEndpointProvider implements EndpointProvider {
    private final Map<Endpoint, HttpEndpoint> endpoints = new ConcurrentHashMap<>();

    /** Creates a new {@link DefaultEndpointProvider}. */
    public DefaultEndpointProvider() {}

    @Override
    public @Nullable HttpEndpoint getEndpoint(@NotNull String path, @NotNull HttpMethod method) {
        for (Endpoint endpoint : endpoints.keySet()) {
            if (endpoint.path().equals(path)) {
                if (endpoint.methods().length == 0) {
                    return endpoints.get(endpoint);
                }
                for (String method2 : endpoint.methods()) {
                    if (method.name().equals(method2)) {
                        return endpoints.get(endpoint);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets the registered endpoints.
     *
     * @return The registered endpoints
     */
    public @NotNull Map<Endpoint, HttpEndpoint> getEndpoints() {
        return endpoints;
    }
}
