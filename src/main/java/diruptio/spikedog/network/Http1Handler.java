package diruptio.spikedog.network;

import diruptio.spikedog.HttpRequest;
import diruptio.spikedog.HttpResponse;
import diruptio.spikedog.ServeTask;
import diruptio.spikedog.Spikedog;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Handles HTTP/1.x requests. */
public class Http1Handler extends SimpleChannelInboundHandler<FullHttpRequest> {
    /** Creates a new HTTP/1.x handler. */
    public Http1Handler() {}

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest nettyRequest) {
        // Handle Continue requests
        if (HttpUtil.is100ContinueExpected(nettyRequest)) {
            ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
            return;
        }

        CompletableFuture<HttpResponse> future = new CompletableFuture<>();
        ServeTask task = new ServeTask(ctx.channel(), new HttpRequest(nettyRequest), future);
        future.thenAccept(response -> {
            // Response received
            ServeTask.getTasks().remove(task);

            // Set response headers
            String contentType = response.header("Content-Type");
            if (contentType != null && response.charset() != null) {
                response.header("Content-Type", contentType + "; charset=" + response.charset());
            }
            response.header("Content-Length", String.valueOf(response.getContentLength()));
            response.header("Server", "Spikedog/" + Spikedog.VERSION.get());
            response.header("Access-Control-Allow-Origin", "*");

            // Write response
            FullHttpResponse nettyResponse =
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, response.status(), response.content());
            ctx.writeAndFlush(nettyResponse).addListener(ChannelFutureListener.CLOSE);
        });
        future.orTimeout(30, TimeUnit.SECONDS).thenRun(() -> {
            // Response timed out
            HttpResponse response =
                    new HttpResponse(nettyRequest.protocolVersion().text());
            response.status(new HttpResponseStatus(522, "Connection Timed Out"));
            response.header("Content-Type", "text/html");
            response.content("<h1>522 Connection Timed Out</h1>");
            future.complete(response);
        });
        task.run();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
