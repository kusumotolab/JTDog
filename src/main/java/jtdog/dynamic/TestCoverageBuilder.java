package jtdog.dynamic;

import java.util.ArrayList;
import java.util.HashMap;

import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;

public class TestCoverageBuilder extends CoverageBuilder {
    private HashMap<String, IClassCoverage> classNameToCoverage;
    private ArrayList<IClassCoverage> coverages;

    public TestCoverageBuilder(HashMap<String, IClassCoverage> classNameToCoverage,
            ArrayList<IClassCoverage> coverages) {
        this.classNameToCoverage = classNameToCoverage;
        this.coverages = coverages;
    }

    @Override
    public void visitCoverage(final IClassCoverage coverage) {
        String testClassName = coverage.getName().replace("/", ".");
        classNameToCoverage.put(testClassName, coverage);
        coverages.add(coverage);
    }

}