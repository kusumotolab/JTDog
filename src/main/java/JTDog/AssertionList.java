package JTDog;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class AssertionList {
    private Set<String> assertions = new HashSet<>();

    public AssertionList() {
        Class ju = org.junit.Assert.class;
        listUpAssertions(ju);
    }

    /**
     * check if the specified method is assertion.
     * @param method : method name.
     * @return true if "method" is assertion, else false.
     */
    public boolean isAssertion(String method){
        return assertions.contains(method);
    }

    /**
     * list up assertion methods declared in specified Class.
     * @param c : class to check.
     */
    private void listUpAssertions(Class c) {
        for (Method method : c.getMethods()) {
            assertions.add(method.getName());
        }
    }
}
