package diruptio.spikedog;

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
        System.out.println(parameters);
        for (String parameter : parameters.split("&")) {
            if (parameter.contains("=")) {
                String[] pieces = parameter.split("=", 2);
                request.parameters.put(pieces[0], URLDecoder.decode(pieces[1], StandardCharsets.UTF_8));
            } else request.parameters.put(parameter, "");
        }
    }

    private static void decodePath(String path, HttpRequest request) {
        if (path.contains("?")) {
            String[] pieces = path.split("\\?", 2);
            path = pieces[0];
            decodeParameters(pieces[1], request);
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
            String[] pieces = requestLine.split(" ");
            if (pieces.length == 3) {
                request.method = pieces[0];
                decodePath(pieces[1], request);
                request.httpVersion = pieces[2];
            } else return null;

            String header;
            while (true) {
                header = readLine(client);
                if (header.isBlank()) break;
                if (header.contains(": ")) {
                    pieces = header.split(": ", 2);
                    request.headers.put(pieces[0], pieces[1]);
                    if (pieces[0].equalsIgnoreCase("Content-Length")) {
                        contentLength = Integer.parseInt(pieces[1]);
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
