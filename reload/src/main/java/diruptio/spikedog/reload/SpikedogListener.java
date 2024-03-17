package diruptio.spikedog.reload;

import diruptio.spikedog.Listener;
import diruptio.spikedog.Module;
import diruptio.spikedog.Spikedog;

public class SpikedogListener implements Listener {
    @Override
    public void onLoad(Module self) {
        System.out.println("Loading Spikedog reload module");

        Spikedog.addServlet(
                "/reload",
                (request, response) -> {
                    response.setStatus(200, "OK");
                    response.setHeader("Content-Type", "text/plain");
                    response.setContent("Reloading modules...");
                    try {

                        response.setContent("Modules reloaded");
                    } catch (Throwable exception) {
                        response.setStatus(500, "Internal Server Error");
                        response.setContent("An error ocurred while reloading modules");
                        exception.printStackTrace(System.err);
                    }
                });
    }

    @Override
    public void onUnload() {
        System.out.println("Unloading example Spikedog project");
    }
}
