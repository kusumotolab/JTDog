package jtdog.dynamic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.manipulation.Ordering;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class TestDependencyDetector {
    private static final int SHUFFLE = 100;

    private HashMap<String, Boolean> testResultsInDefaultOrder;

    private final HashSet<String> dependentTests;

    public TestDependencyDetector() {
        this.dependentTests = new HashSet<>();
    }

    public HashSet<String> getTestDependencies() {
        return dependentTests;
    }

    @SuppressWarnings("unchecked")
    public void run(MemoryClassLoader memoryClassLoader) throws Exception {
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
        for (Class<?> testClass : testClasses) {
            run(testClass);
        }
        serializeObject("jtdog_tmp/dependentTests.ser", dependentTests);
    }

    private void run(final Class<?> testClass) throws Exception {
        // Randomized Algorithm
        // 自動生成テストでは最も効果的
        // そうでない場合
        // SHUFFLE が大きければ大きいほど効果的
        // SHUFFLE = 10 では Reversal よりは効果的
        // SHUFFLE = 100 では Reversal, Exhaustive よりも効果的
        // SHUFFLE = 1000 では他すべてよりかなり効果的
        JUnitCore junit = new JUnitCore();
        DependentTestDetectionListener listener = new DependentTestDetectionListener();
        junit.addListener(listener);
        junit.run(Request.aClass(testClass).orderWith(new Ordering() {
            @Override
            protected List<Description> orderItems(Collection<Description> descriptions) {
                List<Description> ordered = getRandomizedOrder(new ArrayList<>(descriptions));
                return ordered;
            }
        }));
    }

    private List<Class<?>> loadTestClasses(MemoryClassLoader memoryClassLoader, List<String> testClassNamesToExecuted)
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
    private ArrayList<Description> getRandomizedOrder(ArrayList<Description> descriptions) {
        Random random = new Random();
        final ArrayList<Description> randomizedOrder = new ArrayList<>();
        ArrayList<Description> temp;
        // SHUFFLE 回配列をシャッフルする
        for (int i = 0; i < SHUFFLE; i++) {
            randomizedOrder.clear();
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
            temp = new ArrayList<>(randomizedOrder);
        }

        return randomizedOrder;
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
            System.out.println("failed in " + failure.getDescription().getMethodName() + ": " + failure.getMessage());
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
