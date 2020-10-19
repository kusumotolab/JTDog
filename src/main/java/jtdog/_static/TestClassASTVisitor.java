package jtdog._static;

import java.util.ArrayList;

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

    private MethodProperty activeMethod; // 訪問中のメソッド呼び出しのスコープ解決用
    private MethodProperty previousActiveMethod; // ローカルクラス対策

    private MethodProperty activeTopMethod;
    private MethodProperty previousActiveTopMethod;

    private final String testClassName;

    private String qualifiedActiveClassName;
    private String previousQualifiedClassName;

    private int anonymousClassNumber;
    private int localClassNumber;

    public TestClassASTVisitor(final MethodList methodList, final CompilationUnit unit,
            final AssertionList assertions) {
        this.methodList = methodList;
        this.unit = unit;
        this.assertions = assertions;
        this.anonymousClassNumber = 0;
        this.localClassNumber = 0;

        final TypeDeclaration typeDec = (TypeDeclaration) unit.types().get(0);
        final ITypeBinding bind = typeDec.resolveBinding();
        testClassName = bind.getQualifiedName();
    }

    // クラス（内部クラス含む）
    @Override
    public boolean visit(final TypeDeclaration node) {
        previousActiveMethod = activeMethod;

        String tmp = previousQualifiedClassName;
        previousQualifiedClassName = qualifiedActiveClassName;
        qualifiedActiveClassName = node.resolveBinding().getQualifiedName();
        // 内部クラスの場合
        if (qualifiedActiveClassName.isEmpty()) {
            if (tmp == null) {
                tmp = previousQualifiedClassName;
            }
            qualifiedActiveClassName = tmp + "$" + ++localClassNumber + node.getName();
        } else {
            //
            previousActiveTopMethod = activeTopMethod;
        }

        return super.visit(node);
    }

    @Override
    public void endVisit(final TypeDeclaration node) {
        activeMethod = previousActiveMethod;
        if (!node.resolveBinding().getQualifiedName().isEmpty()) {
            activeTopMethod = previousActiveTopMethod;
        }

        qualifiedActiveClassName = previousQualifiedClassName;
    }

    // 匿名クラス
    @Override
    public boolean visit(final AnonymousClassDeclaration node) {
        previousActiveMethod = activeMethod;
        previousActiveTopMethod = activeTopMethod;

        String tmp = previousQualifiedClassName;
        previousQualifiedClassName = qualifiedActiveClassName;
        qualifiedActiveClassName = tmp + "$" + ++anonymousClassNumber;

        return super.visit(node);
    }

    @Override
    public void endVisit(final AnonymousClassDeclaration node) {
        activeMethod = previousActiveMethod;
        activeTopMethod = previousActiveTopMethod;

        qualifiedActiveClassName = previousQualifiedClassName;
    }

    // メソッド宣言
    @Override
    public boolean visit(final MethodDeclaration node) {
        // IMethodBinding で管理すればローカル。匿名に対応できそうな気がする

        // コンストラクタを除外
        if (!node.isConstructor()) {
            final MethodProperty property = new MethodProperty();
            final IMethodBinding binding = node.resolveBinding();
            String className = binding.getDeclaringClass().getQualifiedName();
            boolean isInLocalClass = false;
            if (className.isEmpty()) {
                className = qualifiedActiveClassName;
                isInLocalClass = true;
            }
            final String identifier = node.getName().getIdentifier();
            final String methodName = className + "." + identifier;

            // アノテーションや private などの修飾子のリストを取得
            final ArrayList<String> modifierList = new ArrayList<>();
            for (final Object modifier : node.modifiers()) {
                modifierList.add(modifier.toString());
            }

            final boolean hasTestAnnotation = modifierList.contains("@Test") ? true : false;

            // JUnit4 におけるテストメソッドの条件（@Test を除く）
            final boolean isMaybeTestMethod = (modifierList.contains("public")
                    && node.getReturnType2().toString().equals("void") && node.parameters().size() == 0) ? true : false;

            // MethodProperty を設定
            property.setHasAssertionDirectly(false); // この段階ではアサーションを含むか不明のため
            property.setHasAssertionIndirectly(false); // 同上
            property.setIsInvoked(false); // 同様の理由
            property.setHasTestAnnotation(hasTestAnnotation);
            property.setIsMaybeTestMethod(isMaybeTestMethod);
            property.setBinding(binding);
            property.setQualifiedName(methodName);

            // JSON プロパティ
            property.setName(node.getName().getIdentifier());
            property.setSetStartPosition(unit.getLineNumber(node.getStartPosition()));
            property.setTestClassName(testClassName);

            methodList.addMethodBinding(binding);
            methodList.addMethodProperty(binding, property);
            activeMethod = property;
            if (!isInLocalClass) {
                activeTopMethod = property;
            }

        }

        return super.visit(node);
    }

    // メソッド呼び出し
    @Override
    public boolean visit(final MethodInvocation node) {
        if (activeMethod != null) {
            String invokedMethod;
            final IMethodBinding mb = node.resolveMethodBinding();
            if (mb == null) {
                invokedMethod = node.getName().getIdentifier();
                System.out.println("mb null: " + invokedMethod);
            } else {
                if (mb.getDeclaringClass() == null) {
                    invokedMethod = node.getName().getIdentifier();
                    System.out.println("dec null: " + invokedMethod);
                } else {
                    invokedMethod = mb.getDeclaringClass().getQualifiedName() + "." + node.getName().getIdentifier();
                    System.out.println("not null: " + invokedMethod + ", " + mb.getDeclaringClass().getName());
                }
            }

            // System.out.println("invoked: " + invokedMethod);
            System.out.println("active: " + activeMethod.getQualifiedName());

            InvocationMethod invocation = new InvocationMethod(mb, unit.getLineNumber(node.getStartPosition()));
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
            }

        }
        return super.visit(node);
    }

}