package diruptio.spikedog.example;

import diruptio.spikedog.Listener;
import diruptio.spikedog.Module;
import diruptio.spikedog.Spikedog;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpMethod;
import java.net.InetSocketAddress;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class ExampleListener implements Listener {
    @Override
    public void onLoad(@NotNull Module self) {
        System.out.println("Loading Spikedog example module");

        // Hello World servlet
        Spikedog.addServlet("/hello-world", new ExampleServlet());

        // Only POST servlet:
        // This servlet only accepts POST requests
        Spikedog.addServlet(
                "/only-post",
                (request, response) -> {
                    response.setStatus(200, "OK");
                    response.setHeader("Content-Type", "text/plain");
                    response.setContent("This is a POST request");
                },
                HttpMethod.POST);
        // This servlet (with the same path) accepts all other requests
        Spikedog.addServlet("/only-post", (request, response) -> {
            response.setStatus(405, "Method Not Allowed");
            response.setHeader("Content-Type", "text/plain");
            response.setContent("This endpoint only accepts POST requests");
        });
    }

    @Override
    public void onLoaded(@NotNull Module self) {
        System.out.println("Loaded Spikedog example module");

        // Here you can do something after all modules have been loaded
    }

    @Override
    public void onUnload() {
        System.out.println("Unloading Spikedog example module");
    }

    @Override
    public boolean allowConnection(@NotNull Channel client) {
        // Don't allow the request if the client is from a specific IP address
        return !((InetSocketAddress) client.remoteAddress()).getHostString().equals("123.123.123.123");
    }
}
