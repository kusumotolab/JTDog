package jtdog.dynamic;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.runtime.RuntimeData;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import jtdog.file.ObjectSerializer;
import jtdog.method.InvocationMethod;
import jtdog.method.MethodList;
import jtdog.method.MethodProperty;

public class JUnit5TestRunner {
    private final static int RERUN_TIMES = 20;
    private final String testClassesDirPath;
    private final RuntimeData jacocoRuntimeData;

    List<DiscoverySelector> selectors;

    public JUnit5TestRunner(final String testClassesDirPath, final RuntimeData jacocoRuntimeData) {
        this.testClassesDirPath = testClassesDirPath;
        this.jacocoRuntimeData = jacocoRuntimeData;
    }

    public void run(final MethodList methodList, final List<Class<?>> testClasses) throws Exception {
        final HashMap<String, Boolean> testResultsInDefaultOrder = new HashMap<>();
        final HashMap<String, ArrayList<String>> testClassNameToDefaultExecutionOrder = new HashMap<>();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(getTestClasses(testClasses)).build();
        Launcher launcher = LauncherFactory.create();

        TestExecutionListener listener = new CoverageMeasurementListener(methodList, testResultsInDefaultOrder,
                testClassNameToDefaultExecutionOrder);
        launcher.execute(request, listener);

        ObjectSerializer.serializeObject("jtdog_tmp/testResultsInDefaultOrder.ser", testResultsInDefaultOrder);
        ObjectSerializer.serializeObject("jtdog_tmp/testClassNameToDefaultExecutionOrder.ser",
                testClassNameToDefaultExecutionOrder);
    }

    private List<DiscoverySelector> getTestClasses(List<Class<?>> testClasses) {
        List<DiscoverySelector> selectors = new ArrayList<>();
        for (Class<?> testClass : testClasses) {
            selectors.add(selectClass(testClass));
        }
        return selectors;
    }

    class CoverageMeasurementListener implements TestExecutionListener {
        private final MethodList methodList;
        private final HashMap<String, Boolean> testResultsInDefaultOrder;
        private final HashMap<String, ArrayList<String>> testClassNameToDefaultExecutionOrder;

        public CoverageMeasurementListener(final MethodList methodList,
                final HashMap<String, Boolean> testResultsInDefaultOrder,
                final HashMap<String, ArrayList<String>> testClassNameToDefaultExecutionOrder) {
            this.methodList = methodList;
            this.testResultsInDefaultOrder = testResultsInDefaultOrder;
            this.testClassNameToDefaultExecutionOrder = testClassNameToDefaultExecutionOrder;
        }

        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            System.out.println(
                    "start: " + testIdentifier.getDisplayName() + " unique_id " + testIdentifier.getUniqueId());

            jacocoRuntimeData.reset();
            TestExecutionListener.super.executionStarted(testIdentifier);
        }

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            try {

                MethodSource source = (MethodSource) testIdentifier.getSource().get();
                String testClassName = source.getClassName();
                System.out.println("finish: " + testClassName);
                if (testClassNameToDefaultExecutionOrder.containsKey(testClassName)) {
                    testClassNameToDefaultExecutionOrder.get(testClassName).add(source.getMethodName());
                } else {
                    ArrayList<String> defaultExecutionOrder = new ArrayList<>();
                    defaultExecutionOrder.add(source.getMethodName());
                    testClassNameToDefaultExecutionOrder.put(testClassName, defaultExecutionOrder);
                }

                Status status = testExecutionResult.getStatus();
                if (status == Status.SUCCESSFUL) {
                    testResultsInDefaultOrder.put(getTestMethodFQN(testIdentifier), true);
                    try {
                        collectRuntimeData(testIdentifier);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (status == Status.FAILED) {
                    // for debug
                    System.out.println("test fail: " + testIdentifier.getDisplayName());

                    testResultsInDefaultOrder.put(getTestMethodFQN(testIdentifier), false);
                    // identify flaky test failure
                    reRun(RERUN_TIMES, source.getJavaClass(), source.getMethodName());
                }
            } catch (Exception e) {
                System.out.println("error: " + e.getClass().getName());
            }
            TestExecutionListener.super.executionFinished(testIdentifier, testExecutionResult);
        }

        /**
         * jacoco により計測した行ごとの Coverage を回収する．
         *
         * @param testIdentifier
         * @throws IOException
         */
        private void collectRuntimeData(final TestIdentifier testIdentifier) throws IOException {
            HashSet<Integer> rottenLines = new HashSet<>(); // rotten の原因の行番号のリスト
            HashMap<String, IClassCoverage> classNameToCoverage = new HashMap<>();
            ArrayList<IClassCoverage> coverages = new ArrayList<>();

            final TestCoverageBuilder coverageBuilder = new TestCoverageBuilder(classNameToCoverage, coverages);
            analyzeJacocoRuntimeData(coverageBuilder, rottenLines);

            // 各アサーションが実行されているか調べる
            MethodProperty testMethodProperty = getTestMethodProperty(testIdentifier);
            for (IClassCoverage coverage : coverages) {
                String testClassName = coverage.getName().replace("/", ".");
                checkInvocationExecuted(coverage, testMethodProperty, rottenLines, classNameToCoverage, testClassName);
            }

            // 実行されていないアサーションを含む場合，rotten と判定
            if (rottenLines.size() != 0) {
                if (testMethodProperty.hasContextDependentRottenAssertion()) {
                    testMethodProperty.addTestSmellType(MethodProperty.CONTEXT_DEPENDENT);
                }

                if (testMethodProperty.hasSkippedRottenAssertion()) {
                    testMethodProperty.addTestSmellType(MethodProperty.SKIP);
                }

                if (testMethodProperty.hasFullyRottenAssertion()) {
                    testMethodProperty.addTestSmellType(MethodProperty.ROTTEN);
                }

                if (testMethodProperty.hasMissedFailAssertion()) {
                    testMethodProperty.addTestSmellType(MethodProperty.MISSED_FAIL);
                }

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
        private void analyzeJacocoRuntimeData(final TestCoverageBuilder coverageBuilder, HashSet<Integer> rottenLines)
                throws IOException {
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
                analyzer.analyzeClass(original, "");
                original.close();
            }

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
         * description を基にテストメソッドのプロパティを取得する．
         * 
         * @param description
         * @return
         */
        private MethodProperty getTestMethodProperty(final TestIdentifier testIdentifier) {
            String testMethodName = getTestMethodFQN(testIdentifier);
            return methodList.getPropertyByName(testMethodName);
        }

        /**
         * テストメソッド名を基にテストメソッドのプロパティを取得する．
         * 
         * @param methodName
         * @return
         */
        private MethodProperty getTestMethodProperty(final String methodName) {
            return methodList.getPropertyByName(methodName);
        }

        /**
         * description を基にテストメソッドの fqn を取得する．
         * 
         * @param description
         * @return
         */
        private String getTestMethodFQN(final TestIdentifier testIdentifier) {
            MethodSource source = (MethodSource) testIdentifier.getSource().get();
            return source.getClassName() + "." + source.getMethodName();
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

            // 実行されていない(color = "red" or "") invocation を含むか調べる
            for (InvocationMethod invocation : property.getInvocationList()) {
                int line = invocation.getLineNumber();
                String color = getColor(coverage.getLine(line).getStatus());
                // helper かどうか調べるため，isInvoke の値をセット
                MethodProperty invocationProperty = methodList
                        .getPropertyByIdentifier(invocation.getMethodIdentifier());

                if (invocationProperty == null) {
                    // 実行されていないアサーションの場合
                    if (invocation.getMethodIdentifier().getSimpleName().startsWith("assert")) {
                        if (color.equals("red")) {
                            causeLines.add(line);
                            setRottenProperty(property, invocation);
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
                            setRottenProperty(property, invocation);
                            hasAssertionNotExecuted = true;
                        }
                    }
                }
            }
            return hasAssertionNotExecuted;
        }

        /**
         * テストメソッドがどの種の rotten な assertion を含むかの情報を MethodProperty にセット
         * 
         * @param property
         * @param invocation
         */
        private void setRottenProperty(MethodProperty property, InvocationMethod invocation) {
            boolean isInIfElseStatement = invocation.isInIfElseStatement();
            boolean isCouldBeSkipped = invocation.isCouldBeSkipped();
            boolean isMissedFail = invocation.isMissedFail();
            if (!isInIfElseStatement && !isCouldBeSkipped && !isMissedFail) {
                property.setHasFullyRottenAssertion(true);
            }
            property.setHasContextDependentRottenAssertion(isInIfElseStatement);
            property.setHasSkippedRottenAssertion(isCouldBeSkipped);
            property.setHasMissedFailAssertion(isMissedFail);
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

        private void reRun(int numberOfTimes, Class<?> clazz, String methodName) {
            Boolean wasSuccessful = Boolean.valueOf(false);
            Launcher launcher = LauncherFactory.create();
            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(selectMethod(clazz, methodName)).build();
            TestExecutionListener listener = new reRunListener(wasSuccessful, methodList);

            for (int i = 0; i < numberOfTimes; i++) {
                launcher.execute(request, listener);
            }
        }

    }

    class reRunListener implements TestExecutionListener {
        Boolean wasSuccessful;
        MethodList methodList;

        public reRunListener(Boolean wasSuccessful, MethodList methodList) {
            this.wasSuccessful = wasSuccessful;
            this.methodList = methodList;
        }

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            System.out.println("rerun: " + testIdentifier.getDisplayName() + ", " + testExecutionResult.getStatus());
            try {
                if (testExecutionResult.getStatus() == Status.SUCCESSFUL) {
                    // this test method is flaky
                    MethodSource source = (MethodSource) testIdentifier.getSource().get();
                    String testMethodName = source.getClassName() + "." + source.getMethodName();
                    MethodProperty testMethodProperty = methodList.getPropertyByName(testMethodName);
                    if (!testMethodProperty.getTestSmellTypes().contains(MethodProperty.FLAKY)) {
                        testMethodProperty.addTestSmellType(MethodProperty.FLAKY);
                    }
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
            TestExecutionListener.super.executionFinished(testIdentifier, testExecutionResult);
        }
    }

}