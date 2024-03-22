package diruptio.spikedog;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HttpResponse {
    private String httpVersion = "HTTP/1.1";
    private int statusCode = 200;
    private String statusMessage = "OK";
    private Map<String, String> headers = new HashMap<>();
    private String content = "";

    public HttpResponse() {
        headers.put("Content-Type", "text/plain; charset=UTF-8");
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

    @Override
    public String toString() {
        StringBuilder response = new StringBuilder();
        response.append(httpVersion)
                .append(' ')
                .append(statusCode)
                .append(' ')
                .append(statusMessage)
                .append("\r\n");
        headers.forEach(
                (key, value) -> response.append(key).append(": ").append(value).append("\r\n"));
        response.append("\r\n").append(content);
        return response.toString();
    }
}
