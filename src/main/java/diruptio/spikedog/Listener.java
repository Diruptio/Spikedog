package diruptio.spikedog;

import java.nio.channels.SocketChannel;
import org.jetbrains.annotations.NotNull;

public interface Listener {
    default void onLoad(@NotNull Module self) {}

    default void onUnload() {}

    default boolean onConnect(@NotNull SocketChannel client) {
        return true;
    }
}
