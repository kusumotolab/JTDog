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
    private AssertionList assertionList;

    public TestCoverageBuilder(final String testMethodName, MethodList methodList) {
        this.testMethodName = testMethodName;
        this.methodList = methodList;
    }

    @Override
    public void visitCoverage(final IClassCoverage coverage) {
        System.out.printf("class name:   %s%n", coverage.getName());
        System.out.printf("class id:     %016x%n", Long.valueOf(coverage.getId()));
        System.out.printf("instructions: %s%n", Integer.valueOf(coverage.getInstructionCounter().getTotalCount()));
        System.out.printf("branches:     %s%n", Integer.valueOf(coverage.getBranchCounter().getTotalCount()));
        System.out.printf("lines:        %s%n", Integer.valueOf(coverage.getLineCounter().getTotalCount()));
        System.out.printf("methods:      %s%n", Integer.valueOf(coverage.getMethodCounter().getTotalCount()));
        System.out.printf("complexity:   %s%n%n", Integer.valueOf(coverage.getComplexityCounter().getTotalCount()));

        MethodProperty property = methodList.getMethodNameToProperty().get(testMethodName);
        for (String invocation : property.getInvocationList()) {
            if (assertionList.isAssertion(invocation)) {
                int line = property.getInvocationToLineNumbaer().get(invocation);
                if (getColor(coverage.getLine(line).getStatus()).equals("red")) {
                    property.addTestSmellType(MethodProperty.ROTTEN);
                }
            }

            Map<String, MethodProperty> map = methodList.getMethodNameToProperty();
            MethodProperty mp;
            if ((mp = map.get(invocation)) != null
                    && (mp.getHasAssertionDirectly() || mp.getHasAssertionIndirectly())) {
                int line = property.getInvocationToLineNumbaer().get(invocation);
                if (getColor(coverage.getLine(line).getStatus()).equals("red")) {
                    property.addTestSmellType(MethodProperty.ROTTEN);
                }
            }

        }

        /*
         * for (int i = coverage.getFirstLine(); i <= coverage.getLastLine(); i++) {
         * System.out.printf("Line %s: %s%n", Integer.valueOf(i),
         * getColor(coverage.getLine(i).getStatus())); }
         */
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