package JTDog._static;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import JTDog.AssertionList;
import JTDog._static.method.MethodFilter;
import JTDog._static.method.MethodList;
import JTDog.fileop.JavaFileReader;
import JTDog.json.TestSmellList;

public class StaticAnalyzer {
    // 解析対象のソースコード（複数可）
    private final String[] sources;// = { "src/trial/UserTest.java" };
    private final String[] sourceDirs;// = {"src/trial"};
    private final String[] classpaths;// = {"src/trial"};

    private final AssertionList assertions;

    public StaticAnalyzer(final String[] _sources, final String[] _sourceDirs, final String[] _classpaths) {
        this.sources = JavaFileReader.getFilePaths(_sources, "java");
        this.sourceDirs = _sourceDirs;
        this.classpaths = _classpaths;
        this.assertions = new AssertionList();
    }

    /**
     * analyze Java unit tests statically.
     * 
     * @param testSmells : list to save the analysis results.
     * @throws IOException
     */
    public void run(final TestSmellList testSmells) throws IOException {
        // 解析器の生成
        final ASTParser parser = ASTParser.newParser(AST.JLS14);

        // set options to treat assertion as keyword
        final Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_4, options);
        parser.setCompilerOptions(options);

        parser.setResolveBindings(true);
        parser.setEnvironment(classpaths, sourceDirs, null, true);

        final TestClassASTRequestor requestor = new TestClassASTRequestor();
        parser.createASTs(sources, null, new String[] {}, requestor, new NullProgressMonitor());

        // 対象ソースごとにASTの解析を行う
        for (final CompilationUnit unit : requestor.units) {
            final MethodList methodList = new MethodList();
            final TestClassASTVisitor visitor = new TestClassASTVisitor(methodList, unit, assertions);
            unit.accept(visitor);

            // テストクラスはこれで取得できる
            final List types = unit.types();
            final TypeDeclaration typeDec = (TypeDeclaration) types.get(0);
            System.out.println("className:" + typeDec.getName());

            final MethodFilter filter = new MethodFilter(methodList);
            filter.selectBadTests();

            List<String> badTests;
            // smoke test
            if ((badTests = filter.getSmokeTestList()).size() != 0) {
                for (final String name : badTests) {
                    System.out.println("method " + name + " is smoke test.");
                }
            } else {
                System.out.println("There are no smoke tests.");
            }

            // assertion free test
            if ((badTests = filter.getAssertionFreeTestList()).size() != 0) {
                for (final String name : badTests) {
                    System.out.println("method " + name + " is assertion free test.");
                }
            } else {
                System.out.println("There are no assertion free tests.");
            }

        }
    }

}