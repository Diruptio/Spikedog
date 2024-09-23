package diruptio.spikedog.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/** Initializes a channel without SSL encryption. */
public class UnencryptedChannelInitializer extends ChannelInitializer<SocketChannel> {
    /** Creates a new {@link UnencryptedChannelInitializer}. */
    public UnencryptedChannelInitializer() {}

    @Override
    protected void initChannel(SocketChannel channel) {
        channel.pipeline().addLast(new HttpServerCodec());
        channel.pipeline().addLast(new SimpleChannelInboundHandler<HttpMessage>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) {
                ctx.pipeline().addAfter(ctx.name(), null, new Http1Handler());
                ctx.pipeline().replace(this, null, new HttpObjectAggregator(1024 * 100));
                ctx.fireChannelRead(msg);
            }
        });
    }
}
