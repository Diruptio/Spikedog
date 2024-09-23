package diruptio.spikedog.network;

import diruptio.spikedog.HttpRequest;
import diruptio.spikedog.HttpResponse;
import diruptio.spikedog.ServeTask;
import diruptio.spikedog.Spikedog;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.*;
import java.nio.charset.Charset;
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
        ServeTask task = new ServeTask(ctx.channel(), new HttpRequest(headersFrame, dataFrame), future);
        future.thenAccept(response -> {
            // Set response headers
            String contentType = response.getHeader("Content-Type");
            if (contentType != null && response.getCharset() != null) {
                response.setHeader("Content-Type", contentType + "; charset=" + response.getCharset());
            }
            response.setHeader("Content-Length", String.valueOf(response.getContentLength()));
            response.setHeader("Server", "Spikedog/" + Spikedog.VERSION.get());
            response.setHeader("Access-Control-Allow-Origin", "*");

            // Write headers
            DefaultHttp2Headers headers = new DefaultHttp2Headers();
            headers.status(String.valueOf(response.getStatusCode()));
            for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
                headers.add(entry.getKey().toLowerCase(), entry.getValue());
            }
            String content = response.getContent();
            ctx.write(new DefaultHttp2HeadersFrame(headers).stream(headersFrame.stream()));

            // Write data
            if (!content.isEmpty()) {
                ByteBuf byteBuf = ctx.alloc().buffer();
                Charset charset = response.getCharset();
                byteBuf.writeBytes(charset == null ? content.getBytes() : content.getBytes(charset));
                ctx.write(new DefaultHttp2DataFrame(byteBuf, true).stream(headersFrame.stream()));
            }
        });
        future.orTimeout(30, TimeUnit.SECONDS).thenRun(() -> {
            // Response timed out
            // thread.interrupt();
            HttpResponse response = new HttpResponse();
            response.setStatus(522, "Connection Timed Out");
            response.setHeader("Content-Type", "text/html");
            response.setContent("<h1>522 Connection Timed Out</h1>");
            future.complete(response);
        });
        task.run();
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
