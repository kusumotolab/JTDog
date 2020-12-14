package jtdog._static;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import jtdog.method.InvocationMethod;
import jtdog.method.MethodIdentifier;
import jtdog.method.MethodList;
import jtdog.method.MethodProperty;

public class TestClassASTVisitor extends ASTVisitor {

    private final MethodList methodList;
    private final CompilationUnit unit;
    private List<String> testClassNames;
    private final boolean isJUnit5;

    private MethodProperty activeMethod; // 訪問中のメソッド呼び出しのスコープ解決用
    private MethodProperty previousActiveMethod; // ローカルクラス対策

    private MethodProperty activeTopMethod;
    private final String testClassName;

    private boolean couldBeReturned;

    public TestClassASTVisitor(final MethodList methodList, final CompilationUnit unit,
            final List<String> testClassNames, final boolean isJUnit5) {
        this.methodList = methodList;
        this.unit = unit;
        this.testClassNames = testClassNames;
        this.isJUnit5 = isJUnit5;

        final AbstractTypeDeclaration dec = (AbstractTypeDeclaration) unit.types().get(0);
        final ITypeBinding bind = dec.resolveBinding();
        testClassName = bind.getBinaryName();
    }

    // クラス宣言（内部・ローカルクラス含む）
    @Override
    public boolean visit(final TypeDeclaration node) {
        previousActiveMethod = activeMethod;

        ITypeBinding binding = node.resolveBinding();
        testClassNames.add(binding.getBinaryName());

        return super.visit(node);
    }

    @Override
    public void endVisit(final TypeDeclaration node) {
        activeMethod = previousActiveMethod;
    }

    @Override
    public boolean visit(AnnotationTypeDeclaration node) {
        previousActiveMethod = activeMethod;

        ITypeBinding binding = node.resolveBinding();
        testClassNames.add(binding.getBinaryName());

        return super.visit(node);
    }

    @Override
    public void endVisit(AnnotationTypeDeclaration node) {
        activeMethod = previousActiveMethod;
    }

    @Override
    public boolean visit(EnumDeclaration node) {
        previousActiveMethod = activeMethod;

        ITypeBinding binding = node.resolveBinding();
        testClassNames.add(binding.getBinaryName());

        return super.visit(node);
    }

    @Override
    public void endVisit(EnumDeclaration node) {
        activeMethod = previousActiveMethod;
    }

    // 匿名クラス
    @Override
    public boolean visit(final AnonymousClassDeclaration node) {
        previousActiveMethod = activeMethod;

        ITypeBinding binding = node.resolveBinding();
        testClassNames.add(binding.getBinaryName());

        return super.visit(node);
    }

    @Override
    public void endVisit(final AnonymousClassDeclaration node) {
        activeMethod = previousActiveMethod;
    }

    // メソッド宣言
    @Override
    public boolean visit(final MethodDeclaration node) {
        // コンストラクタを除外
        if (!node.isConstructor()) {
            couldBeReturned = false;

            final MethodProperty property = new MethodProperty();
            final IMethodBinding methodBinding = node.resolveBinding();
            final ITypeBinding declaringClass = methodBinding.getDeclaringClass();
            String className = declaringClass.getBinaryName();

            final String methodName = className + "." + node.getName().getIdentifier();
            // アノテーションや private などの修飾子のリストを取得
            final ArrayList<String> modifierList = new ArrayList<>();
            for (final Object modifier : node.modifiers()) {
                modifierList.add(modifier.toString());
            }

            Pattern test = Pattern.compile("^@Test");
            final boolean hasTestAnnotation = modifierList.stream().anyMatch(e -> test.matcher(e).find());
            Pattern ignore = isJUnit5 ? Pattern.compile("^@Disabled") : Pattern.compile("^@Ignore");
            final boolean hasIgnoreAnnotation = modifierList.stream().anyMatch(e -> ignore.matcher(e).find());
            final boolean isInvoked;
            final boolean isMaybeTestMethod;
            if (isJUnit5) {
                isInvoked = modifierList.contains("@BeforeEach") | modifierList.contains("@AfterEach")
                        | modifierList.contains("@BeforeAll") | modifierList.contains("@AfterAll") ? true : false;
                isMaybeTestMethod = (node.getReturnType2().toString().equals("void") && node.parameters().size() == 0)
                        ? true
                        : false;
            } else {
                isInvoked = modifierList.contains("@Before") | modifierList.contains("@After")
                        | modifierList.contains("@BeforeClass") | modifierList.contains("@AfterClass") ? true : false;
                isMaybeTestMethod = (modifierList.contains("public") && node.getReturnType2().toString().equals("void")
                        && node.parameters().size() == 0) ? true : false;
            }

            // MethodProperty を設定
            property.setHasAssertionDirectly(false); // この段階ではアサーションを含むか不明のため
            property.setHasAssertionIndirectly(false); // 同上
            property.setIsInvoked(isInvoked); // 同様の理由
            property.setHasTestAnnotation(hasTestAnnotation);
            property.setHasIgnoreAnnotation(hasIgnoreAnnotation);
            property.setIsMaybeTestMethod(isMaybeTestMethod);
            property.setBinaryName(methodName);

            activeMethod = property;
            if (!declaringClass.isLocal()) {
                activeTopMethod = property;
                property.setIsDeclaredInLocal(false);
            } else {
                property.setIsDeclaredInLocal(true);
            }

            // JSON プロパティ
            property.setName(node.getName().getIdentifier());
            property.setStartPosition(unit.getLineNumber(node.getStartPosition()));
            property.setEndPosition(unit.getLineNumber(node.getStartPosition() + node.getLength()));
            property.setClassName(testClassName);

            // メソッドのリストに追加
            MethodIdentifier identifier = new MethodIdentifier(methodBinding);
            methodList.addMethodIdentifier(identifier);
            methodList.addMethodProperty(identifier, property);
        }

        return super.visit(node);
    }

    // メソッド呼び出し
    @Override
    public boolean visit(final MethodInvocation node) {
        if (activeMethod != null) {
            final IMethodBinding methodBinding = node.resolveMethodBinding();
            if (methodBinding == null) {
                return super.visit(node);
            }
            MethodIdentifier identifier = new MethodIdentifier(methodBinding);
            InvocationMethod invocation = new InvocationMethod(identifier, unit.getLineNumber(node.getStartPosition()));
            if (isInIfElseStatement(node)) {
                invocation.setIsInIfElseStatement(true);
            }
            if (couldBeReturned) {
                invocation.setCouldBeSkipped(true);
            }

            // missed fail か調べる
            List<?> arguments = node.arguments();
            Expression argument = null;
            // 引数の数が 1 ならその引数を取り出す
            if (arguments.size() == 1) {
                argument = (Expression) arguments.get(0);
                // 引数の数が 2（メッセージを含む）なら メッセージでない引数を取り出す
            } else if (arguments.size() == 2) {
                argument = isJUnit5 ? (Expression) arguments.get(0) : (Expression) arguments.get(1);
            }
            if (argument != null) {
                String argumentLowerCase = argument.toString().toLowerCase();
                String invocationIdentifier = node.getName().getIdentifier();
                if (invocationIdentifier.equals("assertFalse")) {
                    if (argumentLowerCase.equals("\"true\"") || argumentLowerCase.equals("true")
                            || argumentLowerCase.equals("boolean.true")) {
                        invocation.setIsMissedFail(true);
                    }
                } else if (invocationIdentifier.equals("assertTrue")) {
                    if (argumentLowerCase.equals("\"false\"") || argumentLowerCase.equals("false")
                            || argumentLowerCase.equals("boolean.false")) {
                        invocation.setIsMissedFail(true);
                    }
                }
            }

            activeMethod.addInvocation(invocation);

            if (activeMethod != activeTopMethod && activeTopMethod != null) {
                activeTopMethod.addInvocation(invocation);
            }

            // アサーションであるかどうかの判定
            if (node.getName().getIdentifier().startsWith("assert") && node.getNodeType() != ASTNode.ASSERT_STATEMENT) {
                activeMethod.setHasAssertionDirectly(true);
                if (activeMethod != activeTopMethod) {
                    activeTopMethod.setHasAssertionDirectly(true);
                }
            }

        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ReturnStatement node) {
        couldBeReturned = true;
        return super.visit(node);
    }

    private boolean isInIfElseStatement(final MethodInvocation node) {
        ASTNode parent = node.getParent();
        while (parent != null) {
            if (parent.getNodeType() == ASTNode.IF_STATEMENT) {
                IfStatement ifStatement = (IfStatement) parent;
                if (ifStatement.getElseStatement() != null) {
                    return true;
                }
            }
            parent = parent.getParent();
        }
        return false;
    }

}