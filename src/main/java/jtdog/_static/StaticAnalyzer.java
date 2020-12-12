package jtdog._static;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;

import jtdog.method.InvocationMethod;
import jtdog.method.MethodIdentifier;
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
    public void run(final MethodList methodList, final boolean isJUnit5) throws IOException {

        for (String source : sources) {
            // 解析器の生成
            final ASTParser parser = ASTParser.newParser(AST.JLS14);

            final Map<String, String> options = JavaCore.getOptions();
            JavaCore.setComplianceOptions(JavaCore.VERSION_14, options);
            parser.setCompilerOptions(options);
            // for resolve bindings
            parser.setResolveBindings(true);
            parser.setEnvironment(classPaths, sourceDirs, null, true);
            final TestClassASTRequestor requestor = new TestClassASTRequestor();

            // 引数に与えるソースが多すぎると OutOfMemoryException: heap space
            parser.createASTs(new String[] { source }, null, new String[] {}, requestor, new NullProgressMonitor());
            // 対象ソースごとにASTの解析を行う
            for (final CompilationUnit unit : requestor.units) {
                final AbstractTypeDeclaration dec = (AbstractTypeDeclaration) unit.types().get(0);
                // final TypeDeclaration typeDec = (TypeDeclaration) unit.types().get(0);
                final ITypeBinding bind = dec.resolveBinding();
                testClassNamesToExecuted.add(bind.getBinaryName());
                // System.out.println("unit: " + bind.getBinaryName());

                final TestClassASTVisitor visitor = new TestClassASTVisitor(methodList, unit, testClassNames, isJUnit5);
                unit.accept(visitor);
            }
        }

        // 全クラスの AST を走査後，静的解析で検出できる test smell を検出
        detectTestSmellsStatically(methodList);
    }

    /**
     * Recursively check for indirect inclusion of assertions and return the
     * results.
     * 
     * @param
     * @return
     */
    private boolean hasAssertionIndirectly(final MethodIdentifier identifier, final MethodProperty methodProperty,
            MethodList methodList) {
        boolean hasAssertion = false;

        for (final InvocationMethod invocation : methodProperty.getInvocationList()) {
            MethodProperty invocationProperty;
            // ユーザー定義のメソッドではない
            // あるいは再帰呼び出しを行う場合
            if ((invocationProperty = methodList.getPropertyByIdentifier(invocation.getMethodIdentifier())) == null
                    || identifier.equals(invocation.getMethodIdentifier())) {
                continue;
            }

            if (invocationProperty.hasAssertionDirectly() || invocationProperty.hasAssertionIndirectly()) {
                hasAssertion = true;
                break;
            } else {
                hasAssertion = hasAssertionIndirectly(invocation.getMethodIdentifier(), invocationProperty, methodList);
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
        for (final MethodIdentifier identifier : methodList.getMethodIdentifierList()) {
            final MethodProperty property = methodList.getPropertyByIdentifier(identifier);
            final boolean hasAssertionIndirectly = hasAssertionIndirectly(identifier, property, methodList);
            property.setHasAssertionIndirectly(hasAssertionIndirectly);

            // ローカルクラスや匿名クラスで宣言されたメソッドの場合は絶対にテストメソッドではないのでスキップ
            // テストメソッドの条件を満たしていない場合もスキップ
            if (property.isDeclaredInLocal() || !property.isMaybeTestMethod()) {
                continue;
            }

            // ignored test
            // - has @Ignore
            if (property.hasIgnoreAnnotation()) {
                property.addTestSmellType(MethodProperty.IGNORED);
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