package diruptio.spikedog;

import diruptio.spikedog.logging.SpikedogLogger;
import diruptio.spikedog.network.HttpServerChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.logging.*;
import org.jetbrains.annotations.NotNull;

public class Spikedog {
    public static final Logger LOGGER = new SpikedogLogger();
    public static final Supplier<String> VERSION = () -> BuildConstants.VERSION;
    public static final Path MODULES_DIRECTORY = Path.of("modules");
    private static final List<Servlet> servlets = new ArrayList<>();

    public static void main(String[] args) {
        LOGGER.setLevel(Level.INFO);
        listen(8080, "0.0.0.0", true);
    }

    /**
     * Starts the server and listens for incoming connections.
     *
     * @param port The port to listen on
     * @param bindAddress The address to bind to
     * @param loadModules Whether to load modules from the modules directory
     */
    public static void listen(int port, @NotNull String bindAddress, boolean loadModules) {
        try {
            // Start server
            EventLoopGroup group = Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
            bootstrap.group(group);
            bootstrap.channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class);
            bootstrap.childHandler(new HttpServerChannelInitializer());
            Channel channel = bootstrap.bind(port).sync().channel();
            LOGGER.info("Spikedog started on %s:%s".formatted(bindAddress, port));

            // Load modules
            if (loadModules) {
                ModuleLoader.loadModules(MODULES_DIRECTORY);
            }

            channel.closeFuture().sync();
            group.shutdownGracefully();
        } catch (Throwable exception) {
            LOGGER.log(Level.SEVERE, "Spikedog crashed", exception);
            System.exit(1);
        }
    }

    public static @NotNull List<Servlet> getServlets() {
        return servlets;
    }

    public static void addServlet(
            @NotNull String path, @NotNull BiConsumer<HttpRequest, HttpResponse> servlet, @NotNull String... methods) {
        servlets.add(new Servlet(path, servlet, methods));
    }

    public record Servlet(String path, BiConsumer<HttpRequest, HttpResponse> servlet, String[] methods) {}
}
