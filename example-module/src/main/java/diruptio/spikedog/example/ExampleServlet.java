package diruptio.spikedog.example;

import diruptio.spikedog.HttpRequest;
import diruptio.spikedog.HttpResponse;
import java.util.function.BiConsumer;

public class ExampleServlet implements BiConsumer<HttpRequest, HttpResponse> {
    public void accept(HttpRequest request, HttpResponse response) {
        response.content("Hello World!");
    }
}
