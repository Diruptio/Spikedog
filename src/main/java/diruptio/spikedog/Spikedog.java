package diruptio.spikedog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Spikedog {
    public static final int PORT = 8080;

    public static void main(String[] args) {
        try (Selector selector = Selector.open();
                ServerSocketChannel serverSocket = ServerSocketChannel.open()) {

            serverSocket.bind(new InetSocketAddress("0.0.0.0", PORT));
            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                selector.select();
                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();

                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();

                    if (key.isAcceptable()) {
                        SocketChannel client = serverSocket.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                    }

                    if (key.isReadable()) {
                        try (SocketChannel client = (SocketChannel) key.channel()) {
                            SocketAddress socketAddress = client.getRemoteAddress();
                            String address = ((InetSocketAddress) socketAddress).getHostString();

                            ByteBuffer buffer = ByteBuffer.allocate(256);
                            client.read(buffer);
                            buffer.flip();
                            HttpRequest request = HttpRequest.parse(new String(buffer.array()));
                            buffer.clear();

                            HttpResponse response = new HttpResponse();
                            if (request == null) {
                                System.out.printf(
                                        "Received request from %s: Bad Request\n", address);
                                response.setStatus(400, "Bad Request");
                            } else {
                                System.out.printf(
                                        "Received request from %s: %s %s\n",
                                        address, request.getMethod(), request.getPath());
                                // TODO: Servlets
                            }

                            String responseStr = response.toString();
                            buffer = ByteBuffer.allocate(responseStr.length());
                            buffer.put(responseStr.getBytes(StandardCharsets.UTF_8));
                            buffer.flip();
                            while (buffer.hasRemaining()) client.write(buffer);
                            buffer.clear();
                        }
                    }
                    selectedKeys.remove();
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
