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
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import jtdog.AssertionList;
import jtdog.method.InvocationMethod;
import jtdog.method.MethodList;
import jtdog.method.MethodProperty;

public class StaticAnalyzer {
    // 解析対象のソースコード（複数可）
    private final String[] sources;
    private final String[] sourceDirs;
    private final String[] classPaths;

    // Visitor でクラス宣言ごとに追加し，これを基に instrumenter を適用する
    private List<String> testClassNames;
    private List<String> testClassNamesToExecuted;

    public StaticAnalyzer(final String[] sources, final String[] sourceDirs, final String[] classPaths) {
        this.sources = sources;
        this.sourceDirs = sourceDirs;
        this.classPaths = classPaths;
        this.testClassNames = new ArrayList<>();
        this.testClassNamesToExecuted = new ArrayList<>();
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
        // for resolve bindings
        parser.setResolveBindings(true);
        parser.setEnvironment(classPaths, sourceDirs, null, true);

        final TestClassASTRequestor requestor = new TestClassASTRequestor();

        // ソースが多すぎると heap space error
        parser.createASTs(sources, null, new String[] {}, requestor, new NullProgressMonitor());
        // 対象ソースごとにASTの解析を行う
        for (final CompilationUnit unit : requestor.units) {
            final TypeDeclaration typeDec = (TypeDeclaration) unit.types().get(0);
            final ITypeBinding bind = typeDec.resolveBinding();
            testClassNamesToExecuted.add(bind.getBinaryName());

            final TestClassASTVisitor visitor = new TestClassASTVisitor(methodList, unit, assertions, testClassNames);
            unit.accept(visitor);

        }

        // 全クラスの AST を走査後，静的解析で検出できる test smell を検出
        detectTestSmellsStatically(methodList);
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

        for (final InvocationMethod invocation : mp.getInvocationList()) {
            MethodProperty tmp;
            // ユーザー定義のメソッドではない
            // あるいは再帰呼び出しを行う場合
            if ((tmp = methodList.getPropertyByBinding(invocation.getBinding())) == null
                    || mp.getBinding() == invocation) {
                continue;
            }

            if (tmp.hasAssertionDirectly() || tmp.hasAssertionIndirectly()) {
                hasAssertion = true;
                break;
            } else {
                hasAssertion = hasAssertionIndirectly(tmp, methodList);
            }
        }

        return hasAssertion;
    }

    /**
     * 静的解析で検出できる test smell を検出する
     * 
     * @param methodList
     */
    private void detectTestSmellsStatically(MethodList methodList) {
        for (final IMethodBinding method : methodList.getMethodBindingList()) {
            final MethodProperty property = methodList.getPropertyByBinding(method);
            final boolean hasAssertionIndirectly = hasAssertionIndirectly(property, methodList);
            property.setHasAssertionIndirectly(hasAssertionIndirectly);

            // ローカルクラスや匿名クラスで宣言されたメソッドの場合は絶対にテストメソッドではないのでスキップ
            // テストメソッドの条件を満たしていない場合もスキップ
            if (property.isDeclaredInLocal() || !property.isMaybeTestMethod()) {
                continue;
            }

            // ignore test
            // - has @Ignore
            if (property.hasIgnoreAnnotation()) {
                property.addTestSmellType(MethodProperty.IGNORE);
                continue;
            }

            // annotation free test
            // - do not have @Test
            // - is not helper
            // - contains assertion
            if (!property.hasTestAnnotation() && !property.isInvoked()
                    && (property.hasAssertionDirectly() || hasAssertionIndirectly)) {
                property.addTestSmellType(MethodProperty.ANNOTATION_FREE);
                continue;
            }
            // smoke test
            // - has @Test
            // - contains no assertions
            if (property.hasTestAnnotation() && !property.hasAssertionDirectly() && !hasAssertionIndirectly) {
                property.addTestSmellType(MethodProperty.SMOKE);
                continue;
            }
        }
    }

    public List<String> getTestClassNames() {
        return testClassNames;
    }

    public List<String> getTestClassNamesToExecuted() {
        return testClassNamesToExecuted;
    }

}