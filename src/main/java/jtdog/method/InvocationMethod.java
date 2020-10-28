package jtdog.method;

public class InvocationMethod {
    private MethodIdentifier identifier;
    private int line;

    public InvocationMethod(MethodIdentifier identifier, int line) {
        this.identifier = identifier;
        this.line = line;
    }

    public MethodIdentifier getMethodIdentifier() {
        return identifier;
    }

    public int getLineNumber() {
        return line;
    }
}
