package jtdog.dynamic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jacoco.core.runtime.RuntimeData;

import jtdog.file.ObjectSerializer;
import jtdog.method.MethodList;
import jtdog.method.MethodProperty;

public class DynamicAnalyzer {
    private final static int RANDOMIZED_TRIALS = 20;

    private final List<String> testClassNames;
    private final List<String> testClassNamesToExecuted;
    private final String testClassesDirPath;
    private final IRuntime jacocoRuntime;
    private final Instrumenter jacocoInstrumenter;
    private final RuntimeData jacocoRuntimeData;

    public DynamicAnalyzer(final List<String> testClassNames, final List<String> testClassNamesToExecuted,
            final String testClassesDirPath) {
        this.testClassNames = testClassNames;
        this.testClassNamesToExecuted = testClassNamesToExecuted;
        this.testClassesDirPath = testClassesDirPath;

        this.jacocoRuntime = new LoggerRuntime();
        this.jacocoInstrumenter = new Instrumenter(jacocoRuntime);
        this.jacocoRuntimeData = new RuntimeData();

        try {
            jacocoRuntime.startup(jacocoRuntimeData);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    // テスト以外のクラスも instrumenter を適用すべき？
    public void run(final MethodList methodList, final MemoryClassLoader memoryClassLoader, final String projectName,
            final boolean isJUnit5) throws Exception {
        // テストクラスすべてに instrumenter を適用
        for (String testClassName : testClassNames) {
            final InputStream original = getTargetClass(testClassName);
            final byte[] instrumented = jacocoInstrumenter.instrument(original, testClassName);
            original.close();
            memoryClassLoader.addDefinition(testClassName, instrumented);
        }

        // テストクラスのロード
        List<Class<?>> testClasses = loadTestClasses(memoryClassLoader);

        if (isJUnit5) {
            JUnit5TestRunner runner = new JUnit5TestRunner(testClassesDirPath, jacocoRuntimeData);
            runner.run(methodList, testClasses);
        } else {
            JUnit4TestRunner runner = new JUnit4TestRunner(testClassesDirPath, jacocoRuntimeData);
            runner.run(methodList, testClasses);
        }

        ObjectSerializer.serializeObject("jtdog_tmp/testClassNamesToExecuted.ser", testClassNamesToExecuted);

        HashSet<String> dependentTests = new HashSet<>();
        System.out.println("detecting dependent tests ...");
        // ランダムな順番でテスト実行を繰り返す
        for (int i = 0; i < RANDOMIZED_TRIALS; i++) {
            List<String> cmd = new ArrayList<String>();
            cmd.add("gradle");
            String taskName = (projectName == null) ? "detectDependentTest" : projectName + ":detectDependentTest";
            cmd.add(taskName);
            if (isJUnit5) {
                cmd.add("-Pjunit5=true");
            }
            cmd.add("--stacktrace");

            Process p = Runtime.getRuntime().exec(cmd.toArray(new String[cmd.size()]));
            // 出力ストリーム
            new StreamThread(p.getInputStream(), "OUTPUT").start();
            // エラーストリーム
            new StreamThread(p.getErrorStream(), "ERROR").start();

            p.waitFor();
            p.destroy();

            dependentTests.addAll(deserializeHashMap("jtdog_tmp/dependentTests.ser"));
        }

        for (String fqn : dependentTests) {
            MethodProperty testMethodProperty = methodList.getPropertyByName(fqn);
            if (!testMethodProperty.getTestSmellTypes().contains(MethodProperty.FLAKY)) {
                testMethodProperty.addTestSmellType(MethodProperty.TEST_DEPENDENCY);
            }
        }

        File tmpDirectory = new File("jtdog_tmp");
        recursiveDeleteFile(tmpDirectory);

    }

    private void recursiveDeleteFile(final File file) throws Exception {
        // 存在しない場合は処理終了
        if (!file.exists()) {
            return;
        }
        // 対象がディレクトリの場合は再帰処理
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                recursiveDeleteFile(child);
            }
        }
        // 対象がファイルもしくは配下が空のディレクトリの場合は削除する
        file.delete();
    }

    @SuppressWarnings("unchecked")
    private HashSet<String> deserializeHashMap(String fileName) throws IOException, ClassNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(fileName);
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

        HashSet<String> hashSet = (HashSet<String>) objectInputStream.readObject();

        objectInputStream.close();
        fileInputStream.close();

        return hashSet;
    }

    private List<Class<?>> loadTestClasses(MemoryClassLoader memoryClassLoader) throws ClassNotFoundException {
        final List<Class<?>> testClasses = new ArrayList<>();
        for (final String name : testClassNamesToExecuted) {
            final Class<?> targetClass = memoryClassLoader.loadClass(name);
            testClasses.add(targetClass);
        }
        return testClasses;
    }

    /**
     * 指定された名前の .class ファイルを InputStream として読み込む
     * 
     * @param name
     * @return
     * @throws FileNotFoundException
     */
    private InputStream getTargetClass(final String name) throws FileNotFoundException {
        final String resource = testClassesDirPath + "/" + name.replace('.', '/') + ".class";
        return new FileInputStream(resource);
    }

}
