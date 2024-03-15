package diruptio.spikedog;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private String method;
    private String path;
    private String httpVersion;
    private final Map<String, String> headers = new HashMap<>();
    private String content;

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

    public String getContent() {
        return content;
    }

    public static HttpRequest parse(String str) {
        HttpRequest request = new HttpRequest();

        BufferedReader reader = new BufferedReader(new StringReader(str));

        try {
            String[] requestLine = reader.readLine().split(" ");
            if (requestLine.length == 3) {
                request.method = requestLine[0];
                request.path = requestLine[1];
                request.httpVersion = requestLine[2];
            } else return null;

            for (String header = reader.readLine();
                    header != null && !header.isBlank();
                    header = reader.readLine()) {
                if (header.contains(": ")) {
                    String[] pieces = header.split(": ", 2);
                    request.headers.put(pieces[0], pieces[1]);
                } else return null;
            }

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\r\n");
            }
            request.content = content.toString();
        } catch (Exception ignored) {
        }

        return request;
    }
}
