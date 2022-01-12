package jtdog.dynamic;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import jtdog.file.ObjectSerializer;

public class FlakyTestsDetector {
    private HashMap<String, Boolean> testResultsInDefaultOrder;

    private final HashSet<String> flakyTests;

    public FlakyTestsDetector() {
        this.flakyTests = new HashSet<>();
    }

    public HashSet<String> getTestDependencies() {
        return flakyTests;
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
            runJUnit5Tests(testClasses);

        } else {
            runJUnit4Tests(testClasses);
        }

        ObjectSerializer.serializeObject("jtdog_tmp/flakyTests.ser", flakyTests);
    }

    private void runJUnit5Tests(final List<Class<?>> testClasses)
            throws Exception {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(getTestClasses(testClasses))
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "true").build();
        Launcher launcher = LauncherFactory.create();

        TestExecutionListener listener = new TestExecutionListener() {
            @Override
            public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
                try {
                    MethodSource source = (MethodSource) testIdentifier.getSource().get();
                    String testMethodFQN = source.getClassName() + "." + source.getMethodName();
                    boolean defaultResult = testResultsInDefaultOrder.get(testMethodFQN);
                    boolean wasTestSuccessful = testExecutionResult.getStatus() == Status.SUCCESSFUL ? true : false;

                    // デフォルトの実行順でのテスト結果と異なる場合は flaky test
                    boolean isResultDifferent = defaultResult == wasTestSuccessful ? false : true;
                    if (isResultDifferent) {
                        flakyTests.add(testMethodFQN);
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                }
                TestExecutionListener.super.executionFinished(testIdentifier, testExecutionResult);
            }
        };
        launcher.execute(request, listener);

    }

    private List<DiscoverySelector> getTestClasses(List<Class<?>> testClasses) {
        List<DiscoverySelector> selectors = new ArrayList<>();
        for (Class<?> testClass : testClasses) {
            selectors.add(selectClass(testClass));
        }
        return selectors;
    }

    private void runJUnit4Tests(final List<Class<?>> testClasses) throws Exception {
        // Randomized Algorithm
        JUnitCore junit = new JUnitCore();
        FlakyTestDetectionListener listener = new FlakyTestDetectionListener();
        junit.addListener(listener);
        junit.run(testClasses.toArray(new Class<?>[testClasses.size()]));
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

    class FlakyTestDetectionListener extends RunListener {
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
            // System.out.println("failed in " + failure.getDescription().getMethodName() +
            // ": " + failure.getMessage());
            super.testFailure(failure);
        }

        @Override
        public void testFinished(Description description) throws Exception {
            boolean defaultResult = testResultsInDefaultOrder.get(getTestMethodFQN(description));

            // デフォルトの実行順でのテスト結果と異なる場合は flaky test
            boolean isResultDifferent = defaultResult == wasTestSuccessful ? false : true;
            if (isResultDifferent) {
                flakyTests.add(getTestMethodFQN(description));
            }
            super.testFinished(description);
        }

        private String getTestMethodFQN(final Description description) {
            return description.getClassName() + "." + description.getMethodName();
        }
    }
}
