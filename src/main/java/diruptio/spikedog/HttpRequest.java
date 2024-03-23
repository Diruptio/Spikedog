package diruptio.spikedog;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
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

    private static int readLines(List<String> lines, String content) {
        if (content == null) return -1;
        if (!lines.isEmpty()) content = lines.remove(lines.size() - 1) + content;
        lines.addAll(Arrays.asList(content.split("\r\n")));
        return content.length();
    }

    public static @Nullable HttpRequest read(@NotNull Function<Integer, String> reader) {
        try {
            HttpRequest request = new HttpRequest();
            List<String> lines = new ArrayList<>();
            long length = readLines(lines, reader.apply(1024));
            System.out.println(1);
            if (length == -1) return null;
            int processedLength = 0;
            int contentLength;

            String requestLine = lines.remove(0);
            processedLength += requestLine.length() + 2;
            String[] pieces = requestLine.split(" ");
            System.out.println(2);
            System.out.println(requestLine);
            if (pieces.length == 3) {
                request.method = pieces[0];
                decodePath(pieces[1], request);
                request.httpVersion = pieces[2];
            } else return null;

            for (String header = lines.remove(0);
                    !lines.isEmpty() && header != null && !header.isBlank();
                    header = lines.remove(0)) {
                processedLength += header.length() + 2;
                if (header.contains(": ")) {
                    pieces = header.split(": ", 2);
                    request.headers.put(pieces[0], pieces[1]);
                    if (pieces[0].equalsIgnoreCase("Content-Length")) {
                        contentLength = Integer.parseInt(pieces[1]);
                        int charsLeft = (int) (length - processedLength);
                        if (charsLeft < contentLength) {
                            int addedLength =
                                    readLines(lines, reader.apply(contentLength - charsLeft));
                            System.out.println(3);
                            if (addedLength == -1) return null;
                            length += addedLength;
                        }
                    }
                }
            }

            StringBuilder content = new StringBuilder();
            while (!lines.isEmpty()) content.append(lines.remove(0)).append("\r\n");
            request.content = content.toString();
            String contentType = request.getHeader("Content-Type", "");
            if (contentType.equals("application/x-www-form-urlencoded")) {
                decodeParameters(request.content, request);
            }

            return request;
        } catch (Throwable ignored) {
            System.out.println(4);
            ignored.printStackTrace(System.err);
            return null;
        }
    }
}
