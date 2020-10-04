package jtdog.method;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MethodFilter {

    private final MethodList methodList;

    // たぶん消す
    private final List<String> assertionFreeTestList;
    private final List<String> smokeTestList;
    //

    public MethodFilter(final MethodList ml) {
        this.methodList = ml;
        this.assertionFreeTestList = new ArrayList<>();
        this.smokeTestList = new ArrayList<>();
    }

    // methodList から (maybe) bad test を抽出
    // rotten も合わせて最後に全部検出していくようにしたい（メソッドを順番に結果に出力したいから）
    // hasAssertionIndirectly を切り分けて先に再帰的に設定するようにすればいい
    public void selectTestSmells() {
        for (final String name : methodList.getMethodNameList()) {
            final MethodProperty mp = methodList.getMethodNameToProperty().get(name);
            final boolean hasAssertionIndirectly = hasAssertionIndirectly(mp);
            mp.setHasAssertionIndirectly(hasAssertionIndirectly);

            // assertion free test
            if (!mp.getHasTestAnnotation() && mp.getIsMaybeTestMethod()
                    && (mp.getHasAssertionDirectly() || hasAssertionIndirectly)) {
                // assertionFreeTestList.add(name);
                this.methodList.addTestSmell(name);
                mp.addTestSmellType(MethodProperty.ASSERTION_FREE);
                continue;
            }
            // smoke test
            if (mp.getHasTestAnnotation() && mp.getIsMaybeTestMethod() && !mp.getHasAssertionDirectly()
                    && !hasAssertionIndirectly) {
                // smokeTestList.add(name);
                this.methodList.addTestSmell(name);
                mp.addTestSmellType(MethodProperty.SMOKE);
                continue;
            }
        }
    }
    /*
     * public List<String> getAssertionFreeTestList() { return
     * assertionFreeTestList; }
     * 
     * public List<String> getSmokeTestList() { return smokeTestList; }
     */

    /**
     * Recursively check for indirect inclusion of assertions and return the
     * results.
     * 
     * @param mp
     * @return
     */
    private boolean hasAssertionIndirectly(final MethodProperty mp) {
        boolean hasAssertion = false;
        final Map<String, MethodProperty> properties = methodList.getMethodNameToProperty();

        for (final String name : mp.getInvocationList()) {
            MethodProperty tmp;
            if ((tmp = properties.get(name)) == null) {
                continue;
            }

            if (tmp.getHasAssertionDirectly() || tmp.getHasAssertionIndirectly()) {
                hasAssertion = true;
                break;
            } else {
                hasAssertion = hasAssertionIndirectly(tmp);
            }
        }

        return hasAssertion;
    }
}
