package diruptio.spikedog.network;

import diruptio.spikedog.HttpRequest;
import diruptio.spikedog.HttpResponse;
import diruptio.spikedog.ServeTask;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
        AtomicReference<ServeTask> task = new AtomicReference<>();
        future.thenAccept(response -> {
            // Response received
            ServeTask.getTasks().remove(task.get());

            // Write headers
            DefaultHttp2Headers headers = new DefaultHttp2Headers();
            headers.status(response.status().codeAsText());
            for (Map.Entry<CharSequence, CharSequence> entry :
                    response.headers().entrySet()) {
                headers.add(entry.getKey().toString().toLowerCase(), entry.getValue());
            }
            ctx.write(new DefaultHttp2HeadersFrame(headers).stream(headersFrame.stream()));

            // Write data
            if (response.content().readableBytes() > 0) {
                ctx.write(new DefaultHttp2DataFrame(response.content(), true).stream(headersFrame.stream()));
            }
        });
        future.orTimeout(30, TimeUnit.SECONDS).thenRun(() -> {
            // Response timed out
            ServeTask.getTasks().remove(task.get());
            HttpResponse response = new HttpResponse("HTTP/2");
            response.status(new HttpResponseStatus(522, "Connection Timed Out"));
            response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_HTML);
            response.content("<h1>522 Connection Timed Out</h1>");
            future.complete(response);
        });
        try {
            task.set(new ServeTask(ctx.channel(), new HttpRequest(headersFrame, dataFrame), future));
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
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.flush(ctx);
    }
}
