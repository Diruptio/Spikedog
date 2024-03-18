package diruptio.spikedog;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class ModuleLoader {
    private static final List<Module> modules = new ArrayList<>();

    public static void loadModules(@NotNull Path directory) throws IOException {
        if (!Files.exists(directory)) Files.createDirectories(directory);
        else if (!Files.isDirectory(directory))
            throw new IOException(directory + " is not a directory");
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.toList()) {
                if (Files.isDirectory(file) || !file.getFileName().toString().endsWith(".jar"))
                    continue;
                try {
                    modules.add(loadModule(file));
                } catch (IOException | ClassNotFoundException exception) {
                    new IOException(
                                    "An error ocurred while loading modules from " + directory,
                                    exception)
                            .printStackTrace(System.err);
                }
            }
        }
        for (Module module : modules) {
            try {
                module.listeners().forEach(listener -> listener.onLoad(module));
            } catch (Throwable exception) {
                exception.printStackTrace(System.err);
            }
        }
    }

    public static @NotNull Module loadModule(@NotNull Path file)
            throws IOException, ClassNotFoundException {
        URL[] urls = {new URL("jar:file:" + file + "!/")};
        try (JarFile jarFile = new JarFile(file.toFile());
                URLClassLoader classLoader = URLClassLoader.newInstance(urls)) {
            Module module = new Module(file, classLoader, new ArrayList<>(), new ArrayList<>());

            // Load classes
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;
                String classFileName = entry.getName().substring(0, entry.getName().length() - 6);
                String className = classFileName.replace("/", ".");
                try {
                    module.classes().add(Class.forName(className));
                } catch (ClassNotFoundException exception) {
                    module.classes().add(classLoader.loadClass(className));
                }
            }

            // Load listeners
            for (Class<?> clazz : module.classes()) {
                if (Listener.class.isAssignableFrom(clazz)) {
                    try {
                        Listener listener = (Listener) clazz.getDeclaredConstructor().newInstance();
                        module.listeners().add(listener);
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
            }

            System.out.printf("Loaded module %s\n", file.getFileName());
            return module;
        }
    }

    public static void unloadModules() throws IOException {
        modules.forEach(module -> module.listeners().forEach(Listener::onUnload));
        modules.forEach(module -> module.listeners().clear());
        for (Module module : modules) {
            module.classLoader().close();
            System.out.printf("Unloaded module %s\n", module.file().getFileName());
        }
        modules.clear();
    }

    public static @NotNull List<Module> getModules() {
        return modules;
    }
}
