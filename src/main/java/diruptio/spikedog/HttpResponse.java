package diruptio.spikedog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HttpResponse {
    private String httpVersion = "HTTP/1.1";
    private int statusCode = 200;
    private String statusMessage = "OK";
    private Map<String, String> headers = new LinkedHashMap<>();
    private String content = "";
    private Charset charset = StandardCharsets.UTF_8;

    public HttpResponse() {
        headers.put("Content-Type", "text/plain");
    }

    public @NotNull String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(@NotNull String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public @NotNull String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(@NotNull String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public void setStatus(int statusCode, @NotNull String statusMessage) {
        this.statusMessage = statusMessage;
        this.statusCode = statusCode;
    }

    public @NotNull Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(@NotNull Map<String, String> headers) {
        this.headers = headers;
    }

    public @Nullable String getHeader(@NotNull String key) {
        return headers.get(key);
    }

    public @NotNull String getHeader(@NotNull String key, @NotNull String defaultValue) {
        return headers.getOrDefault(key, defaultValue);
    }

    public void setHeader(@NotNull String key, @NotNull String value) {
        headers.put(key, value);
    }

    public @NotNull String getContent() {
        return content;
    }

    public void setContent(@NotNull String content) {
        this.content = content;
    }

    public @Nullable Charset getCharset() {
        return charset;
    }

    public void setCharset(@Nullable Charset charset) {
        this.charset = charset;
    }

    public byte[] toByteArray() {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            stream.writeBytes(httpVersion.getBytes(StandardCharsets.US_ASCII));
            stream.writeBytes(" ".getBytes(StandardCharsets.US_ASCII));
            stream.writeBytes(String.valueOf(statusCode).getBytes(StandardCharsets.US_ASCII));
            stream.writeBytes(" ".getBytes(StandardCharsets.US_ASCII));
            stream.writeBytes(statusMessage.getBytes(StandardCharsets.US_ASCII));
            stream.writeBytes("\r\n".getBytes(StandardCharsets.US_ASCII));
            headers.forEach((key, value) -> {
                stream.writeBytes(key.getBytes(StandardCharsets.US_ASCII));
                stream.writeBytes(": ".getBytes(StandardCharsets.US_ASCII));
                stream.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
                if (key.equalsIgnoreCase("Content-Type") && charset != null) {
                    stream.writeBytes("; charset=".getBytes(StandardCharsets.US_ASCII));
                    stream.writeBytes(charset.name().getBytes(StandardCharsets.US_ASCII));
                }
                stream.writeBytes("\r\n".getBytes(StandardCharsets.US_ASCII));
            });
            stream.writeBytes("\r\n".getBytes(StandardCharsets.US_ASCII));
            if (charset != null) {
                stream.writeBytes(content.getBytes(charset));
            } else {
                stream.writeBytes(content.getBytes());
            }
            byte[] bytes = stream.toByteArray();
            stream.close();
            return bytes;
        } catch (IOException ignored) {
            return new byte[0];
        }
    }
}
