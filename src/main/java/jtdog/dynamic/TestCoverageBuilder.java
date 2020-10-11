package jtdog.dynamic;

import java.util.Map;

import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;

import jtdog.AssertionList;
import jtdog.method.MethodList;
import jtdog.method.MethodProperty;

public class TestCoverageBuilder extends CoverageBuilder {
    private final String testMethodName;
    private MethodList methodList;
    private AssertionList assertions;

    public TestCoverageBuilder(final String testMethodName, MethodList methodList, AssertionList assertions) {
        this.testMethodName = testMethodName;
        this.methodList = methodList;
        this.assertions = assertions;
    }

    @Override
    public void visitCoverage(final IClassCoverage coverage) {
        MethodProperty property = methodList.getMethodNameToProperty().get(testMethodName);
        // 実行されていない(color = "red" or "") invocation を含むか調べる
        for (String invocation : property.getInvocationList()) {
            int line = property.getInvocationToLineNumber().get(invocation);
            String color = getColor(coverage.getLine(line).getStatus());

            // helper かどうか調べるため，isInvoke の値をセット
            MethodProperty invProperty = methodList.getMethodNameToProperty().get(testMethodName);
            if (invProperty != null) {
                invProperty.setIsInvoked(true);
            }
            // 実行されていないアサーションの場合
            if (assertions.isAssertion(invocation)) {
                if (color.equals("red") || color.equals("")) {
                    property.addTestSmellType(MethodProperty.ROTTEN);
                    property.addCauseLine(line);
                }
                continue;
            }
            // 実行されていないアサーションを含む helper の場合
            Map<String, MethodProperty> map = methodList.getMethodNameToProperty();
            MethodProperty mp;
            if ((mp = map.get(invocation)) != null
                    && (mp.getHasAssertionDirectly() || mp.getHasAssertionIndirectly())) {
                if (color.equals("red") || color.equals("")) {
                    property.addTestSmellType(MethodProperty.ROTTEN);
                    property.addCauseLine(line);
                }
            }
        }

    }

    private String getColor(final int status) {
        switch (status) {
            case ICounter.NOT_COVERED:
                return "red";
            case ICounter.PARTLY_COVERED:
                return "yellow";
            case ICounter.FULLY_COVERED:
                return "green";
        }
        return "";
    }

}