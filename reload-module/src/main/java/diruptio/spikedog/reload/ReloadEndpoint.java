package diruptio.spikedog.reload;

import diruptio.spikedog.*;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.jetbrains.annotations.NotNull;

public class ReloadEndpoint implements HttpEndpoint {
    @Endpoint(path = "/reload")
    @Override
    public void handle(@NotNull HttpRequest request, @NotNull HttpResponse response) {
        // Authorization
        if (ReloadModule.getConfig().getBoolean("authorization")) {
            String password = ":" + ReloadModule.getConfig().getString("password");
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

        // Authorized
        response.header("Content-Type", "text/html");
        response.content("<h1>Reloading modules...</h1>");

        // Reload
        ServeTask task = ServeTask.getTaskByRequest(request);
        if (task != null) task.getFuture().thenRun(new ReloadTask());
    }
}
