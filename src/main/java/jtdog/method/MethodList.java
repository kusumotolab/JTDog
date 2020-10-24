package jtdog.method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.IMethodBinding;

public class MethodList {

    private final List<IMethodBinding> methodBindingList;
    private final Map<IMethodBinding, MethodProperty> methodBindingToProperty;

    public MethodList() {
        methodBindingList = new ArrayList<>();
        methodBindingToProperty = new HashMap<>();
    }

    public void addMethodBinding(final IMethodBinding binding) {
        methodBindingList.add(binding);
    }

    public List<IMethodBinding> getMethodBindingList() {
        return methodBindingList;
    }

    public void addMethodProperty(final IMethodBinding binding, final MethodProperty property) {
        methodBindingToProperty.put(binding, property);
    }

    public MethodProperty getPropertyByBinding(final IMethodBinding binding) {
        return methodBindingToProperty.get(binding);
    }

    // オーバーロードを考慮しない
    // テストメソッドはオーバーロードされないという前提で運用している
    public MethodProperty getPropertyByName(final String name) {
        for (IMethodBinding binding : methodBindingList) {
            MethodProperty property = getPropertyByBinding(binding);
            if (property.getBinaryName().equals(name)) {
                return property;
            }
        }
        return null;
    }

}
