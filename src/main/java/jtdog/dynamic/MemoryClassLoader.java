package jtdog.dynamic;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * A class loader that loads classes from in-memory data.
 */
public class MemoryClassLoader extends URLClassLoader {

    public MemoryClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    private final Map<String, byte[]> definitions = new HashMap<String, byte[]>();

    /**
     * Add a in-memory representation of a class.
     *
     * @param name  name of the class
     * @param bytes class definition
     */
    public void addDefinition(final String name, final byte[] bytes) {
        definitions.put(name, bytes);
    }

    /**
     * メモリ上からクラスを探す． <br>
     * まずURLClassLoaderによるファイルシステム上のクラスのロードを試み，それがなければメモリ上のクラスロードを試す．
     */
    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        Class<?> c = null;
        // try to load from memory
        final byte[] bytes = definitions.get(name);
        if (bytes != null) {
            try {
                c = defineClass(name, bytes, 0, bytes.length);
            } catch (final ClassFormatError e) {
                throw e;
            }
        }

        // if fails, try to load from classpath
        if (null == c) {
            try {
                c = super.findClass(name);
            } catch (final ClassNotFoundException e1) {
                // ignore
            }
        }

        // otherwise, class not found
        if (null == c) {
            throw new ClassNotFoundException(name);
        }
        return c;
    }

}
