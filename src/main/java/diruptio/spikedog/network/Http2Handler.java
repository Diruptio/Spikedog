package diruptio.spikedog.network;

import diruptio.spikedog.HttpRequest;
import diruptio.spikedog.HttpResponse;
import diruptio.spikedog.ServeTask;
import diruptio.spikedog.Spikedog;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Handles HTTP/2 requests. */
public class Http2Handler extends ChannelDuplexHandler {
    private final Map<ChannelHandlerContext, Http2HeadersFrame> headers = new HashMap<>();

    /** Creates a new HTTP/2 handler. */
    public Http2Handler() {}

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2HeadersFrame headersFrame) {
            if (headersFrame.isEndStream()) {
                complete(ctx, headersFrame, null);
            } else {
                headers.put(ctx, headersFrame);
            }
        } else if (msg instanceof Http2DataFrame dataFrame) {
            Http2HeadersFrame headersFrame = headers.remove(ctx);
            if (headersFrame != null) {
                complete(ctx, headersFrame, dataFrame);
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }

    private void complete(
            @NotNull ChannelHandlerContext ctx,
            @NotNull Http2HeadersFrame headersFrame,
            @Nullable Http2DataFrame dataFrame) {
        CompletableFuture<HttpResponse> future = new CompletableFuture<>();
        future.thenAccept(response -> {
            // Set response headers
            String contentType = response.header("Content-Type");
            if (contentType != null && response.charset() != null) {
                response.header("Content-Type", contentType + "; charset=" + response.charset());
            }
            response.header("Content-Length", String.valueOf(response.getContentLength()));
            response.header("Server", "Spikedog/" + Spikedog.VERSION.get());
            response.header("Access-Control-Allow-Origin", "*");

            // Write headers
            DefaultHttp2Headers headers = new DefaultHttp2Headers();
            headers.status(response.status().codeAsText());
            for (Map.Entry<String, String> entry : response.headers().entrySet()) {
                headers.add(entry.getKey().toLowerCase(), entry.getValue());
            }
            ctx.write(new DefaultHttp2HeadersFrame(headers).stream(headersFrame.stream()));

            // Write data
            if (response.content().readableBytes() > 0) {
                ctx.write(new DefaultHttp2DataFrame(response.content(), true).stream(headersFrame.stream()));
            }
        });
        future.orTimeout(30, TimeUnit.SECONDS).thenRun(() -> {
            // Response timed out
            HttpResponse response = new HttpResponse("HTTP/2");
            response.status(new HttpResponseStatus(522, "Connection Timed Out"));
            response.header("Content-Type", "text/html");
            response.content("<h1>522 Connection Timed Out</h1>");
            future.complete(response);
        });
        HttpRequest request;
        try {
            request = new HttpRequest(headersFrame, dataFrame);
        } catch (Throwable ignored) {
            HttpResponse response = new HttpResponse("HTTP/2");
            response.status(HttpResponseStatus.BAD_REQUEST);
            response.content("<h1>400 Bad Request</h1>");
            future.complete(response);
            return;
        }
        new ServeTask(ctx.channel(), request, future).run();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.flush(ctx);
    }
}
