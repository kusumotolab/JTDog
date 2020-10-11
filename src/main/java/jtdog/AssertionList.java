package jtdog;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class AssertionList {
    private final Set<String> assertions;
    // private final Class<?> assertionClass;

    public AssertionList(Class<?> assertionClass) {
        assertions = new HashSet<>();
        // 可変にしたい
        // assertionClass = org.junit.Assert.class;
        listUpAssertions(assertionClass);
    }

    /**
     * check if the specified method is assertion.
     * 
     * @param method : method name.
     * @return true if "method" is assertion, else false.
     */
    public boolean isAssertion(final String method) {
        return assertions.contains(method);
    }

    /**
     * list up assertion methods declared in specified Class.
     * 
     * @param c : class to check.
     */
    private void listUpAssertions(final Class<?> c) {
        for (final Method method : c.getMethods()) {
            assertions.add(c.getName() + "." + method.getName());
        }
    }
}
