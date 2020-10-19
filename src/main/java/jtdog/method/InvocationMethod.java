package jtdog.method;

import org.eclipse.jdt.core.dom.IMethodBinding;

public class InvocationMethod {
    private IMethodBinding binding;
    private int line;

    public InvocationMethod(IMethodBinding binding, int line) {
        this.binding = binding;
        this.line = line;
    }

    public IMethodBinding getBinding() {
        return binding;
    }

    public int getLineNumber() {
        return line;
    }
}
