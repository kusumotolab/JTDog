package jtdog._static;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import jtdog.AssertionList;
import jtdog.method.InvocationMethod;
import jtdog.method.MethodList;
import jtdog.method.MethodProperty;

public class TestClassASTVisitor extends ASTVisitor {

    private final MethodList methodList;
    private final CompilationUnit unit;
    private final AssertionList assertions;
    private List<String> testClassNames;

    private MethodProperty activeMethod; // 訪問中のメソッド呼び出しのスコープ解決用
    private MethodProperty previousActiveMethod; // ローカルクラス対策

    private MethodProperty activeTopMethod;
    private final String testClassName;

    public TestClassASTVisitor(final MethodList methodList, final CompilationUnit unit, final AssertionList assertions,
            final List<String> testClassNames) {
        this.methodList = methodList;
        this.unit = unit;
        this.assertions = assertions;
        this.testClassNames = testClassNames;

        final TypeDeclaration typeDec = (TypeDeclaration) unit.types().get(0);
        final ITypeBinding bind = typeDec.resolveBinding();
        testClassName = bind.getBinaryName();
    }

    // クラス宣言（内部・ローカルクラス含む）
    @Override
    public boolean visit(final TypeDeclaration node) {
        previousActiveMethod = activeMethod;

        ITypeBinding binding = node.resolveBinding();
        if (!binding.isLocal()) { // ローカルクラスでない場合
            // previousActiveTopMethod = activeTopMethod;

            final ArrayList<String> modifierList = new ArrayList<>();
            for (final Object modifier : node.modifiers()) {
                modifierList.add(modifier.toString());
            }
        }

        testClassNames.add(binding.getBinaryName());

        return super.visit(node);

    }

    @Override
    public void endVisit(final TypeDeclaration node) {
        activeMethod = previousActiveMethod;
        if (node.resolveBinding().isLocal()) {
            // activeTopMethod = previousActiveTopMethod;
        }

    }

    // 匿名クラス
    @Override
    public boolean visit(final AnonymousClassDeclaration node) {
        previousActiveMethod = activeMethod;
        // previousActiveTopMethod = activeTopMethod;

        ITypeBinding binding = node.resolveBinding();
        testClassNames.add(binding.getBinaryName());

        return super.visit(node);
    }

    @Override
    public void endVisit(final AnonymousClassDeclaration node) {
        activeMethod = previousActiveMethod;
        // activeTopMethod = previousActiveTopMethod;

    }

    // メソッド宣言
    @Override
    public boolean visit(final MethodDeclaration node) {
        // IMethodBinding で管理すればローカル。匿名に対応できそうな気がする

        // コンストラクタを除外
        if (!node.isConstructor()) {
            final MethodProperty property = new MethodProperty();
            final IMethodBinding methodBinding = node.resolveBinding();
            final ITypeBinding declaringClass = methodBinding.getDeclaringClass();
            String className = declaringClass.getBinaryName();

            final String identifier = node.getName().getIdentifier();
            final String methodName = className + "." + identifier;

            // アノテーションや private などの修飾子のリストを取得
            final ArrayList<String> modifierList = new ArrayList<>();
            for (final Object modifier : node.modifiers()) {
                modifierList.add(modifier.toString());
            }

            final boolean hasTestAnnotation = modifierList.contains("@Test") ? true : false;
            final boolean hasIgnoreAnnotation = modifierList.contains("@Ignore") ? true : false;

            // JUnit4 におけるテストメソッドの条件（@Test を除く）
            final boolean isMaybeTestMethod = (modifierList.contains("public")
                    && node.getReturnType2().toString().equals("void") && node.parameters().size() == 0) ? true : false;

            // MethodProperty を設定
            property.setHasAssertionDirectly(false); // この段階ではアサーションを含むか不明のため
            property.setHasAssertionIndirectly(false); // 同上
            property.setIsInvoked(false); // 同様の理由
            property.setHasTestAnnotation(hasTestAnnotation);
            property.setHasIgnoreAnnotation(hasIgnoreAnnotation);
            property.setIsMaybeTestMethod(isMaybeTestMethod);
            property.setBinding(methodBinding);
            property.setBinaryName(methodName);

            // JSON プロパティ
            property.setName(node.getName().getIdentifier());
            property.setSetStartPosition(unit.getLineNumber(node.getStartPosition()));
            property.setClassName(testClassName);

            methodList.addMethodBinding(methodBinding);
            methodList.addMethodProperty(methodBinding, property);
            activeMethod = property;
            if (!declaringClass.isLocal()) {
                activeTopMethod = property;
                property.setIsDeclaredInLocal(false);
            } else {
                property.setIsDeclaredInLocal(true);
            }

        }

        return super.visit(node);
    }

    // メソッド呼び出し
    @Override
    public boolean visit(final MethodInvocation node) {
        if (activeMethod != null) {
            final IMethodBinding methodBinding = node.resolveMethodBinding();
            final ITypeBinding declaringClass = methodBinding.getDeclaringClass();
            String invokedMethod = declaringClass.getBinaryName() + "." + node.getName().getIdentifier();

            InvocationMethod invocation = new InvocationMethod(methodBinding,
                    unit.getLineNumber(node.getStartPosition()));
            activeMethod.addInvocation(invocation);
            // activeMethod.addInvocationLineNumber(mb,
            // unit.getLineNumber(node.getStartPosition()));
            if (activeMethod != activeTopMethod) {
                activeTopMethod.addInvocation(invocation);
            }
            // activeTopMethod.addInvocationLineNumber(mb,
            // unit.getLineNumber(node.getStartPosition()));

            // アサーションであるかどうかの判定
            if (assertions.isAssertion(invokedMethod)) {
                activeMethod.setHasAssertionDirectly(true);
                if (activeMethod != activeTopMethod) {
                    activeTopMethod.setHasAssertionDirectly(true);
                }
            }

        }
        return super.visit(node);
    }

}