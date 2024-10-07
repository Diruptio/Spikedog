package diruptio.spikedog.reload;

import diruptio.spikedog.*;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
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

        // Authorized
        response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_HTML);
        response.content("<h1>Reloading modules...</h1>");

        // Reload
        ServeTask task = ServeTask.getTaskByRequest(request);
        if (task != null) task.getFuture().thenRun(new ReloadTask());
    }
}
