package jtdog.method;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.eclipse.jdt.core.dom.IMethodBinding;

public class MethodProperty {

    public static final String IGNORE = "ignore";
    public static final String SMOKE = "smoke";
    public static final String ANNOTATION_FREE = "annotation_free";
    public static final String ROTTEN = "rotten";

    private boolean hasAssertionDirectly;
    private boolean hasAssertionIndirectly;
    private boolean hasTestAnnotation;
    private boolean hasIgnoreAnnotation;
    private boolean isMaybeTestMethod;
    private boolean isInvoked;
    private String color;
    private final List<InvocationMethod> invocationList;
    private IMethodBinding binding;
    private String binaryName;
    private boolean isDeclaredInLocal;

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
        causeLines = new ArrayList<>();
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
    public IMethodBinding getBinding() {
        return binding;
    }

    public void setBinding(IMethodBinding binding) {
        this.binding = binding;
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

    public void setSetStartPosition(final int number) {
        this.startPosition = number;
    }

    public void addCauseLine(final int number) {
        this.causeLines.add(number);
    }
}
