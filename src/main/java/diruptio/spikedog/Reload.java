package diruptio.spikedog;

public class Reload implements Runnable {
    @Override
    public void run() {
        System.out.println("Reloading modules...");
        try {
            ModuleLoader.unloadModules();
            Spikedog.getServlets().clear();
            ModuleLoader.loadModules(Spikedog.MODULES_DIRECTORY);
        } catch (Throwable exception) {
            exception.printStackTrace(System.err);
        }
    }
}
