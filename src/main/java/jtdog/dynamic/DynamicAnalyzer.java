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
        // this.targetName = UserTest.class.getName();
        try {
            jacocoRuntime.startup(jacocoRuntimeData);
        } catch (final Exception e) {
            // TODO should be described to log
            e.printStackTrace();
        }
    }

    public void run(final MethodList methodlist, MemoryClassLoader memoryClassLoader) throws Exception {
        System.out.println("run.");
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
        final RunListener listener = new CoverageMeasurementListener(methodlist);
        junit.addListener(listener);

        // 対象プロジェクト内の依存関係を解決できていないのが原因と考えられる
        junit.run(testClasses.toArray(new Class<?>[testClasses.size()]));
    }

    private InputStream getTargetClass(final String name) throws FileNotFoundException {
        // final String resource = '/' + name.replace('.', '/') + ".class";

        // SourceDirectorySet の getDestinationDirectory() 使えるかも
        final String resource = projectDirPath + "/build/classes/java/test/" + name.replace('.', '/') + ".class";
        // System.out.println("resource: " + resource);
        // getClass で取得すべきクラスが対象プロジェクトのクラスと考えられる．
        // ↑ 対象プロジェクトのリソースを検索したいため
        // もう一つの instrument を使う方がいいのか？
        // 一度はコンパイルする必要がある ← class ファイルが欲しいため
        // return getClass().getResourceAsStream(resource);

        // 直接クラスファイルから読み込む方式
        return new FileInputStream(resource);
    }

    /**
     * JUnit 実行中のイベントを受け取るリスナー． Jacoco のカバレッジを回収．
     */
    class CoverageMeasurementListener extends RunListener {
        private final MethodList methodList;

        public CoverageMeasurementListener(final MethodList methodList) {
            this.methodList = methodList;
        }

        // for debug
        @Override
        public void testFailure(Failure failure) throws Exception {
            System.out.println("fail: " + failure.getMessage());
            // System.out.println("trace: " + failure.getTrace());
            System.out.println("fail class: " + failure.getDescription().getClassName());
            super.testFailure(failure);
        }

        @Override
        public void testStarted(final Description description) {
            System.out.println("start:" + getTestMethodName(description));
            jacocoRuntimeData.reset();
        }

        @Override
        public void testFinished(final Description description) throws IOException {
            System.out.println("finish:" + getTestMethodName(description));
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
                    methodList);
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
