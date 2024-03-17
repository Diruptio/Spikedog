package diruptio.spikedog;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

public record Module(
        Path file, URLClassLoader classLoader, List<Class<?>> classes, List<Listener> listeners) {}
