package diruptio.spikedog;

import java.nio.file.Files;
import java.util.logging.Level;

/** A task to reload Spikedog. */
public class ReloadTask implements Runnable {
    /** Creates a new reload task. */
    public ReloadTask() {}

    @Override
    public void run() {
        Spikedog.LOGGER.info("Reloading modules...");
        try {
            Spikedog.getDefaultEndpointProvider().getEndpoints().clear();
            Spikedog.getEndpointProviders().clear();
            ModuleLoader.unloadModules();
            Spikedog.getEndpointProviders().add(Spikedog.getDefaultEndpointProvider());
            ModuleLoader.loadModules(
                    Spikedog.MODULES_DIRECTORY.resolve("order.txt"), Files.list(Spikedog.MODULES_DIRECTORY));
        } catch (Throwable exception) {
            Spikedog.LOGGER.log(Level.SEVERE, "Failed to reload modules", exception);
        }
    }
}
