package jtdog.method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodList {

    private final List<String> methodNameList;
    private final List<String> testSmellList;
    private final Map<String, MethodProperty> methodNameToProperty;

    public MethodList() {
        methodNameList = new ArrayList<>();
        testSmellList = new ArrayList<>();
        methodNameToProperty = new HashMap<>();
    }

    public void addMethodName(final String name) {
        methodNameList.add(name);
    }

    public List<String> getMethodNameList() {
        return methodNameList;
    }

    public void addTestSmell(final String name) {
        testSmellList.add(name);
    }

    public List<String> getTestSmellList() {
        return testSmellList;
    }

    public void addMethodProperty(final String name, final MethodProperty property) {
        methodNameToProperty.put(name, property);
    }

    public Map<String, MethodProperty> getMethodNameToProperty() {
        return methodNameToProperty;
    }

}
