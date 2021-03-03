package jtdog.dynamic;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.manipulation.Ordering;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import jtdog.file.ObjectSerializer;

public class TestDependencyDetector {
    private HashMap<String, Boolean> testResultsInDefaultOrder;

    private final HashSet<String> dependentTests;

    public TestDependencyDetector() {
        this.dependentTests = new HashSet<>();
    }

    public HashSet<String> getTestDependencies() {
        return dependentTests;
    }

    @SuppressWarnings("unchecked")
    public void run(URLClassLoader memoryClassLoader, final boolean isJUnit5) throws Exception {
        FileInputStream fileInputStream = new FileInputStream("jtdog_tmp/testResultsInDefaultOrder.ser");
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        testResultsInDefaultOrder = (HashMap<String, Boolean>) objectInputStream.readObject();
        objectInputStream.close();
        fileInputStream.close();

        fileInputStream = new FileInputStream("jtdog_tmp/testClassNamesToExecuted.ser");
        objectInputStream = new ObjectInputStream(fileInputStream);
        List<String> testClassNamesToExecuted = (List<String>) objectInputStream.readObject();
        objectInputStream.close();
        fileInputStream.close();

        List<Class<?>> testClasses = loadTestClasses(memoryClassLoader, testClassNamesToExecuted);

        if (isJUnit5) {
            fileInputStream = new FileInputStream("jtdog_tmp/testClassNameToDefaultExecutionOrder.ser");
            objectInputStream = new ObjectInputStream(fileInputStream);
            HashMap<String, ArrayList<String>> testClassNameToDefaultExecutionOrder = (HashMap<String, ArrayList<String>>) objectInputStream
                    .readObject();
            objectInputStream.close();
            fileInputStream.close();

            Launcher launcher = LauncherFactory.create();
            TestExecutionListener listener = new TestExecutionListener() {
                @Override
                public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
                    try {
                        MethodSource source = (MethodSource) testIdentifier.getSource().get();
                        String testMethodFQN = source.getClassName() + "." + source.getMethodName();
                        boolean defaultResult = testResultsInDefaultOrder.get(testMethodFQN);
                        boolean wasTestSuccessful = testExecutionResult.getStatus() == Status.SUCCESSFUL ? true : false;
                        if (testExecutionResult.getStatus() == Status.FAILED) {
                            //System.out.println("fail: " + testMethodFQN + ", " + testExecutionResult.getThrowable());
                        }

                        // デフォルトの実行順でのテスト結果と異なる場合は test dependency
                        boolean isResultDifferent = defaultResult == wasTestSuccessful ? false : true;
                        if (isResultDifferent) {
                            dependentTests.add(testMethodFQN);
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                    TestExecutionListener.super.executionFinished(testIdentifier, testExecutionResult);
                }
            };

            for (Class<?> testClass : testClasses) {
                ArrayList<String> order = testClassNameToDefaultExecutionOrder.get(testClass.getName());
                if (order != null) {
                    runJUnit5Tests(testClass, getRandomizedStringOrder(order), launcher, listener);
                }
            }
        } else {
            for (Class<?> testClass : testClasses) {
                runJUnit4Tests(testClass);
            }
        }

        ObjectSerializer.serializeObject("jtdog_tmp/dependentTests.ser", dependentTests);
    }

    private void runJUnit5Tests(final Class<?> testClass, final ArrayList<String> order, final Launcher launcher,
            final TestExecutionListener listener) {
        for (String methodName : order) {
            // System.out.println("name: " + methodName + " in " + testClass.getName());
            try {
                LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                        .selectors(selectMethod(testClass, methodName))
                        .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "true").build();
                launcher.execute(request, listener);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    private void runJUnit4Tests(final Class<?> testClass) throws Exception {
        // Randomized Algorithm
        JUnitCore junit = new JUnitCore();
        DependentTestDetectionListener listener = new DependentTestDetectionListener();
        junit.addListener(listener);
        junit.run(Request.aClass(testClass).orderWith(new Ordering() {
            @Override
            protected List<Description> orderItems(Collection<Description> descriptions) {
                List<Description> ordered = getRandomizedDescriptionOrder(new ArrayList<>(descriptions));
                return ordered;
            }
        }));
    }

    private List<Class<?>> loadTestClasses(URLClassLoader memoryClassLoader, List<String> testClassNamesToExecuted)
            throws ClassNotFoundException {
        final List<Class<?>> testClasses = new ArrayList<>();
        for (final String name : testClassNamesToExecuted) {
            final Class<?> targetClass = memoryClassLoader.loadClass(name);
            testClasses.add(targetClass);
        }
        return testClasses;
    }

    /**
     * テスト実行の順番をランダムな順にしたリスト取得
     * 
     * @return
     */
    private ArrayList<Description> getRandomizedDescriptionOrder(ArrayList<Description> descriptions) {
        Random random = new Random();
        final ArrayList<Description> randomizedOrder = new ArrayList<>();
        // SHUFFLE 回配列をシャッフルする
        final ArrayList<Description> remaining = new ArrayList<>(descriptions);
        for (int j = 0; j < descriptions.size(); j++) {
            final int remainingCount = remaining.size(); // 残っている要素の数
            final int index = random.nextInt(remainingCount); // ランダムに選択されたインデックス

            final Description element = remaining.get(index); // ランダムに選択された要素
            randomizedOrder.add(element); // ランダムに選択された要素のリストの末尾にランダムに選択された要素を追加する。

            final int lastIndex = remainingCount - 1; // 残っている要素のリストの末尾のインデックス
            final Description lastElement = remaining.remove(lastIndex); // 残っている要素のリストから末尾を削除する。
            if (index < lastIndex) { // ランダムに選択された要素が末尾以外なら…
                remaining.set(index, lastElement); // それを末尾の要素で置換する。
            }
        }

        return randomizedOrder;
    }

    private ArrayList<String> getRandomizedStringOrder(ArrayList<String> methodNames) {
        Random random = new Random();
        final ArrayList<String> randomizedOrder = new ArrayList<>();
        // SHUFFLE 回配列をシャッフルする
        final ArrayList<String> remaining = new ArrayList<>(methodNames);
        for (int j = 0; j < methodNames.size(); j++) {
            final int remainingCount = remaining.size(); // 残っている要素の数
            final int index = random.nextInt(remainingCount); // ランダムに選択されたインデックス

            final String element = remaining.get(index); // ランダムに選択された要素
            randomizedOrder.add(element); // ランダムに選択された要素のリストの末尾にランダムに選択された要素を追加する。

            final int lastIndex = remainingCount - 1; // 残っている要素のリストの末尾のインデックス
            final String lastElement = remaining.remove(lastIndex); // 残っている要素のリストから末尾を削除する。
            if (index < lastIndex) { // ランダムに選択された要素が末尾以外なら…
                remaining.set(index, lastElement); // それを末尾の要素で置換する。
            }
        }

        return randomizedOrder;
    }

    class DependentTestDetectionListener extends RunListener {
        private boolean wasTestSuccessful;

        @Override
        public void testStarted(Description description) throws Exception {
            // 初期化
            wasTestSuccessful = true;
            super.testStarted(description);
        }

        @Override
        public void testFailure(Failure failure) throws Exception {
            wasTestSuccessful = false;
            //System.out.println("failed in " + failure.getDescription().getMethodName() + ": " + failure.getMessage());
            super.testFailure(failure);
        }

        @Override
        public void testFinished(Description description) throws Exception {
            boolean defaultResult = testResultsInDefaultOrder.get(getTestMethodFQN(description));

            // デフォルトの実行順でのテスト結果と異なる場合は test dependency
            boolean isResultDifferent = defaultResult == wasTestSuccessful ? false : true;
            if (isResultDifferent) {
                dependentTests.add(getTestMethodFQN(description));
            }
            super.testFinished(description);
        }

        private String getTestMethodFQN(final Description description) {
            return description.getClassName() + "." + description.getMethodName();
        }
    }
}
