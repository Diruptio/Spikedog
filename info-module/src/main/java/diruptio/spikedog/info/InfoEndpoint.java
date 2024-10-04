package diruptio.spikedog.info;

import diruptio.spikedog.*;
import diruptio.spikedog.Module;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class InfoEndpoint implements HttpEndpoint {
    @Endpoint(path = "/info")
    @Override
    public void handle(@NotNull HttpRequest request, @NotNull HttpResponse response) {
        // Authorization
        if (InfoModule.getConfig().getBoolean("authorization")) {
            String password = ":" + InfoModule.getConfig().getString("password");
            byte[] bytes = password.getBytes(StandardCharsets.UTF_8);
            String auth = "Basic " + Base64.getEncoder().encodeToString(bytes);

            if (!auth.equals(request.header("Authorization"))) {
                // Unauthorized
                response.status(HttpResponseStatus.UNAUTHORIZED);
                response.header("Content-Type", "text/html");
                response.header("WWW-Authenticate", "Basic charset=\"UTF-8\"");
                response.content("<h1>Unauthorized</h1>");
                return;
            }
        }

        // Success
        response.header("Content-Type", "text/html");
        StringBuilder content = new StringBuilder("<html>");
        content.append("<head><title>Spikedog Info</title></head>");
        content.append("<body>");
        content.append("<h1>Spikedog Info</h1>");
        content.append("<b>Version:</b> ").append(Spikedog.VERSION.get()).append("<hr>");
        content.append("<b>Modules:</b><ul>");
        List<Module> modules = new ArrayList<>(ModuleLoader.getModules());
        modules.sort(Comparator.comparing(module -> module.file().getFileName().toString()));
        for (Module module : modules) {
            content.append("<li>").append(module.file().getFileName()).append("</li>");
        }
        content.append("</ul><hr><b>Endpoints:</b><ul>");
        List<Endpoint> endpoints = new ArrayList<>(
                Spikedog.getDefaultEndpointProvider().getEndpoints().keySet());
        endpoints.sort(Comparator.comparing(Endpoint::path));
        for (Endpoint endpoint : endpoints) {
            content.append("<li>").append(endpoint.path()).append("</li>");
        }
        content.append("</body>");
        content.append("</html>");
        response.content(content);
    }
}
