package jtdog.method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MethodProperty {

    public static final String SMOKE = "smoke";
    public static final String ASSERTION_FREE = "assertion_free";
    public static final String ROTTEN = "rotten";

    @JsonIgnore
    private boolean hasAssertionDirectly;
    @JsonIgnore
    private boolean hasAssertionIndirectly;
    @JsonIgnore
    private boolean hasTestAnnotation;
    @JsonIgnore
    private boolean isMaybeTestMethod;
    @JsonIgnore
    private final List<String> invocationList;
    // ignore してもバグった名前で出力される（ignore がきかない）
    @JsonProperty
    private final Map<String, Integer> invocationToLineNumber;

    @JsonProperty("test_class")
    private String className;
    @JsonProperty("test_name")
    private String name;
    @JsonProperty("line")
    private int startPosition;
    @JsonProperty("test_smell_types")
    private final List<String> testSmellTypes;

    public MethodProperty() {
        invocationList = new ArrayList<>();
        testSmellTypes = new ArrayList<>();
        invocationToLineNumber = new HashMap<>();
    }

    public void setHasAssertionDirectly(final boolean b) {
        this.hasAssertionDirectly = b;
    }

    public boolean getHasAssertionDirectly() {
        return hasAssertionDirectly;
    }

    public void setHasAssertionIndirectly(final boolean b) {
        this.hasAssertionIndirectly = b;
    }

    public boolean getHasAssertionIndirectly() {
        return hasAssertionIndirectly;
    }

    public void setHasTestAnnotation(final boolean b) {
        this.hasTestAnnotation = b;
    }

    public boolean getHasTestAnnotation() {
        return hasTestAnnotation;
    }

    public void setIsMaybeTestMethod(final boolean b) {
        this.isMaybeTestMethod = b;
    }

    public boolean getIsMaybeTestMethod() {
        return isMaybeTestMethod;
    }

    public void addInvocation(final String name) {
        this.invocationList.add(name);
    }

    public List<String> getInvocationList() {
        return invocationList;
    }

    public void addInvocationLineNumber(final String invocation, final int line) {
        invocationToLineNumber.put(invocation, line);
    }

    public Map<String, Integer> getInvocationToLineNumbaer() {
        return invocationToLineNumber;
    }

    /*
     * 以下は JSON プロパティ関連
     */
    public void addTestSmellType(final String type) {
        this.testSmellTypes.add(type);
    }

    public void setTestClassName(final String name) {
        this.className = name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setSetStartPosition(final int number) {
        this.startPosition = number;
    }

    public int getStartPosition() {
        return startPosition;
    }

}
