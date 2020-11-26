package jtdog.method;

public class InvocationMethod {
    private MethodIdentifier identifier;
    private int line;
    private boolean isInIfElseStatement;
    private boolean couldBeSkipped;
    private boolean isMissedFail;

    public InvocationMethod(MethodIdentifier identifier, int line) {
        this.identifier = identifier;
        this.line = line;
        this.isInIfElseStatement = false;
        this.couldBeSkipped = false;
        this.isMissedFail = false;
    }

    public MethodIdentifier getMethodIdentifier() {
        return identifier;
    }

    public int getLineNumber() {
        return line;
    }

    public boolean isInIfElseStatement() {
        return isInIfElseStatement;
    }

    public void setIsInIfElseStatement(boolean isInIfElseStatement) {
        this.isInIfElseStatement = isInIfElseStatement;
    }

    public boolean isCouldBeSkipped() {
        return couldBeSkipped;
    }

    public void setCouldBeSkipped(boolean couldBeSkipped) {
        this.couldBeSkipped = couldBeSkipped;
    }

    public boolean isMissedFail() {
        return isMissedFail;
    }

    public void setIsMissedFail(boolean isMissedFail) {
        this.isMissedFail = isMissedFail;
    }
}
