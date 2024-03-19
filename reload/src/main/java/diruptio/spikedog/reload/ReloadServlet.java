package diruptio.spikedog.reload;

import diruptio.spikedog.HttpRequest;
import diruptio.spikedog.HttpResponse;
import diruptio.spikedog.Reload;
import diruptio.spikedog.ServeThread;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.BiConsumer;

public class ReloadServlet implements BiConsumer<HttpRequest, HttpResponse> {
    public void accept(HttpRequest request, HttpResponse response) {
        // Authorization
        if (ReloadModule.getConfig().getBoolean("authorization")) {
            String password = ":" + ReloadModule.getConfig().getString("password");
            byte[] bytes = password.getBytes(StandardCharsets.UTF_8);
            String auth = "Basic " + Base64.getEncoder().encodeToString(bytes);

            if (!auth.equals(request.getHeader("Authorization"))) {
                // Unauthorized
                response.setStatus(401, "Unauthorized");
                response.setHeader("Content-Type", "text/html");
                response.setHeader("WWW-Authenticate", "Basic charset=\"UTF-8\"");
                response.setContent("<h1>Unauthorized</h1>");
                return;
            }
        }

        // Authorized
        response.setStatus(200, "OK");
        response.setHeader("Content-Type", "text/plain");
        response.setContent("<h1>Reloading modules...</h1>");

        // Reload
        ServeThread.runAfterServe(new Reload());
    }
}
