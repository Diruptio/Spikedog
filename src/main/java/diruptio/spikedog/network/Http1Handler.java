package diruptio.spikedog.network;

import diruptio.spikedog.HttpRequest;
import diruptio.spikedog.HttpResponse;
import diruptio.spikedog.ServeTask;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
        AtomicReference<ServeTask> task = new AtomicReference<>();
        future.thenAccept(response -> {
            // Response received
            ServeTask.getTasks().remove(task.get());

            // Write response
            FullHttpResponse nettyResponse =
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, response.status(), response.content());
            ctx.writeAndFlush(nettyResponse).addListener(ChannelFutureListener.CLOSE);
        });
        future.orTimeout(30, TimeUnit.SECONDS).thenRun(() -> {
            // Response timed out
            ServeTask.getTasks().remove(task.get());
            HttpResponse response =
                    new HttpResponse(nettyRequest.protocolVersion().text());
            response.status(new HttpResponseStatus(522, "Connection Timed Out"));
            response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_HTML);
            response.content("<h1>522 Connection Timed Out</h1>");
            future.complete(response);
        });
        try {
            task.set(new ServeTask(ctx.channel(), new HttpRequest(nettyRequest), future));
            ServeTask.getTasks().add(task.get());
            task.get().run();
        } catch (Throwable ignored) {
            HttpResponse response = new HttpResponse("HTTP/2");
            response.status(HttpResponseStatus.BAD_REQUEST);
            response.content("<h1>400 Bad Request</h1>");
            future.complete(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
