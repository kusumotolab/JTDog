package jtdog.dynamic;

import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;

import jtdog.AssertionList;
import jtdog.method.InvocationMethod;
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

    // for (IClassCoverage cc : coverageBuilder.getClasses()) を使うようにすれば
    // ローカル・匿名に対応できる？
    // 1つのテスト内の全クラス（内部・ローカル・匿名）もanalyze し，以下の処理やれば良い気がする．
    @Override
    public void visitCoverage(final IClassCoverage coverage) {
        MethodProperty property = methodList.getPropertyByName(testMethodName);

        boolean tmp = false;
        if (tmp) {
            for (int i = coverage.getFirstLine(); i <= coverage.getLastLine(); i++) {
                System.out.printf("Line %s: %s%n", Integer.valueOf(i), getColor(coverage.getLine(i).getStatus()));
            }
        }

        // 実行されていない(color = "red" or "") invocation を含むか調べる
        for (InvocationMethod invocation : property.getInvocationList()) {
            int line = invocation.getLineNumber();
            String color = getColor(coverage.getLine(line).getStatus());
            // helper かどうか調べるため，isInvoke の値をセット
            MethodProperty invocationProperty = methodList.getPropertyByBinding(invocation.getBinding());

            if (invocationProperty == null) {
                // 実行されていないアサーションの場合
                String className = invocation.getBinding().getDeclaringClass().getQualifiedName();
                String invokedMethodName = invocation.getBinding().getName();
                if (assertions.isAssertion(className + "." + invokedMethodName)) {
                    if (color.equals("red") || color.equals("")) {
                        property.addTestSmellType(MethodProperty.ROTTEN);
                        property.addCauseLine(line);
                    }
                    continue;
                }
            } else {
                invocationProperty.setIsInvoked(true);
                // 実行されていないアサーションを含む helper の場合
                MethodProperty mp;
                if ((mp = methodList.getPropertyByBinding(invocation.getBinding())) != null
                        && (mp.getHasAssertionDirectly() || mp.getHasAssertionIndirectly())) {
                    if (color.equals("red") || color.equals("")) {
                        property.addTestSmellType(MethodProperty.ROTTEN);
                        property.addCauseLine(line);
                    }
                    /* else で helper の中を見に行くべき */
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