package diruptio.spikedog;

import diruptio.spikedog.logging.SpikedogLogger;
import diruptio.spikedog.network.EncryptedChannelInitializer;
import diruptio.spikedog.network.UnencryptedChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.logging.*;
import javax.net.ssl.SSLException;
import org.jetbrains.annotations.NotNull;

/** The main class of Spikedog */
public class Spikedog {
    /** The main logger for Spikedog */
    public static final Logger LOGGER = new SpikedogLogger();
    /** The version of Spikedog */
    public static final Supplier<String> VERSION = () -> BuildConstants.VERSION;
    /** The directory where modules are stored */
    public static final Path MODULES_DIRECTORY = Path.of("modules");

    private static final List<Servlet> servlets = new ArrayList<>();

    /**
     * The main method of Spikedog
     *
     * @param args The command line arguments
     */
    public static void main(String[] args) {
        LOGGER.setLevel(Level.INFO);
        listen(8080, "0.0.0.0", true, true);
    }

    private Spikedog() {}

    /**
     * Starts the server and listens for incoming connections.
     *
     * @param port The port to listen on
     * @param bindAddress The address to bind to
     * @param useSsl Whether to use SSL. If true, a self-signed SSL certificate will be created to encrypt connections
     *     and HTTP 2 will be supported. Otherwise, only HTTP 1 will be supported.
     * @param loadModules Whether to load modules from the modules directory
     */
    public static void listen(int port, @NotNull String bindAddress, boolean useSsl, boolean loadModules) {
        try {
            // Start server
            EventLoopGroup group = Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
            bootstrap.group(group);
            bootstrap.channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class);
            if (useSsl) {
                bootstrap.childHandler(new EncryptedChannelInitializer(createSslContext()));
            } else {
                bootstrap.childHandler(new UnencryptedChannelInitializer());
            }
            Channel channel = bootstrap.bind(port).sync().channel();

            StringBuilder url = new StringBuilder("http");
            if (useSsl) url.append("s");
            url.append("://").append(bindAddress);
            if (useSsl ? port != 443 : port != 80) url.append(":").append(port);
            LOGGER.info("Spikedog listens on " + url);

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

    private static @NotNull SslContext createSslContext() {
        try {
            SslProvider provider =
                    SslProvider.isAlpnSupported(SslProvider.OPENSSL) ? SslProvider.OPENSSL : SslProvider.JDK;
            SelfSignedCertificate certificate = new SelfSignedCertificate();
            return SslContextBuilder.forServer(certificate.certificate(), certificate.privateKey())
                    .sslProvider(provider)
                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                            ApplicationProtocolConfig.Protocol.ALPN,
                            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                            ApplicationProtocolNames.HTTP_2,
                            ApplicationProtocolNames.HTTP_1_1))
                    .build();
        } catch (SSLException | CertificateException e) {
            throw new RuntimeException(e);
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
