package jtdog.dynamic;

import java.net.URL;

public class JUnitMemoryClassLoader extends MemoryClassLoader {
    private final ClassLoader parentClassLoader;
    private final boolean isJUnit5;

    public JUnitMemoryClassLoader(URL[] urls, ClassLoader parent, boolean isJUnit5) {
        super(urls);
        this.parentClassLoader = parent;
        this.isJUnit5 = isJUnit5;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c = null;

        // JUnit関係のクラスのみロードを通常の委譲関係に任す．これがないとJUnitが期待通りに動かない．
        if (name.startsWith("org.junit.") || name.startsWith("junit.")
                || !isJUnit5 && name.startsWith("org.hamcrest.")) {
            try {
                c = parentClassLoader.loadClass(name);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }

        if (c == null) {
            c = super.loadClass(name, resolve);
        }

        if (c == null) {
            throw new ClassNotFoundException(name);
        }

        return c;

    }
}
