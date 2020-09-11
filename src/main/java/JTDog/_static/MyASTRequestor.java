package JTDog._static;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

public class MyASTRequestor extends FileASTRequestor {

	public List<CompilationUnit> units;

	public MyASTRequestor() {
		units = new ArrayList<CompilationUnit>();
	}

	public void acceptAST(String path, CompilationUnit ast) {
		units.add(ast);
	}
}