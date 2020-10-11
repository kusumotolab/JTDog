package jtdog.method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MethodProperty {

    public static final String SMOKE = "smoke";
    public static final String ANNOTATION_FREE = "annotation_free";
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
    private boolean isInvoked;
    @JsonIgnore
    private String color;
    @JsonIgnore
    private final List<String> invocationList;
    @JsonProperty
    private final Map<String, Integer> invocationToLineNumber;

    @JsonProperty("test_class")
    private String className;
    @JsonProperty("test_name")
    private String name;
    @JsonProperty("line")
    private int startPosition;
    @JsonProperty("test_smell_types")
    private final Set<String> testSmellTypes;
    @JsonProperty("cause_of_smells")
    private final List<Integer> causeLines;

    public MethodProperty() {
        invocationList = new ArrayList<>();
        testSmellTypes = new HashSet<>();
        invocationToLineNumber = new HashMap<>();
        causeLines = new ArrayList<>();
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

    public void setIsInvoked(boolean b) {
        this.isInvoked = b;
    }

    public boolean getIsInvoked() {
        return isInvoked;
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

    public Map<String, Integer> getInvocationToLineNumber() {
        return invocationToLineNumber;
    }

    public void setColor(String c) {
        this.color = c;
    }

    public String getColor() {
        return color;
    }

    /*
     * 以下は JSON プロパティ関連
     */
    public void addTestSmellType(final String type) {
        this.testSmellTypes.add(type);
    }

    public Set<String> getTestSmellTypes() {
        return testSmellTypes;
    }

    public void setTestClassName(final String name) {
        this.className = name;
    }

    public String getTestClassName() {
        return className;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setSetStartPosition(final int number) {
        this.startPosition = number;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public void addCauseLine(final int number) {
        this.causeLines.add(number);
    }

}
