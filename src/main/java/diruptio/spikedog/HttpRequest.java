package diruptio.spikedog;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A HTTP request. */
public class HttpRequest {
    private final String version;
    private final HttpMethod method;
    private final QueryString queryString;
    private final Map<String, String> headers = new HashMap<>();
    private final ByteBuf content;
    private final Map<String, List<String>> parameters;

    /**
     * Creates a new {@link HttpRequest} with data from a Netty HTTP/1 request.
     *
     * @param request The Netty HTTP/1 request
     * @throws IllegalArgumentException If the URI is invalid
     */
    public HttpRequest(@NotNull FullHttpRequest request) {
        method = request.method();
        try {
            queryString = QueryString.parse(new URI(request.uri()));
        } catch (URISyntaxException ignored) {
            throw new IllegalArgumentException("Invalid URI");
        }
        version = request.protocolVersion().text();
        for (Map.Entry<String, String> header : request.headers()) {
            headers.put(header.getKey(), header.getValue());
        }
        content = Unpooled.wrappedUnmodifiableBuffer(request.content());
        parameters = queryString.parameters;
        if (header("Content-Type", "").startsWith("application/x-www-form-urlencoded")) {
            decodeParameters(content.toString(), this);
        }
    }

    /**
     * Creates a new {@link HttpRequest} with data from a Netty HTTP/2 request.
     *
     * @param headersFrame The Netty HTTP/2 headers frame
     * @param dataFrame The Netty HTTP/2 data frame
     * @throws IllegalArgumentException If the URI is invalid
     */
    public HttpRequest(@NotNull Http2HeadersFrame headersFrame, @Nullable Http2DataFrame dataFrame) {
        method = HttpMethod.valueOf(headersFrame.headers().method().toString());
        try {
            queryString = QueryString.parse(new URI(
                    headersFrame.headers().scheme().toString(),
                    headersFrame.headers().authority().toString(),
                    headersFrame.headers().path().toString()));
        } catch (URISyntaxException ignored) {
            throw new IllegalArgumentException("Invalid URI");
        }
        version = "HTTP/2";
        headersFrame
                .headers()
                .iterator()
                .forEachRemaining(header -> headers.put(
                        header.getKey().toString(), header.getValue().toString()));
        if (dataFrame == null) {
            content = Unpooled.directBuffer(0);
        } else {
            content = new EmptyByteBuf(UnpooledByteBufAllocator.DEFAULT);
        }
        parameters = queryString.parameters;
        if (header("Content-Type", "").startsWith("application/x-www-form-urlencoded")) {
            decodeParameters(content.toString(), this);
        }
    }

    /**
     * Gets the version of the request.
     *
     * @return The HTTP version
     */
    public @NotNull String version() {
        return version;
    }

    /**
     * Gets the method of the request.
     *
     * @return The method
     */
    public @NotNull HttpMethod method() {
        return method;
    }

    /**
     * Gets the query string of the request.
     *
     * @return The query string
     */
    public @NotNull QueryString queryString() {
        return queryString;
    }

    /**
     * Gets the headers of the request.
     *
     * @return The headers
     */
    public @NotNull Map<String, String> headers() {
        return headers;
    }

    /**
     * Gets a header from the request.
     *
     * @param key The key of the header
     * @return The header value, or {@code null} if not found
     */
    public @Nullable String header(@NotNull String key) {
        return headers.get(key);
    }

    /**
     * Gets a header from the request.
     *
     * @param key The key of the header
     * @param defaultValue The default value if the header is not found
     * @return The header value, or the default value if not found
     */
    public @NotNull String header(@NotNull String key, @NotNull String defaultValue) {
        return headers.getOrDefault(key, defaultValue);
    }

    /**
     * Gets the parameters of the request.
     *
     * @return The parameters
     */
    public @NotNull Map<String, List<String>> parameters() {
        return parameters;
    }

    /**
     * Gets a list of values for a parameter from the request.
     *
     * @param key The key of the parameter
     * @return The parameter values, or {@code null} if not found
     */
    public @Nullable List<String> parameters(@NotNull String key) {
        return parameters.get(key);
    }

    /**
     * Gets a parameter from the request.
     *
     * @param key The key of the parameter
     * @return The parameter value, or {@code null} if not found
     */
    public @Nullable String parameter(@NotNull String key) {
        List<String> values = parameters.get(key);
        return values == null ? null : values.getFirst();
    }

    /**
     * Gets the content of the request.
     *
     * @return The content
     */
    public @NotNull ByteBuf content() {
        return content;
    }

    /**
     * Gets the content of the request as a string.
     *
     * @return The content as a string
     */
    public @NotNull String contentAsString() {
        return content.toString();
    }

    private void decodeParameters(String parameters, HttpRequest request) {
        for (String parameter : parameters.split("&")) {
            if (parameter.contains("=")) {
                String[] parts = parameter.split("=", 2);
                String name = parts[0];
                String value = parts[1];
                ImmutableList.Builder<String> builder = ImmutableList.builder();
                if (request.parameters.containsKey(name)) {
                    builder.addAll(request.parameters.get(name));
                }
                builder.add(URLDecoder.decode(value, StandardCharsets.UTF_8));
                request.parameters.put(name, builder.build());
            }
        }
    }

    /**
     * The query string of an URI.
     *
     * @param rawQuery The raw query string
     * @param path The path of the URI
     * @param parameters Additional parameters of the query string
     * @param fragment The fragment of the URI
     */
    public record QueryString(
            @NotNull String path,
            @NotNull String rawQuery,
            @NotNull Map<String, List<String>> parameters,
            @Nullable String fragment) {

        /**
         * Parses a URI into a query string.
         *
         * @param uri The URI
         * @return The query string
         */
        public static @NotNull QueryString parse(@NotNull URI uri) {
            QueryStringDecoder decoder = new QueryStringDecoder(uri);
            return new QueryString(decoder.path(), decoder.rawQuery(), decoder.parameters(), uri.getFragment());
        }
    }
}
