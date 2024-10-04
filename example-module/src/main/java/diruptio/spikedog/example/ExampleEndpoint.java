package diruptio.spikedog.example;

import diruptio.spikedog.Endpoint;
import diruptio.spikedog.HttpRequest;
import diruptio.spikedog.HttpResponse;

public class ExampleEndpoint {
    @Endpoint(path = "/hello-world")
    public void handle(HttpRequest request, HttpResponse response) {
        response.content("Hello World!");
    }
}
