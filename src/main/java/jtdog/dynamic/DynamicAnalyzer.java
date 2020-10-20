package jtdog.dynamic;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jacoco.core.runtime.RuntimeData;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import jtdog.AssertionList;
import jtdog.method.MethodList;

public class DynamicAnalyzer {

    private final IRuntime jacocoRuntime;
    private final Instrumenter jacocoInstrumenter;
    private final RuntimeData jacocoRuntimeData;
    private final List<String> testClassNames;
    private final String projectDirPath;

    public DynamicAnalyzer(final List<String> testClasses, final String projectDirPath) {
        this.testClassNames = testClasses;
        this.projectDirPath = projectDirPath;
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
    public void run(final MethodList methodList, final AssertionList assertions,
            final MemoryClassLoader memoryClassLoader) throws Exception {

        // FileReader で ${projectdir}/build/classes/java 以下の .class ファイルのパスを取得
        // それぞれについて instrument
        String[] root = { projectDirPath + "build/classes/java" };
        String[] sources = FileReader.getFilePaths(root, "class");
        for(final String name : sources){
            // getTargetClass を書き換える必要あり
            final InputStream original = getTargetClass(name);
            final byte[] instrumented = jacocoInstrumenter.instrument(original, name);
            original.close();
            memoryClassLoader.addDefinition(name, instrumented);
        }

        // テストクラスのロード
        final List<Class<?>> testClasses = new ArrayList<>();
        for (final String name : testClassNames) {
            // String target = testDirPath + "/" + name;
            System.out.println("name: " + name);

            final InputStream original = getTargetClass(name);
            final byte[] instrumented = jacocoInstrumenter.instrument(original, name);
            original.close();
            memoryClassLoader.addDefinition(name, instrumented);

            final Class<?> targetClass = memoryClassLoader.loadClass(name);
            testClasses.add(targetClass);
        }
        final JUnitCore junit = new JUnitCore();
        final RunListener listener = new CoverageMeasurementListener(methodList, assertions);
        junit.addListener(listener);

        // 対象プロジェクト内の依存関係を解決できていないのが原因と考えられる
        junit.run(testClasses.toArray(new Class<?>[testClasses.size()]));
    }

    private InputStream getTargetClass(final String name) throws FileNotFoundException {
        final String resource = projectDirPath + "/build/classes/java/test/" + name.replace('.', '/') + ".class";
        // 直接クラスファイルから読み込む
        return new FileInputStream(resource);
    }

    /**
     * JUnit 実行中のイベントを受け取るリスナー． Jacoco のカバレッジを回収．
     */
    class CoverageMeasurementListener extends RunListener {
        private final MethodList methodList;
        private final AssertionList assertions;

        public CoverageMeasurementListener(final MethodList methodList, final AssertionList assertions) {
            this.methodList = methodList;
            this.assertions = assertions;
        }

        // for debug
        @Override
        public void testFailure(Failure failure) throws Exception {
            System.out.println("test fail: " + failure.getMessage());
            System.out.println("test fail class: " + failure.getDescription().getClassName());
            /* テストの実行結果を表すプロパティを MethodProperty オブジェクトに追加し，ここで失敗を設定 */
            super.testFailure(failure);
        }

        @Override
        public void testStarted(final Description description) {
            System.out.println("start: " + description.getMethodName());
            jacocoRuntimeData.reset();
        }

        @Override
        public void testFinished(final Description description) throws IOException {
            System.out.println("finish: " + description.getMethodName());
            collectRuntimeData(description);
        }

        /**
         * Descriptionから実行したテストメソッドのFQNを取り出す．
         *
         * @param description
         * @return
         */
        private String getTestMethodName(final Description description) {
            return description.getTestClass().getName() + "." + description.getMethodName();
        }

        /**
         * jacocoにより計測した行ごとのCoverageを回収する．
         *
         * @param description
         * @throws IOException
         */
        private void collectRuntimeData(final Description description) throws IOException {
            final TestCoverageBuilder coverageBuilder = new TestCoverageBuilder(getTestMethodName(description),
                    methodList, assertions);
            analyzeJacocoRuntimeData(coverageBuilder, description);
        }

        /**
         * jacocoにより計測した行ごとのCoverageを回収する．
         *
         * @param coverageBuilder 計測したCoverageを格納する保存先
         * @param description
         * @throws IOException
         */
        private void analyzeJacocoRuntimeData(final TestCoverageBuilder coverageBuilder, final Description description)
                throws IOException {
            final ExecutionDataStore executionData = new ExecutionDataStore();
            final SessionInfoStore sessionInfo = new SessionInfoStore();
            jacocoRuntimeData.collect(executionData, sessionInfo, false);

            final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);

            // 一度でもカバレッジ計測されたクラスのみに対してカバレッジ情報を探索
            for (final ExecutionData data : executionData.getContents()) {
                final String strFqn = data.getName().replace("/", ".");
                System.out.println("data: " + strFqn + ", " + description.getTestClass().getName());

                // 当該テスト実行でprobeが反応しない＝実行されていない場合はskip
                if (!data.hasHits()) {
                    continue;
                }

                final InputStream original = getTargetClass(description.getTestClass().getName());
                analyzer.analyzeClass(original, "");
                original.close();
            }
        }
    }
}
