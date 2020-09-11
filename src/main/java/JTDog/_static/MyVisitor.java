package JTDog._static;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;

import JTDog._static.method.MethodList;
import JTDog._static.method.MethodProperty;

public class MyVisitor extends ASTVisitor {

	private MethodList methodList;
	private CompilationUnit unit;

	private MethodProperty activeMethod; // 訪問中のメソッド呼び出しのスコープ解決用
	private MethodProperty previousActiveMethod; // ローカルクラス対策

	public MyVisitor(MethodList ml, CompilationUnit cu) {
		methodList = ml;
		unit = cu;
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

		if(!node.isConstructor()) {
			MethodProperty m = new MethodProperty();

			ArrayList<String> modifierList = new ArrayList<>();
			for (Object modifier : node.modifiers()) {
				modifierList.add(modifier.toString());
				System.out.println(modifier.toString());
			}

			boolean hasTestAnnotation = false;
			if(modifierList.contains("@Test")) {
				hasTestAnnotation = true;
			}

			boolean isMaybeTestMethod = false;
			if(modifierList.contains("public")
					&& node.getReturnType2().toString().equals("void")
					&& node.parameters().size() == 0) {
				isMaybeTestMethod = true;
			}

			System.out.println("bool: "+isMaybeTestMethod);

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

		// 呼び出し元メソッドが直接アサーションを含む場合
		// 良くない
		// assertThat, assertEquals などと一致するかを調べる
		if(invokedMethod.contains("assert")) {
			activeMethod.setHasAssertionDirectry(true);
        }

		return super.visit(node);
	}

}