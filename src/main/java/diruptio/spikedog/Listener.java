package diruptio.spikedog;

import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

/** A listener for a module. */
public interface Listener {
    /**
     * Called when the module is loaded. The Module should initialize everything that other modules depend on.
     *
     * @param self The module's listener
     */
    default void onLoad(@NotNull Module self) {}

    /**
     * Called when every module is loaded. The Module should initialize everything that depends on other modules.
     *
     * @param self The module's listener
     */
    default void onLoaded(@NotNull Module self) {}

    /** Called when the module is unloaded. The Module should clean up everything that was initialized in onLoad. */
    default void onUnload() {}

    /**
     * Called when a client connects to the server. The Module should return true if the connection is allowed.
     *
     * @param client The client's socket channel
     * @return {@code true} if the connection is allowed, {@code false} otherwise
     */
    default boolean allowConnection(@NotNull Channel client) {
        return true;
    }
}
