package diruptio.spikedog;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HttpResponseTest {
    @Test
    public void testToString() {
        HttpResponse response = new HttpResponse();
        response.setStatus(200, "OK");
        response.setHeader("Content-Type", "text/plain");
        response.setContent("Hello, World!");

        Assertions.assertEquals("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nHello, World!", response.toString());
    }
}
