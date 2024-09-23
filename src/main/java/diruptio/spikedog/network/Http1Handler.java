package diruptio.spikedog.network;

import diruptio.spikedog.HttpRequest;
import diruptio.spikedog.HttpResponse;
import diruptio.spikedog.ServeTask;
import diruptio.spikedog.Spikedog;
import io.netty.buffer.ByteBuf;
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
        future.thenAccept(spikedogResponse -> {
            // Response received
            ServeTask.getTasks().remove(task);

            // Set response headers
            String contentType = spikedogResponse.getHeader("Content-Type");
            if (contentType != null && spikedogResponse.getCharset() != null) {
                spikedogResponse.setHeader("Content-Type", contentType + "; charset=" + spikedogResponse.getCharset());
            }
            spikedogResponse.setHeader("Content-Length", String.valueOf(spikedogResponse.getContentLength()));
            spikedogResponse.setHeader("Server", "Spikedog/" + Spikedog.VERSION.get());
            spikedogResponse.setHeader("Access-Control-Allow-Origin", "*");

            // Write response
            HttpResponseStatus status =
                    new HttpResponseStatus(spikedogResponse.getStatusCode(), spikedogResponse.getStatusMessage());
            ByteBuf content = ctx.alloc().buffer();
            content.writeBytes(spikedogResponse.getContent().getBytes());
            FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
            ctx.writeAndFlush(nettyResponse).addListener(ChannelFutureListener.CLOSE);
        });
        future.orTimeout(30, TimeUnit.SECONDS).thenRun(() -> {
            // Response timed out
            HttpResponse response = new HttpResponse();
            response.setStatus(522, "Connection Timed Out");
            response.setHeader("Content-Type", "text/html");
            response.setContent("<h1>522 Connection Timed Out</h1>");
            future.complete(response);
        });
        task.run();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
