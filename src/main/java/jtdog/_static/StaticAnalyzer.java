package jtdog._static;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import jtdog.AssertionList;
import jtdog.method.MethodList;
import jtdog.method.MethodProperty;

public class StaticAnalyzer {
    // 解析対象のソースコード（複数可）
    private final String[] sources;
    private final String[] sourceDirs;
    private final String[] classpaths;

    private List<String> testClasses;

    public StaticAnalyzer(final String[] _sources, final String[] _sourceDirs, final String[] _classpaths) {
        this.sources = _sources;
        this.sourceDirs = _sourceDirs;
        this.classpaths = _classpaths;
        this.testClasses = new ArrayList<>();
    }

    /**
     * analyze Java unit tests statically.
     * 
     * @param testSmells : list to save the analysis results.
     * @throws IOException
     */
    public void run(final MethodList methodList, final AssertionList assertions) throws IOException {
        // 解析器の生成
        final ASTParser parser = ASTParser.newParser(AST.JLS14);

        // set options to treat assertion as keyword
        final Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_11, options);
        parser.setCompilerOptions(options);

        // for resoleve bindings
        parser.setResolveBindings(true);
        parser.setEnvironment(classpaths, sourceDirs, null, true);

        final TestClassASTRequestor requestor = new TestClassASTRequestor();
        parser.createASTs(sources, null, new String[] {}, requestor, new NullProgressMonitor());

        // 対象ソースごとにASTの解析を行う
        for (final CompilationUnit unit : requestor.units) {
            final TypeDeclaration typeDec = (TypeDeclaration) unit.types().get(0);
            final ITypeBinding bind = typeDec.resolveBinding();
            testClasses.add(bind.getQualifiedName());
            System.out.println("unit: " + bind.getQualifiedName());

            final TestClassASTVisitor visitor = new TestClassASTVisitor(methodList, unit, assertions);
            unit.accept(visitor);

        }

        // 全クラスの AST を走査後，静的解析で検出できる test smell を検出
        // smoke
        // assertion free
        // final MethodFilter filter = new MethodFilter(methodList);
        // filter.selectTestSmells();

        for (final String name : methodList.getMethodNameList()) {
            final MethodProperty mp = methodList.getMethodNameToProperty().get(name);
            final boolean hasAssertionIndirectly = hasAssertionIndirectly(mp, methodList);
            mp.setHasAssertionIndirectly(hasAssertionIndirectly);

            // assertion free test
            if (!mp.getHasTestAnnotation() && mp.getIsMaybeTestMethod() && !mp.getIsInvoked()
                    && (mp.getHasAssertionDirectly() || hasAssertionIndirectly)) {
                mp.addTestSmellType(MethodProperty.ASSERTION_FREE);
                continue;
            }
            // smoke test
            if (mp.getHasTestAnnotation() && mp.getIsMaybeTestMethod() && !mp.getHasAssertionDirectly()
                    && !hasAssertionIndirectly) {
                mp.addTestSmellType(MethodProperty.SMOKE);
                continue;
            }
        }
    }

    /**
     * Recursively check for indirect inclusion of assertions and return the
     * results.
     * 
     * @param mp
     * @return
     */
    private boolean hasAssertionIndirectly(final MethodProperty mp, MethodList methodList) {
        boolean hasAssertion = false;
        final Map<String, MethodProperty> properties = methodList.getMethodNameToProperty();
        String methodName = mp.getTestClassName() + "." + mp.getName();

        for (final String name : mp.getInvocationList()) {
            MethodProperty tmp;
            // ユーザー定義のメソッドではない
            // あるいは再帰呼び出しを行う場合
            if ((tmp = properties.get(name)) == null || methodName.equals(name)) {
                continue;
            }

            if (tmp.getHasAssertionDirectly() || tmp.getHasAssertionIndirectly()) {
                hasAssertion = true;
                break;
            } else {
                hasAssertion = hasAssertionIndirectly(tmp, methodList);
            }
        }

        return hasAssertion;
    }

    public List<String> getTestClasses() {
        return testClasses;
    }

}