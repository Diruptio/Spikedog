package diruptio.spikedog.util;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class SharedClassLoader extends URLClassLoader {
    private static final List<SharedClassLoader> classLoaders = new ArrayList<>();

    public SharedClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        classLoaders.add(this);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return internalLoadClass(name, resolve, true);
    }

    private Class<?> internalLoadClass(String name, boolean resolve, boolean checkOther) throws ClassNotFoundException {
        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException exception) {
            if (checkOther) {
                for (SharedClassLoader classLoader : classLoaders) {
                    try {
                        if (classLoader != this) {
                            return classLoader.internalLoadClass(name, resolve, false);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
            throw exception;
        }
    }
}
