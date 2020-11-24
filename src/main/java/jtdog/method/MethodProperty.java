package jtdog.method;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MethodProperty {

    public static final String IGNORED = "ignored";
    public static final String SMOKE = "smoke";
    public static final String ANNOTATION_FREE = "annotation_free";
    public static final String ROTTEN = "fully_rotten";
    public static final String CONTEXT_DEPENDENT = "rotten_context_dependent";
    public static final String MISSED_FAIL = "rotten_missed_fail";
    public static final String SKIP = "rotten_skip";
    public static final String EMPTY = "empty";
    public static final String FLAKY = "flaky";
    public static final String TEST_DEPENDENCY = "test_dependency";

    private boolean hasAssertionDirectly;
    private boolean hasAssertionIndirectly;
    private boolean hasTestAnnotation;
    private boolean hasIgnoreAnnotation;
    private boolean isMaybeTestMethod;
    private boolean isInvoked;
    private String color;
    private final List<InvocationMethod> invocationList;
    private String binaryName;
    private boolean isDeclaredInLocal;

    // TODO できれば別の方法でこの情報の保存をしたい
    private boolean hasContextDependentRottenAssertion;
    private boolean hasSkippedRottenAssertion;
    private boolean hasFullyRottenAssertion;

    @JsonProperty("test_class")
    private String className;
    @JsonProperty("test_name")
    private String name;
    @JsonProperty("start_line")
    private int startPosition;
    @JsonProperty("end_line")
    private int endPosition;
    @JsonProperty("test_smell_types")
    private final Set<String> testSmellTypes;
    @JsonProperty("rotten_lines")
    private final List<Integer> rottenLines;

    public MethodProperty() {
        invocationList = new ArrayList<>();
        testSmellTypes = new HashSet<>();
        rottenLines = new ArrayList<>();

        this.hasContextDependentRottenAssertion = false;
        this.hasSkippedRottenAssertion = false;
        this.hasFullyRottenAssertion = false;
    }

    public boolean hasAssertionDirectly() {
        return hasAssertionDirectly;
    }

    public void setHasAssertionDirectly(final boolean hasAssertionDirectly) {
        this.hasAssertionDirectly = hasAssertionDirectly;
    }

    public boolean hasAssertionIndirectly() {
        return hasAssertionIndirectly;
    }

    public void setHasAssertionIndirectly(final boolean hasAssertionIndirectly) {
        this.hasAssertionIndirectly = hasAssertionIndirectly;
    }

    public boolean hasTestAnnotation() {
        return hasTestAnnotation;
    }

    public void setHasTestAnnotation(final boolean hasTestAnnotation) {
        this.hasTestAnnotation = hasTestAnnotation;
    }

    public boolean hasIgnoreAnnotation() {
        return hasIgnoreAnnotation;
    }

    public void setHasIgnoreAnnotation(final boolean hasIgnoreAnnotation) {
        this.hasIgnoreAnnotation = hasIgnoreAnnotation;
    }

    @JsonIgnore
    public boolean isMaybeTestMethod() {
        return isMaybeTestMethod;
    }

    public void setIsMaybeTestMethod(final boolean isMaybeTestMethod) {
        this.isMaybeTestMethod = isMaybeTestMethod;
    }

    @JsonIgnore
    public boolean isInvoked() {
        return isInvoked;
    }

    public void setIsInvoked(boolean isInvoked) {
        this.isInvoked = isInvoked;
    }

    @JsonIgnore
    public List<InvocationMethod> getInvocationList() {
        return invocationList;
    }

    public void addInvocation(final InvocationMethod invocation) {
        this.invocationList.add(invocation);
    }

    @JsonIgnore
    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @JsonIgnore
    public String getBinaryName() {
        return binaryName;
    }

    public void setBinaryName(final String binaryName) {
        this.binaryName = binaryName;
    }

    @JsonIgnore
    public boolean isDeclaredInLocal() {
        return isDeclaredInLocal;
    }

    public void setIsDeclaredInLocal(boolean isDeclaredInLocal) {
        this.isDeclaredInLocal = isDeclaredInLocal;
    }

    @JsonIgnore
    public boolean hasContextDependentRottenAssertion() {
        return hasContextDependentRottenAssertion;
    }

    public void setHasContextDependentRottenAssertion(boolean hasContextDependentRottenAssertion) {
        this.hasContextDependentRottenAssertion = hasContextDependentRottenAssertion;
    }

    @JsonIgnore
    public boolean hasSkippedRottenAssertion() {
        return hasSkippedRottenAssertion;
    }

    public void setHasSkippedRottenAssertion(boolean hasSkippedRottenAssertion) {
        this.hasSkippedRottenAssertion = hasSkippedRottenAssertion;
    }

    @JsonIgnore
    public boolean hasFullyRottenAssertion() {
        return hasFullyRottenAssertion;
    }

    public void setHasFullyRottenAssertion(boolean hasFullyRottenAssertion) {
        this.hasFullyRottenAssertion = hasFullyRottenAssertion;
    }

    /*
     * 以下は JSON プロパティ関連メソッド
     */

    public Set<String> getTestSmellTypes() {
        return testSmellTypes;
    }

    public void addTestSmellType(final String testSmellType) {
        this.testSmellTypes.add(testSmellType);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(final String className) {
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(final int number) {
        this.startPosition = number;
    }

    public void addRottenLine(final int number) {
        this.rottenLines.add(number);
    }

    public int getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }

}
