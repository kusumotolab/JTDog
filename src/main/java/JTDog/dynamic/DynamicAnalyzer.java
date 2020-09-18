package JTDog.dynamic;

import java.io.IOException;
import java.io.InputStream;

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
import org.junit.runner.notification.RunListener;

public class DynamicAnalyzer {

    private final IRuntime jacocoRuntime;
    private final Instrumenter jacocoInstrumenter;
    private final RuntimeData jacocoRuntimeData;
    private InputStream original;
    private final String[] testClasses;
    private String targetName; // UserTest.java でテスト

    public DynamicAnalyzer(final String[] sources) {
        this.testClasses = sources;
        this.jacocoRuntime = new LoggerRuntime();
        this.jacocoInstrumenter = new Instrumenter(jacocoRuntime);
        this.jacocoRuntimeData = new RuntimeData();
        // this.targetName = UserTest.class.getName();
        try {
            System.out.println("startup.");
            jacocoRuntime.startup(jacocoRuntimeData);
        } catch (final Exception e) {
            // TODO should be described to log
            e.printStackTrace();
        }
    }

    public void run() throws Exception {

        final InputStream original = getTargetClass(targetName);
        final byte[] instrumented = jacocoInstrumenter.instrument(original, targetName);
        original.close();

        final MemoryClassLoader memoryClassLoader = new MemoryClassLoader();
        memoryClassLoader.addDefinition(targetName, instrumented);
        final Class<?> targetClass = memoryClassLoader.loadClass(targetName);

        final JUnitCore junit = new JUnitCore();
        final RunListener listener = new CoverageMeasurementListener();
        junit.addListener(listener);
        junit.run(targetClass);
    }

    private InputStream getTargetClass(final String name) {
        final String resource = '/' + name.replace('.', '/') + ".class";
        return getClass().getResourceAsStream(resource);
    }

    /**
     * JUnit 実行中のイベントを受け取るリスナー． Jacoco のカバレッジを回収．
     */
    class CoverageMeasurementListener extends RunListener {

        @Override
        public void testStarted(final Description description) {
            jacocoRuntimeData.reset();
        }

        @Override
        public void testFinished(final Description description) throws IOException {
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
            // メソッド名出力 for debug
            System.out.println(getTestMethodName(description));
            final TestCoverageBuilder coverageBuilder = new TestCoverageBuilder();
            analyzeJacocoRuntimeData(coverageBuilder);
        }

        /**
         * jacocoにより計測した行ごとのCoverageを回収する．
         *
         * @param coverageBuilder 計測したCoverageを格納する保存先
         * @throws IOException
         */
        private void analyzeJacocoRuntimeData(final TestCoverageBuilder coverageBuilder) throws IOException {
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

                original = getTargetClass(targetName);
                analyzer.analyzeClass(original, targetName);
                original.close();
            }
        }
    }
}
