package diruptio.spikedog;

import java.io.BufferedReader;
import java.io.StringReader;
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
                String[] pieces = parameter.split("=", 2);
                request.parameters.put(
                        pieces[0], URLDecoder.decode(pieces[1], StandardCharsets.UTF_8));
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

    public static @Nullable HttpRequest parse(@NotNull String str) {
        HttpRequest request = new HttpRequest();

        BufferedReader reader = new BufferedReader(new StringReader(str));

        try {
            String[] requestLine = reader.readLine().split(" ");
            if (requestLine.length == 3) {
                request.method = requestLine[0];
                decodePath(requestLine[1], request);
                request.httpVersion = requestLine[2];
            } else return null;

            for (String header = reader.readLine();
                    header != null && !header.isBlank();
                    header = reader.readLine()) {
                if (header.contains(": ")) {
                    String[] pieces = header.split(": ", 2);
                    request.headers.put(pieces[0], pieces[1]);
                }
            }

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) content.append(line).append("\r\n");
            request.content = content.toString();
            String contentType = request.getHeader("Content-Type");
            if (contentType != null && contentType.equals("application/x-www-form-urlencoded")) {
                decodeParameters(request.content, request);
            }
        } catch (Exception ignored) {
        }

        return request;
    }
}
