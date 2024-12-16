package diruptio.spikedog.network;

import diruptio.spikedog.Spikedog;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;

/**
 * A handler that chooses between HTTP/2 and HTTP/1.x based on the protocol that is chosen by
 * {@link ApplicationProtocolNegotiationHandler}.
 */
public class Http1OrHttp2Handler extends ApplicationProtocolNegotiationHandler {
    /** Creates a new {@link Http1OrHttp2Handler}. */
    public Http1OrHttp2Handler() {
        super(ApplicationProtocolNames.HTTP_2);
    }

    @Override
    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
        if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
            ctx.pipeline().addLast(Http2FrameCodecBuilder.forServer().build(), new Http2Handler());
        } else if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
            ctx.pipeline()
                    .addLast(
                            new HttpServerCodec(),
                            new HttpObjectAggregator(Spikedog.MAX_CONTENT_LENGTH),
                            new Http1Handler());
        } else {
            throw new IllegalStateException("Unknown protocol: " + protocol);
        }
    }
}
