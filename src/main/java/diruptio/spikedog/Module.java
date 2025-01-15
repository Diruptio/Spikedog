package diruptio.spikedog;

import diruptio.spikedog.util.SharedClassLoader;
import java.nio.file.Path;
import java.util.List;

public record Module(Path file, SharedClassLoader classLoader, List<Class<?>> classes, List<Listener> listeners) {}
