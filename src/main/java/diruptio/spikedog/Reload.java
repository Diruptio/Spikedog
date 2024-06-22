package diruptio.spikedog;

import java.util.logging.Level;

public class Reload implements Runnable {
    @Override
    public void run() {
        Spikedog.LOGGER.info("Reloading modules...");
        try {
            ModuleLoader.unloadModules();
            Spikedog.getServlets().clear();
            ModuleLoader.loadModules(Spikedog.MODULES_DIRECTORY);
        } catch (Throwable exception) {
            Spikedog.LOGGER.log(Level.SEVERE, "Failed to reload modules", exception);
        }
    }
}
