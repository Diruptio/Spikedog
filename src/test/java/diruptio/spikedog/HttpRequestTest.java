package diruptio.spikedog;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HttpRequestTest {
    @Test
    public void testParse() {
        String request =
                """
                GET /test HTTP/1.1
                Host: localhost:8080
                User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0
                Accept: application/json""";
        HttpRequest httpRequest = HttpRequest.parse(request);
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
                GET HTTP/1.1
                Host: localhost:8080
                User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0
                Accept: application/json""";
        HttpRequest httpRequest = HttpRequest.parse(request);
        Assertions.assertNull(httpRequest);

        request =
                """
                GET /test HTTP/1.1
                Host=localhost:8080
                User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0
                Accept: application/json""";
        httpRequest = HttpRequest.parse(request);
        Assertions.assertNull(httpRequest);
    }
}
