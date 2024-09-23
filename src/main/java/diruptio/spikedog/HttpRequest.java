package diruptio.spikedog;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HttpRequest {
    private String method;
    private String path;
    private String httpVersion;
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, String> parameters = new HashMap<>();
    private String content;

    public HttpRequest(@NotNull FullHttpRequest request) {
        method = request.method().name();
        path = request.uri();
        httpVersion = request.protocolVersion().text();
        for (Map.Entry<String, String> header : request.headers()) {
            headers.put(header.getKey(), header.getValue());
        }
        content = request.content().toString(StandardCharsets.UTF_8);
        String contentType = getHeader("Content-Type", "");
        if (contentType.startsWith("application/x-www-form-urlencoded")) {
            decodeParameters(content, this);
        }
    }

    public HttpRequest(@NotNull Http2HeadersFrame headersFrame, @Nullable Http2DataFrame dataFrame) {
        method = headersFrame.headers().method().toString();
        path = headersFrame.headers().path().toString();
        httpVersion = "HTTP/2";
        headersFrame
                .headers()
                .iterator()
                .forEachRemaining(header -> headers.put(
                        header.getKey().toString(), header.getValue().toString()));
        if (dataFrame == null) {
            content = "";
        } else {
            content = new String(dataFrame.content().array());
        }
        String contentType = getHeader("Content-Type", "");
        if (contentType.startsWith("application/x-www-form-urlencoded")) {
            decodeParameters(content, this);
        }
    }

    private HttpRequest() {}

    public @NotNull String getMethod() {
        return method;
    }

    public @NotNull String getPath() {
        return path;
    }

    public @NotNull String getHttpVersion() {
        return httpVersion;
    }

    public @NotNull Map<String, String> getHeaders() {
        return headers;
    }

    public @Nullable String getHeader(@NotNull String key) {
        return headers.get(key);
    }

    public @NotNull String getHeader(@NotNull String key, @NotNull String defaultValue) {
        return headers.getOrDefault(key, defaultValue);
    }

    public @NotNull Map<String, String> getParameters() {
        return parameters;
    }

    public @Nullable String getParameter(@NotNull String key) {
        return parameters.get(key);
    }

    public @NotNull String getContent() {
        return content;
    }

    private static void decodeParameters(String parameters, HttpRequest request) {
        for (String parameter : parameters.split("&")) {
            if (parameter.contains("=")) {
                String[] parts = parameter.split("=", 2);
                request.parameters.put(parts[0], URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            } else request.parameters.put(parameter, "");
        }
    }
}
