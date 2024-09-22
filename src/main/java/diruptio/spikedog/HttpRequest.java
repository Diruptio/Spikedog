package diruptio.spikedog;

import io.netty.handler.codec.http.FullHttpRequest;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
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

    private static void decodePath(String path, HttpRequest request) {
        if (path.contains("?")) {
            String[] parts = path.split("\\?", 2);
            path = parts[0];
            decodeParameters(parts[1], request);
        }
        request.path = URLDecoder.decode(path, StandardCharsets.UTF_8);
    }

    private static @NotNull String readLine(@NotNull SocketChannel client) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        StringBuilder line = new StringBuilder();
        while (client.read(buffer) != -1) {
            buffer.flip();
            char c = (char) buffer.get();
            if (c == '\n') break;
            if (c != '\r') line.append(c);
            buffer.clear();
        }
        return line.toString();
    }

    public static @Nullable HttpRequest read(@NotNull SocketChannel client) {
        try {
            HttpRequest request = new HttpRequest();
            int contentLength = -1;
            String requestLine = readLine(client);
            String[] parts = requestLine.split(" ");
            if (parts.length == 3) {
                request.method = parts[0];
                decodePath(parts[1], request);
                request.httpVersion = parts[2];
            } else return null;

            String header;
            while (true) {
                header = readLine(client);
                if (header.isBlank()) break;
                if (header.contains(": ")) {
                    parts = header.split(": ", 2);
                    request.headers.put(parts[0], parts[1]);
                    if (parts[0].equalsIgnoreCase("Content-Length")) {
                        contentLength = Integer.parseInt(parts[1]);
                    }
                }
            }
            if (contentLength > 0) {
                ByteBuffer buffer = ByteBuffer.allocate(contentLength);
                client.read(buffer);
                request.content = new String(buffer.array());
                buffer.clear();
            } else request.content = "";
            String contentType = request.getHeader("Content-Type", "");
            if (contentType.startsWith("application/x-www-form-urlencoded")) {
                decodeParameters(request.content, request);
            }

            return request;
        } catch (Throwable ignored) {
            ignored.printStackTrace(System.err);
            return null;
        }
    }
}
