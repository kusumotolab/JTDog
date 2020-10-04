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
        this.assertionList = new AssertionList();
    }

    @Override
    public void visitCoverage(final IClassCoverage coverage) {
        System.out.printf("class name:   %s%n", coverage.getName());
        // System.out.printf("class id: %016x%n", Long.valueOf(coverage.getId()));
        // System.out.printf("instructions: %s%n",
        // Integer.valueOf(coverage.getInstructionCounter().getTotalCount()));
        // System.out.printf("branches: %s%n",
        // Integer.valueOf(coverage.getBranchCounter().getTotalCount()));
        // System.out.printf("lines: %s%n",
        // Integer.valueOf(coverage.getLineCounter().getTotalCount()));
        // System.out.printf("methods: %s%n",
        // Integer.valueOf(coverage.getMethodCounter().getTotalCount()));
        // System.out.printf("complexity: %s%n",
        // Integer.valueOf(coverage.getComplexityCounter().getTotalCount()));

        /*
         * for (int i = coverage.getFirstLine(); i <= coverage.getLastLine(); i++) {
         * System.out.printf("Line %s: %s%n", Integer.valueOf(i),
         * getColor(coverage.getLine(i).getStatus())); }
         */

        boolean isRotten = false;
        MethodProperty property = methodList.getMethodNameToProperty().get(testMethodName);
        for (String invocation : property.getInvocationList()) {
            System.out.println("** invocation: " + invocation + " ,size: " + property.getInvocationList().size());
            if (assertionList.isAssertion(invocation)) {
                System.out.println("assertion");
                int line = property.getInvocationToLineNumber().get(invocation);
                System.out.println("line: " + line + " ,color: " + getColor(coverage.getLine(line).getStatus()));
                String color = getColor(coverage.getLine(line).getStatus());
                if (color.equals("red") || color.equals("")) {
                    property.addTestSmellType(MethodProperty.ROTTEN);
                    isRotten = true;
                    System.out.println("rotten");
                }
                continue;
            }
            Map<String, MethodProperty> map = methodList.getMethodNameToProperty();
            MethodProperty mp;
            if ((mp = map.get(invocation)) != null
                    && (mp.getHasAssertionDirectly() || mp.getHasAssertionIndirectly())) {
                int line = property.getInvocationToLineNumber().get(invocation);
                System.out.println("line: " + line + " ,color: " + getColor(coverage.getLine(line).getStatus()));
                String color = getColor(coverage.getLine(line).getStatus());
                if (color.equals("red") || color.equals("")) {
                    property.addTestSmellType(MethodProperty.ROTTEN);
                    isRotten = true;
                    System.out.println("rotten");
                }
            }
        }

        if (isRotten) {
            this.methodList.addTestSmell(testMethodName);
        }

        System.out.print("\n\n");

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