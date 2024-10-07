package diruptio.spikedog.info;

import diruptio.spikedog.*;
import diruptio.spikedog.Module;
import diruptio.spikedog.ModuleLoader;
import diruptio.spikedog.Spikedog;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
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

            CharSequence authorization = request.header(HttpHeaderNames.AUTHORIZATION);
            if (authorization == null || !auth.contentEquals(authorization)) {
                // Unauthorized
                response.status(HttpResponseStatus.UNAUTHORIZED);
                response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_HTML);
                response.header(HttpHeaderNames.WWW_AUTHENTICATE, "Basic charset=\"UTF-8\"");
                response.content("<h1>Unauthorized</h1>");
                return;
            }
        }

        // Success
        response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_HTML);
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
