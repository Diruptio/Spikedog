package diruptio.spikedog.info;

import diruptio.spikedog.Listener;
import diruptio.spikedog.Module;
import diruptio.spikedog.Spikedog;
import diruptio.spikedog.config.Config;
import java.nio.file.Path;

public class InfoModule implements Listener {
    private static Config config;

    @Override
    public void onLoad(Module self) {
        Path configFile = self.file().resolveSibling("info").resolve("config.yml");
        config = new Config(configFile, Config.Type.YAML);
        if (!config.contains("authorization")) {
            config.set("authorization", false);
            config.save();
        }
        if (!config.contains("password")) {
            config.set("password", "YOUR_PASSWORD");
            config.save();
        }

        Spikedog.addServlet("/info", new InfoServlet());
    }

    public static Config getConfig() {
        return config;
    }
}
