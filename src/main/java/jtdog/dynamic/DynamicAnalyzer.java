package jtdog.dynamic;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
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

import jtdog.method.InvocationMethod;
import jtdog.method.MethodList;
import jtdog.method.MethodProperty;

public class DynamicAnalyzer {

    private final IRuntime jacocoRuntime;
    private final Instrumenter jacocoInstrumenter;
    private final RuntimeData jacocoRuntimeData;
    private final List<String> testClassNames;
    private final List<String> testClassNamesToExecuted;
    private final String testClassesDirPath;

    Map<String, String> fqnToPathString;

    public DynamicAnalyzer(final List<String> testClassNames, final List<String> testClassNamesToExecuted,
            final String testClassesDirPath) {
        this.testClassNames = testClassNames;
        this.testClassNamesToExecuted = testClassNamesToExecuted;
        this.testClassesDirPath = testClassesDirPath;

        this.jacocoRuntime = new LoggerRuntime();
        this.jacocoInstrumenter = new Instrumenter(jacocoRuntime);
        this.jacocoRuntimeData = new RuntimeData();
        this.fqnToPathString = new HashMap<>();
        try {
            jacocoRuntime.startup(jacocoRuntimeData);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    // テスト以外のクラスも instrumenter を適用すべき？
    public void run(final MethodList methodList, final MemoryClassLoader memoryClassLoader) throws Exception {
        // テストクラスすべてに instrumenter を適用
        for (String testClassName : testClassNames) {
            final InputStream original = getTargetClass(testClassName);
            final byte[] instrumented = jacocoInstrumenter.instrument(original, testClassName);
            original.close();
            memoryClassLoader.addDefinition(testClassName, instrumented);
        }

        // テストクラスのロード
        final List<Class<?>> testClasses = new ArrayList<>();
        for (final String name : testClassNamesToExecuted) {
            // System.out.println("name: " + name);
            final Class<?> targetClass = memoryClassLoader.loadClass(name);
            testClasses.add(targetClass);
        }

        // JUnit runner を使用
        final JUnitCore junit = new JUnitCore();
        final RunListener listener = new CoverageMeasurementListener(methodList);
        junit.addListener(listener);
        junit.run(testClasses.toArray(new Class<?>[testClasses.size()]));
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

    /**
     * JUnit 実行中のイベントを受け取るリスナー． Jacoco のカバレッジを回収．
     */
    class CoverageMeasurementListener extends RunListener {
        private final MethodList methodList;
        private boolean analyzeRuntimeData;

        public CoverageMeasurementListener(final MethodList methodList) {
            this.methodList = methodList;
            this.analyzeRuntimeData = true;
        }

        @Override
        public void testFailure(Failure failure) throws Exception {
            // for debug
            System.out.println("test fail: " + failure.getMessage());
            System.out.println("test fail class: " + failure.getDescription().getClassName());

            // test fail = not rotten
            analyzeRuntimeData = false;
            super.testFailure(failure);
        }

        @Override
        public void testIgnored(Description description) throws Exception {
            analyzeRuntimeData = false;
            super.testIgnored(description);
        }

        @Override
        public void testStarted(final Description description) {
            // for debug
            System.out.println(
                    "start: " + description.getMethodName() + " in " + description.getTestClass().getCanonicalName());

            jacocoRuntimeData.reset();
            analyzeRuntimeData = true;
        }

        @Override
        public void testFinished(final Description description) throws IOException {
            // for debug
            System.out.println("finish: " + description.getMethodName());
            if (analyzeRuntimeData) {
                collectRuntimeData(description);
            }
        }

        /**
         * description を基にテストメソッドのプロパティを取得する．
         * 
         * @param description
         * @return
         */
        private MethodProperty getTestMethodProperty(final Description description) {
            String testMethodName = getTestMethodName(description);
            return methodList.getPropertyByName(testMethodName);
        }

        /**
         * description を基にテストメソッドの名前を取得する．
         * 
         * @param description
         * @return
         */
        private String getTestMethodName(final Description description) {
            return description.getTestClass().getName() + "." + description.getMethodName();
        }

        /**
         * jacoco により計測した行ごとの Coverage を回収する．
         *
         * @param description
         * @throws IOException
         */
        private void collectRuntimeData(final Description description) throws IOException {
            HashSet<Integer> rottenLines = new HashSet<>(); // rotten の原因の行番号のリスト
            HashMap<String, IClassCoverage> classNameToCoverage = new HashMap<>();
            ArrayList<IClassCoverage> coverages = new ArrayList<>();

            final TestCoverageBuilder coverageBuilder = new TestCoverageBuilder(classNameToCoverage, coverages);
            analyzeJacocoRuntimeData(coverageBuilder, description, rottenLines);

            // 各アサーションが実行されているか調べる
            MethodProperty testMethodProperty = getTestMethodProperty(description);
            for (IClassCoverage coverage : coverages) {
                String testClassName = coverage.getName().replace("/", ".");
                System.out.println("coverage: " + testClassName);
                checkInvocationExecuted(coverage, testMethodProperty, rottenLines, classNameToCoverage, testClassName);
            }

            // 実行されていないアサーションを含む場合，rotten と判定
            if (rottenLines.size() != 0) {
                testMethodProperty.addTestSmellType(MethodProperty.ROTTEN);
                for (Integer line : rottenLines) {
                    testMethodProperty.addRottenLine(line);
                }
            }

            // 実行可能な文を含まない場合，empty と判定
            if (checkMethodIsEmpty(classNameToCoverage.get(testMethodProperty.getClassName()), testMethodProperty,
                    classNameToCoverage)) {
                testMethodProperty.addTestSmellType(MethodProperty.EMPTY);
            }
        }

        /**
         * jacoco により計測した行ごとの Coverage を回収する．
         *
         * @param coverageBuilder 計測した Coverage を格納する保存先
         * @param description
         * @throws IOException
         */
        private void analyzeJacocoRuntimeData(final TestCoverageBuilder coverageBuilder, final Description description,
                HashSet<Integer> rottenLines) throws IOException {
            final ExecutionDataStore executionData = new ExecutionDataStore();
            final SessionInfoStore sessionInfo = new SessionInfoStore();
            jacocoRuntimeData.collect(executionData, sessionInfo, false);

            final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);

            // 一度でもカバレッジ計測されたクラスのみに対してカバレッジ情報を探索
            for (final ExecutionData data : executionData.getContents()) {
                final String binaryName = data.getName().replace("/", ".");

                // 当該テスト実行でprobeが反応しない＝実行されていない場合はskip
                if (!data.hasHits()) {
                    continue;
                }
                final InputStream original = getTargetClass(binaryName);
                System.out.println("analyze: " + binaryName);
                analyzer.analyzeClass(original, "");
                original.close();
            }

        }

        /**
         * 実行されていないアサーションを含むかを調べる アサーションを含む helper の呼び出しについては helper 内も再帰的に調べる
         * 
         * @param coverage
         * @param property
         * @return
         */
        private boolean checkInvocationExecuted(final IClassCoverage coverage, final MethodProperty property,
                final HashSet<Integer> causeLines, HashMap<String, IClassCoverage> classNameToCoverage,
                final String testClassName) {
            boolean hasAssertionNotExecuted = false;

            boolean tmp = false;
            if (tmp) {
                System.out.println(coverage.getName());
                for (int i = coverage.getFirstLine(); i <= coverage.getLastLine(); i++) {
                    if (!getColor(coverage.getLine(i).getStatus()).equals("")) {
                        System.out.printf("Line %s: %s%n", Integer.valueOf(i),
                                getColor(coverage.getLine(i).getStatus()));
                    }
                }
            }

            // 実行されていない(color = "red" or "") invocation を含むか調べる
            for (InvocationMethod invocation : property.getInvocationList()) {
                int line = invocation.getLineNumber();
                String color = getColor(coverage.getLine(line).getStatus());
                // helper かどうか調べるため，isInvoke の値をセット
                MethodProperty invocationProperty = methodList
                        .getPropertyByIdentifier(invocation.getMethodIdentifier());

                if (invocationProperty == null) {
                    // 実行されていないアサーションの場合
                    String className = invocation.getMethodIdentifier().getClassBinaryName();
                    // String invokedMethodName = invocation.getBinding().getName();
                    if (className.contains("Assert")) {
                        if (color.equals("red")) {
                            causeLines.add(line);
                            hasAssertionNotExecuted = true;
                        }
                        continue;
                    }
                } else {
                    invocationProperty.setIsInvoked(true);
                    // 実行されていないアサーションを含む helper の場合
                    if (invocationProperty.hasAssertionDirectly() || invocationProperty.hasAssertionIndirectly()) {
                        // helper の中まで見に行く
                        String className = invocationProperty.getClassName();
                        if (checkInvocationExecuted(classNameToCoverage.get(className), invocationProperty, causeLines,
                                classNameToCoverage, testClassName)) {
                            causeLines.add(line);
                            hasAssertionNotExecuted = true;
                        }
                    }
                }
            }
            return hasAssertionNotExecuted;
        }

        /**
         * 実行可能文を含まないかどうかを調べる
         * 
         * @param coverage
         * @param property
         * @param classNameToCoverage
         * @return
         */
        private boolean checkMethodIsEmpty(final IClassCoverage coverage, final MethodProperty property,
                HashMap<String, IClassCoverage> classNameToCoverage) {
            int startPosition = property.getStartPosition();
            int endPosition = property.getEndPosition();
            // メソッドの最後の行についてはカラーがあるため，無視する
            int loopEndPosition = startPosition < endPosition ? endPosition - 1 : startPosition;

            // カラーがない = 実行不可能文 or 空白行
            for (int i = startPosition; i <= loopEndPosition; i++) {
                if (!getColor(coverage.getLine(i).getStatus()).equals("")) {
                    return false;
                }
            }

            // TODO ここは本当に必要か要検討
            // ローカルクラスや匿名クラスについても同様に実行可能文を含むかチェック
            String className = coverage.getName().replace("/", ".");
            for (InvocationMethod invocation : property.getInvocationList()) {
                MethodProperty invocationProperty = methodList
                        .getPropertyByIdentifier(invocation.getMethodIdentifier());
                if (invocationProperty != null) {
                    String declaredClassName = invocationProperty.getClassName();
                    if (!declaredClassName.equals(className)
                            && !checkMethodIsEmpty(classNameToCoverage.get(declaredClassName), property,
                                    classNameToCoverage)) {
                        return false;
                    }
                }
            }

            return true;
        }

        /**
         * 特定の行の実行状態を表す定数を色を表す文字列に変換
         * 
         * @param status
         * @return
         */
        private String getColor(final int status) {
            switch (status) {
                case ICounter.NOT_COVERED:
                    return "red";
                case ICounter.PARTLY_COVERED:
                    return "yellow";
                case ICounter.FULLY_COVERED:
                    return "green";
            }
            return "";
        }
    }
}
