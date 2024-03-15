package diruptio.spikedog;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

public record Jar(Path file, URLClassLoader classLoader, List<Class<?>> classes) {}
