package JTDog._static;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import JTDog._static.method.MethodList;
import JTDog.fileop.JavaFileReader;
import JTDog.json.TestSmellList;
import JTDog.AssertionList;
import JTDog._static.method.MethodFilter;

public class StaticAnalyzer {
	// 解析対象のソースコード（複数可）
	private String[] sources;// = { "src/trial/UserTest.java" };
	private String[] sourceDirs;// = {"src/trial"};
	private String[] classpaths;// = {"src/trial"};

	private AssertionList assertions;

	public StaticAnalyzer(String[] _sources, String[] _sourceDirs, String[] _classpaths) {
		sources = JavaFileReader.getFilePaths(_sources, "java");
		sourceDirs = _sourceDirs;
		classpaths = _classpaths;
		assertions = new AssertionList();
	}

	/**
	 * analyze Java unit tests statically.
	 * @param testSmells	: list to save the analysis results.
	 * @throws IOException
	 */
    public void analyze(TestSmellList testSmells) throws IOException {
		// 解析器の生成
		ASTParser parser = ASTParser.newParser(AST.JLS14);

		// set options to treat assertion as keyword
		Map<String, String> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_4, options);
		parser.setCompilerOptions(options);

		parser.setResolveBindings(true);
		parser.setEnvironment(classpaths, sourceDirs, null, true);

		TestClassASTRequestor requestor = new TestClassASTRequestor();
		parser.createASTs(sources, null, new String[] {}, requestor, new NullProgressMonitor());

		// 対象ソースごとにASTの解析を行う
		for (CompilationUnit unit : requestor.units) {
			MethodList methodList = new MethodList();
			TestClassASTVisitor visitor = new TestClassASTVisitor(methodList, unit, assertions);
			unit.accept(visitor);

			// テストクラスはこれで取得できる
			List types = unit.types();
			TypeDeclaration typeDec = (TypeDeclaration) types.get(0);
			System.out.println("className:" + typeDec.getName());

			MethodFilter filter = new MethodFilter(methodList);
			filter.selectBadTests();

			List<String> badTests;
			// smoke test
			if((badTests = filter.getSmokeTestList()).size() != 0) {
				for (String name : badTests) {
					System.out.println("method "+ name +" is smoke test.");
				}
			} else {
				System.out.println("There are no smoke tests.");
			}

			// assertion free test
			if((badTests = filter.getAssertionFreeTestList()).size() != 0) {
				for (String name : badTests) {
					System.out.println("method "+ name +" is assertion free test.");
				}
			} else {
				System.out.println("There are no assertion free tests.");
			}

		}
	}
    
}