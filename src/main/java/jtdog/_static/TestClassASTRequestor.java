package jtdog._static;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

public class TestClassASTRequestor extends FileASTRequestor {

    public List<CompilationUnit> units;

    public TestClassASTRequestor() {
        units = new ArrayList<CompilationUnit>();
    }

    @Override
    public void acceptAST(String path, CompilationUnit ast) {
        units.add(ast);
    }
}