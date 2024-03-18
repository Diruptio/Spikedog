package diruptio.spikedog.reload;

import diruptio.spikedog.HttpRequest;
import diruptio.spikedog.HttpResponse;
import diruptio.spikedog.Reload;
import diruptio.spikedog.ServeThread;

import java.util.Base64;
import java.util.function.BiConsumer;

public class ReloadServlet implements BiConsumer<HttpRequest, HttpResponse> {
    public void accept(HttpRequest request, HttpResponse response) {
        // Authorization
        if (ReloadModule.getConfig().getBoolean("authorization")) {
            String auth = request.getHeader("Authorization");
            if (auth == null) {
                unauthorized(response);
                return;
            }
            try {
                String decoded = new String(Base64.getDecoder().decode(auth.replaceFirst("Basic ", "")));
                System.out.println(decoded);
            } catch (Throwable ignored) {
                unauthorized(response);
                return;
            }
        }

        response.setStatus(200, "OK");
        response.setHeader("Content-Type", "text/html");
        response.setContent("Reloading modules...");

        // Reload
        ServeThread.runAfterServe(new Reload());
    }

    private void unauthorized(HttpResponse response) {
        response.setStatus(401, "Unauthorized");
        response.setHeader("Content-Type", "text/html");
        response.setHeader("WWW-Authenticate", "Basic realm=\"Spikedog\", charset=\"UTF-8\"");
        response.setContent("<h1>Unauthorized</h1>");
    }
}