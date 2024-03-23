package diruptio.spikedog;

import java.io.StringReader;
import java.util.function.Function;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HttpRequestTest {
    @Test
    public void testParse() {
        String request =
                """
                GET /test HTTP/1.1\r
                Host: localhost:8080\r
                User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0\r
                Accept: application/json\r
                """;
        HttpRequest httpRequest = HttpRequest.read(new StringReader(request));
        Assertions.assertNotNull(httpRequest);
        Assertions.assertEquals("GET", httpRequest.getMethod());
        Assertions.assertEquals("/test", httpRequest.getPath());
        Assertions.assertEquals("HTTP/1.1", httpRequest.getHttpVersion());
        Assertions.assertEquals("localhost:8080", httpRequest.getHeader("Host"));
        Assertions.assertEquals(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0",
                httpRequest.getHeader("User-Agent"));
        Assertions.assertEquals("application/json", httpRequest.getHeader("Accept"));
    }

    @Test
    public void testParseWithBadRequests() {
        String request =
                """
                GET HTTP/1.1\r
                Host: localhost:8080\r
                User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0\r
                Accept: application/json\r
                """;
        HttpRequest httpRequest = HttpRequest.read(new StringReader(request));
        Assertions.assertNull(httpRequest);
    }

    private static class StringReader implements Function<Integer, String> {
        private final java.io.StringReader reader;

        private StringReader(String str) {
            reader = new java.io.StringReader(str);
        }

        @Override
        public String apply(Integer size) {
            try {
                char[] buffer = new char[size];
                reader.read(buffer);
                return new String(buffer);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}
