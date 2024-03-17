package diruptio.spikedog;

import java.nio.channels.SocketChannel;

public interface Listener {
    public default void onLoad(Module self) {}

    public default void onUnload() {}

    public default boolean onConnect(SocketChannel client) {
        return true;
    }
}
