package jtdog.dynamic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Random;

import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;

public class TestDependencyDetector {
    private static final int SHUFFLE = 100;
    private static final int K_BOUNDED = 2;

    private final ArrayList<String> testDefaultOrder;
    private final HashMap<String, Boolean> testResultsInDefaultOrder;

    private final HashSet<String> testDependencies;

    public TestDependencyDetector(final ArrayList<String> testDefaultOrder,
            final HashMap<String, Boolean> testResultsInDefaultOrder) {
        this.testDefaultOrder = testDefaultOrder;
        this.testResultsInDefaultOrder = testResultsInDefaultOrder;

        this.testDependencies = new HashSet<>();
    }

    public HashSet<String> getTestDependencies() {
        return testDependencies;
    }

    //
    public void run(final Class<?> testClass) {
        // Reversal Algorithm
        // runTestInSpecifiedOrder(getReversalOrder(), testClass);

        // Randomized Algorithm
        // 自動生成テストでは最も効果的
        // そうでない場合
        // SHUFFLE が大きければ大きいほど効果的
        // SHUFFLE = 10 では Reversal よりは効果的
        // SHUFFLE = 100 では Reversal, Exhaustive よりも効果的
        // SHUFFLE = 1000 では他すべてよりかなり効果的
        runTestInSpecifiedOrder(getRandomizedOrder(), testClass);

        // Exhaustive Bounded Algorithm
        // Permutation<String> permutation = new Permutation<String>(testDefaultOrder);
        // for (ArrayList<String> list : permutation.permute(K_BOUNDED)) {
        // runTestInSpecifiedOrder(list, testClass);
        // }

        // Dependence-Aware Bounded Algorithm
        // not implemented
    }

    /**
     * 指定した ArrayList の順番に従ってテストメソッドを実行していき， 結果がデフォルトの順番による実行結果と異なる場合は test
     * dependency と判定
     * 
     * @param order
     * @param testClass
     */
    private void runTestInSpecifiedOrder(ArrayList<String> order, Class<?> testClass) {
        JUnitCore junit = new JUnitCore();
        for (String methodName : order) {
            Result result = junit.run(Request.method(testClass, methodName));
            boolean defaultResult = testResultsInDefaultOrder.get(methodName);

            // デフォルトの実行順でのテスト結果と異なる場合は test dependency
            boolean isResultDifferent = defaultResult && result.wasSuccessful() ? false : true;
            if (isResultDifferent) {
                testDependencies.add(methodName);
            }
        }
    }

    /**
     * テスト実行の順番の逆順リスト取得
     * 
     * @return
     */
    private ArrayList<String> getReversalOrder() {
        ArrayList<String> reversalOrder = new ArrayList<>();
        for (ListIterator<String> i = testDefaultOrder.listIterator(testDefaultOrder.size()); i.hasPrevious();) {
            reversalOrder.add(i.previous());
        }
        return reversalOrder;
    }

    /**
     * テスト実行の順番をランダムな順にしたリスト取得
     * 
     * @return
     */
    private ArrayList<String> getRandomizedOrder() {
        Random random = new Random();
        final ArrayList<String> randomizedOrder = new ArrayList<>();
        // SHUFFLE 回配列をシャッフルする
        for (int i = 0; i < SHUFFLE; i++) {
            final ArrayList<String> remaining = new ArrayList<>(testDefaultOrder);
            for (int j = 0; j < testDefaultOrder.size(); j++) {
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
        }

        return randomizedOrder;
    }

    class Permutation<E> {
        int count;
        ArrayList<E> original;
        ArrayList<ArrayList<E>> list;

        Permutation(ArrayList<E> original) {
            this.original = new ArrayList<>(original);
            this.list = new ArrayList<ArrayList<E>>();
        }

        public ArrayList<ArrayList<E>> permute(int k) {
            ArrayList<E> newOne = new ArrayList<E>();
            permute(newOne, original, k);
            return list;
        }

        private void permute(ArrayList<E> newOne, ArrayList<E> originalOne, int k) {
            int n = originalOne.size();
            if (newOne.size() == k) {
                list.add(newOne);
                return;
            }

            for (int i = 0; i < n; i++) {
                ArrayList<E> newList = addItem(originalOne.get(i), newOne);
                ArrayList<E> originalList = removeItem(i, originalOne);
                permute(newList, originalList, k);
            }
        }

        private ArrayList<E> addItem(E item, ArrayList<E> newOne) {
            ArrayList<E> list = new ArrayList<E>();
            list.addAll(newOne);
            list.add(item);
            return list;
        }

        private ArrayList<E> removeItem(int index, ArrayList<E> original) {
            ArrayList<E> list = new ArrayList<E>();
            list.addAll(original);
            list.remove(index);
            return list;
        }
    }

}
