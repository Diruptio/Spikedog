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

public class JarLoader {
    private static final List<Jar> jars = new ArrayList<>();

    public static void loadJars(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.toList()) {
                if (Files.isDirectory(file) || !file.getFileName().toString().endsWith(".jar"))
                    continue;
                try {
                    jars.add(loadJar(file));
                } catch (IOException | ClassNotFoundException exception) {
                    new IOException("An error ocurred while loading " + directory, exception)
                            .printStackTrace(System.err);
                }
            }
        }
    }

    public static Jar loadJar(Path file) throws IOException, ClassNotFoundException {
        URL[] urls = {new URL("jar:file:" + file + "!/")};
        try (JarFile jarFile = new JarFile(file.toFile());
                URLClassLoader classLoader = URLClassLoader.newInstance(urls)) {
            Jar jar = new Jar(file, classLoader, new ArrayList<>());

            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;
                String classFileName = entry.getName().substring(0, entry.getName().length() - 6);
                String className = classFileName.replace("/", ".");
                try {
                    jar.classes().add(Class.forName(className));
                } catch (ClassNotFoundException exception) {
                    classLoader.loadClass(className);
                    jar.classes().add(Class.forName(className));
                }
            }
            return jar;
        }
    }

    public static void unloadJars() throws IOException {
        for (Jar jar : jars) {
            jar.classLoader().close();
        }
    }
}
