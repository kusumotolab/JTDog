package JTDog._static;


import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;

import JTDog.AssertionList;
import JTDog._static.method.MethodList;
import JTDog._static.method.MethodProperty;

public class TestClassASTVisitor extends ASTVisitor {

	private MethodList methodList;
	private CompilationUnit unit;

	private MethodProperty activeMethod; // 訪問中のメソッド呼び出しのスコープ解決用
	private MethodProperty previousActiveMethod; // ローカルクラス対策

	private AssertionList assertions;

	public TestClassASTVisitor(MethodList ml, CompilationUnit cu, AssertionList al) {
		methodList = ml;
		unit = cu;
		assertions = al;
	}

    // TypeDeclarationStatement: クラスの宣言
	@Override
	public boolean visit(TypeDeclarationStatement node) {
		/* activeMethod を保存する処理 */
		previousActiveMethod = activeMethod;
		return super.visit(node);
	}

	@Override
	public void endVisit(TypeDeclarationStatement node) {
		/* activeMethod を戻す処理 */
		activeMethod = previousActiveMethod;
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		System.out.println("MD: "+node.getName());
		System.out.println("Line: "+ unit.getLineNumber(node.getStartPosition()));

		// コンストラクタを除外
		if(!node.isConstructor()) {
			MethodProperty m = new MethodProperty();

			// アノテーションや private などの修飾子のリストを取得
			ArrayList<String> modifierList = new ArrayList<>();
			for (Object modifier : node.modifiers()) {
				modifierList.add(modifier.toString());
			}

			boolean hasTestAnnotation = modifierList.contains("@Test") ? true :false;
			// JUnit4 におけるテストメソッドの条件（@Test を除く）
			boolean isMaybeTestMethod = 
					(modifierList.contains("public")
						&& node.getReturnType2().toString().equals("void")
						&& node.parameters().size() == 0)
							? true : false;

			// MethodProperty を設定
			m.setHasAssertionDirectry(false); // この段階では直接アサーションを含むか不明のため
			m.setHasTestAnnotation(hasTestAnnotation);
			m.setIsMaybeTestMethod(isMaybeTestMethod);

			methodList.addMethodName(node.getName().getIdentifier());
			methodList.addMethodProperty(node.getName().getIdentifier() ,m);
			activeMethod = m;
		}

		return super.visit(node);
	}

	@Override
	public boolean visit(MethodInvocation node) {
		String invokedMethod = node.getName().getIdentifier();
		activeMethod.addInvocation(invokedMethod);
		// アサーションであるかどうかの判定
		if (assertions.isAssertion(invokedMethod)) {
			activeMethod.setHasAssertionDirectry(true);
		}

		return super.visit(node);
	}

}