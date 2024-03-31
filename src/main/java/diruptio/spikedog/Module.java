package diruptio.spikedog;

import java.nio.file.Path;
import java.util.List;

public record Module(
        Path file,
        ModuleLoader.ModuleClassLoader classLoader,
        List<Class<?>> classes,
        List<Listener> listeners) {}
