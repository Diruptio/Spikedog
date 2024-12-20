package diruptio.spikedog;

import com.google.common.collect.Lists;
import com.google.common.reflect.ClassPath;
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
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.*;
import javax.net.ssl.SSLException;
import org.apache.commons.cli.*;
import org.jetbrains.annotations.NotNull;

/** The main class of Spikedog */
public class Spikedog {
    /** The main logger for Spikedog */
    public static final Logger LOGGER = new SpikedogLogger();
    /** The version of Spikedog */
    public static final Supplier<String> VERSION = () -> BuildConstants.VERSION;
    /** The directory where modules are stored */
    public static final Path MODULES_DIRECTORY = Path.of("modules");

    public static int MAX_CONTENT_LENGTH = 1024 * 1024 * 1024; // 1GB

    private static final DefaultEndpointProvider defaultEndpointProvider = new DefaultEndpointProvider();
    private static final List<EndpointProvider> endpointProviders = Lists.newArrayList(defaultEndpointProvider);

    /**
     * The main method of Spikedog
     *
     * @param args The command line arguments
     */
    public static void main(String[] args) {
        LOGGER.setLevel(Level.INFO);
        String bindAddress;
        int port;
        boolean useSsl;
        boolean loadModules;
        List<Path> extraModules = new ArrayList<>();
        Options options = new Options();
        options.addOption(
                Option.builder().option("h").longOpt("help").desc("Show help").build());
        options.addOption(Option.builder()
                .option("b")
                .longOpt("bind-address")
                .hasArg()
                .argName("address")
                .desc("The bind address (default: 0.0.0.0)")
                .build());
        options.addOption(Option.builder()
                .option("p")
                .longOpt("port")
                .hasArg()
                .argName("port")
                .desc("The port (default: 8080)")
                .build());
        options.addOption(Option.builder()
                .option("s")
                .longOpt("use-ssl")
                .hasArg()
                .argName("true/false")
                .desc("Whether to use ssl (default: true)")
                .build());
        options.addOption(Option.builder()
                .option("l")
                .longOpt("load-modules")
                .hasArg()
                .argName("true/false")
                .desc("Whether to load modules (default: true)")
                .build());
        options.addOption(Option.builder()
                .option("e")
                .longOpt("extra-module")
                .hasArg()
                .argName("file")
                .desc("File to load as modules (even if load-modules is false)")
                .build());
        try {
            CommandLine commandLine = new DefaultParser().parse(options, args);
            if (commandLine.hasOption("help")) {
                new HelpFormatter().printHelp("Spikedog.jar [OPTIONS]", options);
                return;
            }
            bindAddress = commandLine.getOptionValue("host", "0.0.0.0");
            port = Integer.parseInt(commandLine.getOptionValue("port", "8080"));
            useSsl = Boolean.parseBoolean(commandLine.getOptionValue("use-ssl", "false"));
            loadModules = Boolean.parseBoolean(commandLine.getOptionValue("load-modules", "true"));
            if (commandLine.hasOption("extra-module")) {
                for (String str : commandLine.getOptionValues("extra-module")) {
                    extraModules.add(Path.of(str));
                }
            }
        } catch (ParseException | NumberFormatException ignored) {
            new HelpFormatter().printHelp("Spikedog.jar [OPTIONS]", options);
            return;
        }
        listen(bindAddress, port, useSsl, loadModules, extraModules);
    }

    private Spikedog() {}

    /**
     * Starts the server and listens for incoming connections.
     *
     * @param bindAddress The address to bind to
     * @param port The port to listen on
     * @param useSsl Whether to use SSL. If {@code true}, a self-signed SSL certificate will be created to encrypt
     *     connections, and HTTP 2 will be supported. Otherwise, only HTTP 1 will be supported.
     * @param loadModules Whether to load modules from the modules directory
     * @param extraModules Files to load as modules (even if loadModules is {@code false})
     */
    public static void listen(
            final @NotNull String bindAddress,
            final int port,
            final boolean useSsl,
            final boolean loadModules,
            final @NotNull List<Path> extraModules) {
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

            ModuleLoader.getExtraModules().addAll(extraModules);

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
            return SslContextBuilder.forServer(certificate.certificate(), certificate.privateKey(), null)
                    .sslProvider(provider)
                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                            ApplicationProtocolConfig.Protocol.ALPN,
                            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                            ApplicationProtocolNames.HTTP_2))
                    .build();
        } catch (SSLException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Registers an endpoint with the specified path and methods.
     *
     * @param path The path
     * @param endpoint The endpoint
     * @param methods The methods ({@code "GET"}, {@code "POST"}, etc.) or an empty array for all methods
     */
    public static void register(@NotNull String path, @NotNull HttpEndpoint endpoint, @NotNull String... methods) {
        defaultEndpointProvider.getEndpoints().put(new RegisteredEndpoint(path, methods), endpoint);
    }

    /**
     * Registers all endpoints in the specified object.
     *
     * @param object The object
     */
    public static void register(@NotNull Object object) {
        for (Method method : object.getClass().getMethods()) {
            Endpoint endpoint = method.getAnnotation(Endpoint.class);
            if (endpoint != null
                    && method.getParameterCount() == 2
                    && method.getParameterTypes()[0] == HttpRequest.class
                    && method.getParameterTypes()[1] == HttpResponse.class) {
                register(endpoint.path(), new RegisteredHttpEndpoint(method, object), endpoint.methods());
            }
        }
    }

    /**
     * Registers all endpoints in the specified package.
     *
     * @param packageName The package name
     */
    public static void registerPackage(@NotNull String packageName) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            for (ClassPath.ClassInfo classInfo : ClassPath.from(classLoader).getTopLevelClassesRecursive(packageName)) {
                Class<?> clazz = classInfo.load();
                for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                    if (constructor.getParameterCount() == 0) {
                        register(constructor.newInstance());
                        break;
                    }
                }
            }
        } catch (IOException | ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the default endpoint provider.
     *
     * @return The default endpoint provider
     */
    public static @NotNull DefaultEndpointProvider getDefaultEndpointProvider() {
        return defaultEndpointProvider;
    }

    /**
     * Gets the endpoint providers.
     *
     * @return The endpoint providers
     */
    public static @NotNull List<EndpointProvider> getEndpointProviders() {
        return endpointProviders;
    }

    private record RegisteredEndpoint(@NotNull String path, @NotNull String[] methods) implements Endpoint {
        @Override
        public Class<? extends Annotation> annotationType() {
            return Endpoint.class;
        }
    }

    private record RegisteredHttpEndpoint(@NotNull Method method, @NotNull Object object) implements HttpEndpoint {
        @Override
        public void handle(@NotNull HttpRequest request, @NotNull HttpResponse response) {
            try {
                method.invoke(object, request, response);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
