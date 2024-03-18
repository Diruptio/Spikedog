package diruptio.spikedog.reload;

import diruptio.spikedog.*;
import diruptio.spikedog.Module;
import diruptio.spikedog.config.Config;

public class ReloadModule implements Listener {
    private static Config config;

    @Override
    public void onLoad(Module self) {
        config =
                new Config(
                        self.file().resolveSibling("reload").resolve("config.yml"),
                        Config.Type.YAML);
        if (!config.contains("authorization")) {
            config.set("authorization", false);
            config.save();
        }
        if (!config.contains("password")) {
            config.set("password", "YOUR_PASSWORD");
            config.save();
        }

        Spikedog.addServlet("/reload", new ReloadServlet());
    }

    public static Config getConfig() {
        return config;
    }
}
