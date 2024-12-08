package diruptio.spikedog;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class ModuleLoader {
    private static final List<Path> extraModules = new ArrayList<>();
    private static final List<Module> modules = new ArrayList<>();

    public static void loadModules(final @NotNull Path directory) throws IOException {
        if (!Files.exists(directory)) Files.createDirectories(directory);
        else if (!Files.isDirectory(directory)) throw new IOException(directory + " is not a directory");
        try (Stream<Path> files = Stream.concat(extraModules.stream(), Files.list(directory))) {
            List<Path> paths = sortPaths(directory, new ArrayList<>(files.toList()));
            for (Path path : paths) {
                if (Files.isDirectory(path) || !path.getFileName().toString().endsWith(".jar")) {
                    continue;
                }
                try {
                    modules.add(loadModule(path));
                } catch (Throwable exception) {
                    Spikedog.LOGGER.log(Level.SEVERE, "An error ocurred while loading " + directory, exception);
                }
            }
        }
        for (Module module : modules) {
            try {
                module.listeners().forEach(listener -> listener.onLoad(module));
            } catch (Throwable exception) {
                Spikedog.LOGGER.log(Level.SEVERE, "An error ocurred while loading " + module.file(), exception);
            }
        }
        for (Module module : modules) {
            try {
                module.listeners().forEach(listener -> listener.onLoaded(module));
            } catch (Throwable exception) {
                Spikedog.LOGGER.log(Level.SEVERE, "An error ocurred while loading " + module.file(), exception);
            }
        }
    }

    private static List<Path> sortPaths(@NotNull Path directory, @NotNull List<Path> paths) {
        List<String> order = new ArrayList<>();
        Path orderFile = directory.resolve("order.txt");
        if (Files.exists(orderFile) && Files.isRegularFile(orderFile)) {
            try (Stream<String> lines = Files.lines(orderFile)) {
                order.addAll(lines.toList());
            } catch (Throwable ignored) {
            }
        }

        List<Path> sorted = new ArrayList<>();
        for (String line : order) {
            paths.removeIf(path -> {
                if (path.getFileName().toString().matches(line)) {
                    sorted.add(path);
                    return true;
                } else return false;
            });
        }
        sorted.addAll(paths);
        return sorted;
    }

    public static @NotNull Module loadModule(@NotNull Path file) throws IOException, ClassNotFoundException {
        URL[] urls;
        try {
            urls = new URL[] {new URI("jar:file:" + file + "!/").toURL()};
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        try (JarFile jarFile = new JarFile(file.toFile())) {
            ClassLoader parent = ClassLoader.getSystemClassLoader();
            ModuleClassLoader classLoader = new ModuleClassLoader(urls, parent);
            Module module = new Module(file, classLoader, new ArrayList<>(), new ArrayList<>());

            // Load classes
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;
                String classFileName =
                        entry.getName().substring(0, entry.getName().length() - 6);
                String className = classFileName.replace("/", ".");
                try {
                    module.classes().add(Class.forName(className));
                } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
                    try {
                        module.classes().add(classLoader.loadClass(className));
                    } catch (NoClassDefFoundError ignored2) {
                    }
                }
            }

            // Load listeners
            for (Class<?> clazz : module.classes()) {
                if (Listener.class.isAssignableFrom(clazz)) {
                    try {
                        Listener listener =
                                (Listener) clazz.getDeclaredConstructor().newInstance();
                        module.listeners().add(listener);
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
            }

            Spikedog.LOGGER.info("Loaded module " + file.getFileName());
            return module;
        }
    }

    public static void unloadModules() throws IOException {
        for (Module module : modules) {
            try {
                module.listeners().forEach(Listener::onUnload);
            } catch (Throwable exception) {
                Spikedog.LOGGER.log(
                        Level.SEVERE, "Failed to unload module " + module.file().getFileName());
            }
        }
        modules.forEach(module -> module.listeners().clear());
        for (Module module : modules) {
            module.classLoader().close();
            Spikedog.LOGGER.info("Unloaded module " + module.file().getFileName());
        }
        modules.clear();
    }

    public static @NotNull List<Path> getExtraModules() {
        return extraModules;
    }

    public static @NotNull List<Module> getModules() {
        return modules;
    }

    public static class ModuleClassLoader extends URLClassLoader {
        public ModuleClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            return internalLoadClass(name, resolve, true);
        }

        private Class<?> internalLoadClass(String name, boolean resolve, boolean checkOther)
                throws ClassNotFoundException {
            try {
                return super.loadClass(name, resolve);
            } catch (ClassNotFoundException exception) {
                if (checkOther) {
                    for (Module module : modules) {
                        try {
                            if (module.classLoader() != this) {
                                return module.classLoader().internalLoadClass(name, resolve, false);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
                throw exception;
            }
        }
    }
}
