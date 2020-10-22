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

import jtdog.AssertionList;
import jtdog.file.FileReader;
import jtdog.method.InvocationMethod;
import jtdog.method.MethodList;
import jtdog.method.MethodProperty;

public class DynamicAnalyzer {

    private final IRuntime jacocoRuntime;
    private final Instrumenter jacocoInstrumenter;
    private final RuntimeData jacocoRuntimeData;
    private final List<String> testClassNames;
    private final String projectDirPath;

    Map<String, String> fqnToPathString;

    public DynamicAnalyzer(final List<String> testClasses, final String projectDirPath) {
        this.testClassNames = testClasses;
        this.projectDirPath = projectDirPath;
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
    public void run(final MethodList methodList, final AssertionList assertions,
            final MemoryClassLoader memoryClassLoader) throws Exception {

        // FileReader で ${projectDir}/build/classes/java 以下の .class ファイルのパスを取得
        // それぞれについて instrument
        // 名前をちゃんとする必要がある．
        // findClass で使うのは jacoco_junit.UserTest みたいな感じ
        // FileReader.getFilePaths で取得できるのは
        // /home/tm/RottenGreen/practice/jacoco_junit/build/classes/java/test/jacoco_junit/UserTest.class
        // みたいな感じ
        // /home/tm/RottenGreen/practice/jacoco_junit/build/classes/java/test/ を replace
        // で消す？
        String[] rootTest = { projectDirPath + "/build/classes/java/test" };
        String[] sources = FileReader.getFilePaths(rootTest, "class");
        for (final String name : sources) {
            String fqn = name.replace(projectDirPath + "/build/classes/java/test/", "").replace(".class", "")
                    .replace("/", ".");
            System.out.println("source test: " + fqn);
            fqnToPathString.put(fqn, name);
            final InputStream original = new FileInputStream(name);
            final byte[] instrumented = jacocoInstrumenter.instrument(original, name);
            original.close();
            memoryClassLoader.addDefinition(fqn, instrumented);
        }

        // イラン気がする
        /*
         * String[] rootMain = { projectDirPath + "/build/classes/java/main" }; String[]
         * mainSources = FileReader.getFilePaths(rootMain, "class"); for (final String
         * name : mainSources) { String fqn = name.replace(projectDirPath +
         * "/build/classes/java/main/", "").replace(".class", "") .replace("/", ".");
         * System.out.println("source main: " + fqn); fqnToPathString.put(fqn, name);
         * final InputStream original = new FileInputStream(name); final byte[]
         * instrumented = jacocoInstrumenter.instrument(original, name);
         * original.close(); memoryClassLoader.addDefinition(fqn, instrumented); }
         */

        // テストクラスのロード
        final List<Class<?>> testClasses = new ArrayList<>();
        for (final String name : testClassNames) {
            // String target = testDirPath + "/" + name;
            System.out.println("name: " + name);

            /*
             * final InputStream original = getTargetClass(name); final byte[] instrumented
             * = jacocoInstrumenter.instrument(original, name); original.close();
             * memoryClassLoader.addDefinition(name, instrumented);
             */

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
         * description を基にテストメソッドのプロパティを取得する．
         * 
         * @param description
         * @return
         */
        private MethodProperty getTestMethodProperty(final Description description) {
            String testMethodName = description.getTestClass().getName() + "." + description.getMethodName();
            return methodList.getPropertyByName(testMethodName);
        }

        /**
         * jacocoにより計測した行ごとのCoverageを回収する．
         *
         * @param description
         * @throws IOException
         */
        private void collectRuntimeData(final Description description) throws IOException {
            HashSet<Integer> causeLines = new HashSet<>(); // rotten の原因の行番号のリスト
            HashMap<String, IClassCoverage> classNameToCoverage = new HashMap<>();
            ArrayList<IClassCoverage> coverages = new ArrayList<>();

            final TestCoverageBuilder coverageBuilder = new TestCoverageBuilder(classNameToCoverage, coverages);
            analyzeJacocoRuntimeData(coverageBuilder, description, causeLines);

            // 各アサーションが実行されているか調べる
            MethodProperty testMethodProperty = getTestMethodProperty(description);
            for (IClassCoverage coverage : coverages) {
                String testClassName = coverage.getName().replace("/", ".");
                System.out.println("coverage: " + testClassName);
                checkInvocationExecuted(coverage, testMethodProperty, causeLines, classNameToCoverage, testClassName);
            }

            // 実行されていないアサーションを含む場合，rotten と設定
            if (causeLines.size() != 0) {
                testMethodProperty.addTestSmellType(MethodProperty.ROTTEN);
                for (Integer line : causeLines) {
                    testMethodProperty.addCauseLine(line);
                }
            }
        }

        /**
         * jacocoにより計測した行ごとのCoverageを回収する．
         *
         * @param coverageBuilder 計測したCoverageを格納する保存先
         * @param description
         * @throws IOException
         */
        private void analyzeJacocoRuntimeData(final TestCoverageBuilder coverageBuilder, final Description description,
                HashSet<Integer> causeLines) throws IOException {
            final ExecutionDataStore executionData = new ExecutionDataStore();
            final SessionInfoStore sessionInfo = new SessionInfoStore();
            jacocoRuntimeData.collect(executionData, sessionInfo, false);

            final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);

            // 一度でもカバレッジ計測されたクラスのみに対してカバレッジ情報を探索
            for (final ExecutionData data : executionData.getContents()) {
                final String qualifiedName = data.getName().replace("/", ".");
                System.out.println("data: " + qualifiedName + ", " + description.getTestClass().getName());

                // 当該テスト実行でprobeが反応しない＝実行されていない場合はskip
                if (!data.hasHits()) {
                    continue;
                }

                final InputStream original = new FileInputStream(fqnToPathString.get(qualifiedName));
                System.out.println("analyze: " + qualifiedName);
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

            System.out.println("coverage: " + coverage.getName());
            boolean tmp = false;
            if (tmp) {
                for (int i = coverage.getFirstLine(); i <= coverage.getLastLine(); i++) {
                    System.out.printf("Line %s: %s%n", Integer.valueOf(i), getColor(coverage.getLine(i).getStatus()));
                }
            }

            // 実行されていない(color = "red" or "") invocation を含むか調べる
            for (InvocationMethod invocation : property.getInvocationList()) {
                int line = invocation.getLineNumber();
                String color = getColor(coverage.getLine(line).getStatus());
                // helper かどうか調べるため，isInvoke の値をセット
                MethodProperty invocationProperty = methodList.getPropertyByBinding(invocation.getBinding());

                // ローカルと匿名に対応できれば
                // color の判定は red だけで良いはず
                if (invocationProperty == null) {
                    // 実行されていないアサーションの場合
                    String className = invocation.getBinding().getDeclaringClass().getQualifiedName();
                    String invokedMethodName = invocation.getBinding().getName();
                    if (assertions.isAssertion(className + "." + invokedMethodName)) {
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
