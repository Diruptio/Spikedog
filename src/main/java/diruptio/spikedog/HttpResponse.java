package diruptio.spikedog;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    private String httpVersion = "HTTP/1.1";
    private int statusCode = 200;
    private String statusMessage = "OK";
    private Map<String, String> headers = new HashMap<>();
    private String content = "";

    public String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public void setStatus(int statusCode, String statusMessage) {
        this.statusMessage = statusMessage;
        this.statusCode = statusCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

    public void setHeader(String key, String value) {
        headers.put(key, value);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
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
