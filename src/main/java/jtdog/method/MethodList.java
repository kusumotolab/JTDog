package jtdog.method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodList {

    private final List<MethodIdentifier> methodIdentifierList;
    private final Map<MethodIdentifier, MethodProperty> methodIdentifierToProperty;

    public MethodList() {
        methodIdentifierList = new ArrayList<>();
        methodIdentifierToProperty = new HashMap<>();
    }

    public void addMethodIdentifier(final MethodIdentifier identifier) {
        methodIdentifierList.add(identifier);
    }

    public List<MethodIdentifier> getMethodIdentifierList() {
        return methodIdentifierList;
    }

    public void addMethodProperty(final MethodIdentifier identifier, final MethodProperty property) {
        methodIdentifierToProperty.put(identifier, property);
    }

    public MethodProperty getPropertyByIdentifier(final MethodIdentifier identifier) {
        return methodIdentifierToProperty.get(identifier);
    }

    // オーバーロードを考慮しない
    // テストメソッドはオーバーロードされないという前提で運用している
    public MethodProperty getPropertyByName(final String name) {
        for (MethodIdentifier identifier : methodIdentifierList) {
            MethodProperty property = getPropertyByIdentifier(identifier);
            if (property.getBinaryName().equals(name)) {
                return property;
            }
        }
        return null;
    }

}
