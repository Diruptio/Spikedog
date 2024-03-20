package diruptio.spikedog.example;

import diruptio.spikedog.HttpRequest;
import diruptio.spikedog.HttpResponse;
import java.util.function.BiConsumer;

public class ExampleServlet implements BiConsumer<HttpRequest, HttpResponse> {
    public void accept(HttpRequest request, HttpResponse response) {
        response.setStatus(200, "OK");
        response.setHeader("Content-Type", "text/plain");
        response.setContent("Hello World!");
    }
}
