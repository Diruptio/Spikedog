package diruptio.spikedog.example;

import diruptio.spikedog.Listener;
import diruptio.spikedog.Module;
import diruptio.spikedog.Spikedog;

public class SpikedogListener implements Listener {
    @Override
    public void onLoad(Module self) {
        System.out.println("Loading Spikedog example module");

        // Hello World servlet
        Spikedog.addServlet("/hello-world", new HelloWorldServlet());

        // Only POST servlet:
        // This servlet only accepts POST requests
        Spikedog.addServlet(
                "/only-post",
                (request, response) -> {
                    response.setStatus(200, "OK");
                    response.setHeader("Content-Type", "text/plain");
                    response.setContent("This is a POST request");
                },
                "POST");
        // This servlet (with the same path) accepts all other requests
        Spikedog.addServlet(
                "/only-post",
                (request, response) -> {
                    response.setStatus(405, "Method Not Allowed");
                    response.setHeader("Content-Type", "text/plain");
                    response.setContent("This endpoint only accepts POST requests");
                });
    }

    @Override
    public void onUnload() {
        System.out.println("Unloading Spikedog example module");
    }
}
