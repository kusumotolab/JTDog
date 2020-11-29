package jtdog.dynamic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jacoco.core.runtime.RuntimeData;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import jtdog.method.InvocationMethod;
import jtdog.method.MethodList;
import jtdog.method.MethodProperty;

public class DynamicAnalyzer {
    private final static int RERUN_TIMES = 10;

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
    public void run(final MethodList methodList, final MemoryClassLoader memoryClassLoader, final String projectName)
            throws Exception {
        // テストクラスすべてに instrumenter を適用
        for (String testClassName : testClassNames) {
            final InputStream original = getTargetClass(testClassName);
            final byte[] instrumented = jacocoInstrumenter.instrument(original, testClassName);
            original.close();
            memoryClassLoader.addDefinition(testClassName, instrumented);
        }

        // テストクラスのロード
        List<Class<?>> testClasses = loadTestClasses(memoryClassLoader);

        // JUnit runner を使用
        final JUnitCore junit = new JUnitCore();
        final HashMap<String, Boolean> testResultsInDefaultOrder = new HashMap<>();
        final RunListener listener = new CoverageMeasurementListener(methodList, testResultsInDefaultOrder);
        junit.addListener(listener);
        junit.run(testClasses.toArray(new Class<?>[testClasses.size()]));

        serializeObject("jtdog_tmp/testClassNamesToExecuted.ser", testClassNamesToExecuted);
        serializeObject("jtdog_tmp/testResultsInDefaultOrder.ser", testResultsInDefaultOrder);

        List<String> cmd = new ArrayList<String>();
        cmd.add("gradle");
        String taskName = (projectName == null) ? "detectDependentTest" : projectName + ":detectDependentTest";
        cmd.add(taskName);
        cmd.add("--stacktrace");

        Process p = Runtime.getRuntime().exec(cmd.toArray(new String[cmd.size()]));
        // 出力ストリーム
        new StreamThread(p.getInputStream(), "OUTPUT").start();
        // エラーストリーム
        new StreamThread(p.getErrorStream(), "ERROR").start();

        p.waitFor();
        p.destroy();
        /*
         * TestDependencyDetector detector = new
         * TestDependencyDetector(testResultsInDefaultOrder); for (Class<?> clazz :
         * testClasses) { detector.run(clazz); }
         */
        HashSet<String> dependentTests = deserializeHashMap("jtdog_tmp/dependentTests.ser");

        for (String fqn : dependentTests) {
            MethodProperty testMethodProperty = methodList.getPropertyByName(fqn);
            if (!testMethodProperty.getTestSmellTypes().contains(MethodProperty.FLAKY)) {
                testMethodProperty.addTestSmellType(MethodProperty.TEST_DEPENDENCY);
            }
        }

        File tmpDirectory = new File("jtdog_tmp");
        tmpDirectory.delete();

    }

    private void serializeObject(String fileName, Object object) throws IOException {
        File file = new File(fileName);
        file.getParentFile().mkdirs();
        file.createNewFile();
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

        objectOutputStream.writeObject(object);
        objectOutputStream.flush();

        objectOutputStream.close();
        fileOutputStream.close();
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

    /**
     * JUnit 実行中のイベントを受け取るリスナー． Jacoco のカバレッジを回収．
     */
    class CoverageMeasurementListener extends RunListener {
        private final MethodList methodList;

        private boolean isTestSuccessful;
        private HashMap<String, Boolean> testResultsInDefaultOrder;

        public CoverageMeasurementListener(final MethodList methodList,
                final HashMap<String, Boolean> testResultsInDefaultOrder) {
            this.methodList = methodList;
            this.testResultsInDefaultOrder = testResultsInDefaultOrder;
            this.isTestSuccessful = true;
        }

        @Override
        public void testSuiteStarted(Description description) throws Exception {
            testResultsInDefaultOrder.clear();
            super.testSuiteStarted(description);
        }

        @Override
        public void testStarted(final Description description) {
            // for debug
            System.out.println(
                    "start: " + description.getMethodName() + " in " + description.getTestClass().getCanonicalName());

            jacocoRuntimeData.reset();
            isTestSuccessful = true;
        }

        @Override
        public void testFailure(Failure failure) throws Exception {
            Description description = failure.getDescription();

            // for debug
            System.out.println("test fail: " + failure.getMessage());
            System.out.println("test fail class: " + failure.getDescription().getClassName());

            testResultsInDefaultOrder.put(getTestMethodFQN(description), false);

            // identify flaky test failure
            if (reRun(RERUN_TIMES, description.getTestClass(), description.getMethodName())) {
                // this test method is flaky
                MethodProperty testMethodProperty = getTestMethodProperty(description);
                testMethodProperty.addTestSmellType(MethodProperty.FLAKY);
            }

            // test fail = not rotten
            isTestSuccessful = false;
            super.testFailure(failure);
        }

        @Override
        public void testIgnored(Description description) throws Exception {
            isTestSuccessful = false;
            super.testIgnored(description);
        }

        @Override
        public void testFinished(final Description description) throws IOException {
            // for debug
            System.out.println("finish: " + description.getMethodName());

            if (isTestSuccessful) {
                testResultsInDefaultOrder.put(getTestMethodFQN(description), true);
                collectRuntimeData(description);
            }
        }

        @Override
        public void testSuiteFinished(Description description) throws Exception {
            // テストクラスの全テストメソッド実行後，test dependency を調べる
            if (!description.getClassName().equals("classes")) {
                /*
                 * TestDependencyDetector detector = new
                 * TestDependencyDetector(testResultsInDefaultOrder);
                 * detector.run(description.getTestClass()); for (Description desc :
                 * detector.getTestDependencies()) { MethodProperty testMethodProperty =
                 * getTestMethodProperty(desc);
                 * testMethodProperty.addTestSmellType(MethodProperty.TEST_DEPENDENCY); }
                 */
            }
            super.testSuiteFinished(description);
        }

        /**
         * description を基にテストメソッドのプロパティを取得する．
         * 
         * @param description
         * @return
         */
        private MethodProperty getTestMethodProperty(final Description description) {
            String testMethodName = getTestMethodFQN(description);
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
        private String getTestMethodFQN(final Description description) {
            return description.getClassName() + "." + description.getMethodName();
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

        private boolean reRun(int numberOfTimes, Class<?> clazz, String methodName) {
            JUnitCore junit = new JUnitCore();
            for (int i = 0; i < numberOfTimes; i++) {
                Result result = junit.run(Request.method(clazz, methodName));
                if (result.wasSuccessful()) {
                    return true;
                }
            }
            return false;
        }
    }
}
