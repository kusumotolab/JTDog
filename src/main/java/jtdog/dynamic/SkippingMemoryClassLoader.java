package jtdog.dynamic;

import java.net.URL;

public class SkippingMemoryClassLoader extends MemoryClassLoader {

    private final ClassLoader delegationClassLoader;

    /**
     * コンストラクタ
     *
     * @param urls クラスパス
     */
    public SkippingMemoryClassLoader(final URL[] urls) {
        super(urls);
        delegationClassLoader = findDelegationClassLoader(getClass().getClassLoader());
    }

    public SkippingMemoryClassLoader(final URL[] urls, ClassLoader parent) {
        super(urls, parent);
        delegationClassLoader = findDelegationClassLoader(getClass().getClassLoader());
    }

    /**
     * クラスローダの親子関係を探索して，委譲先となるExtension/PlatformClassLoaderを探す．
     *
     * @param cl
     * @return
     */
    private ClassLoader findDelegationClassLoader(final ClassLoader cl) {
        if (null == cl) {
            throw new RuntimeException("Cannot find extension class loader.");
        }
        // (#600) patch for greater than jdk9
        // 対象の名前が ExtensionClassLoader (jdk8) or PlatformClassLoader(>jdk9) ならOK．
        final String name = cl.toString();
        if (name.contains("$ExtClassLoader@") || //
                name.contains("$PlatformClassLoader@")) {
            return cl;
        }
        // さもなくば再帰的に親を探す
        return findDelegationClassLoader(cl.getParent());
    }

    /**
     * クラスロードを行う．<br>
     * ロード対象がjunit関係のものであれば，直接親のAppClassLoaderからロードする．<br>
     * そうでない場合，AppClassLoaderをスキップしてExtClassLoaderからロードを試み，最後にメモリからロードを試す．<br>
     */
    @Override
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {

        // JUnit関係のクラスのみロードを通常の委譲関係に任す．これがないとJUnitが期待通りに動かない．
        if (name.startsWith("org.junit.") || name.startsWith("junit.") || name.startsWith("org.hamcrest.")) {
            return getParent().loadClass(name);
        }

        // 委譲処理．java.lang.ClassLoader#loadClassを参考に作成．
        synchronized (getClassLoadingLock(name)) {
            // First, check if the class has already been loaded
            Class<?> c = findLoadedClass(name);

            if (null == c) {
                try {
                    // Second, try to load using extension class loader
                    c = delegationClassLoader.loadClass(name);

                    // Don't delegate to the parent.
                    // c = parent.loadClass(name);

                    // TODO 可視性の問題で，resolve変数が反映されていない．本来以下であるべき．リフレクションでなんとかなる．
                    // c = extensionClassLoader.loadClass(name, resolve);
                } catch (final ClassNotFoundException e) {
                    // ignore
                }
            }
            if (null == c) {
                try {
                    // Finally, try to load from memory
                    c = findClass(name);
                } catch (final ClassNotFoundException e) {
                    // ignore
                }
            }
            if (null == c) {
                throw new ClassNotFoundException(name);
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

}