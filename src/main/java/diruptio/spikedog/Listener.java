package diruptio.spikedog;

public interface Listener {
    public default void onLoad(Jar self) {}

    public default void onUnload(Jar self) {}
}
