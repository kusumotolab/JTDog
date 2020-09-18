package JTDog.dynamic;

import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;

public class TestCoverageBuilder extends CoverageBuilder {

	@Override
	public void visitCoverage(final IClassCoverage coverage) {
		System.out.printf("class name:   %s%n", coverage.getName());
		System.out.printf("class id:     %016x%n", Long.valueOf(coverage.getId()));
		System.out.printf("instructions: %s%n", Integer
				.valueOf(coverage.getInstructionCounter().getTotalCount()));
		System.out.printf("branches:     %s%n",
				Integer.valueOf(coverage.getBranchCounter().getTotalCount()));
		System.out.printf("lines:        %s%n",
				Integer.valueOf(coverage.getLineCounter().getTotalCount()));
		System.out.printf("methods:      %s%n",
				Integer.valueOf(coverage.getMethodCounter().getTotalCount()));
		System.out.printf("complexity:   %s%n%n", Integer
				.valueOf(coverage.getComplexityCounter().getTotalCount()));

		// これを利用して rotten の判定をする
		for (int i = coverage.getFirstLine(); i <= coverage.getLastLine(); i++) {
				System.out.printf("Line %s: %s%n", Integer.valueOf(i),
						getColor(coverage.getLine(i).getStatus()));
				}
	}

	private String getColor(final int status) {
		switch (status) {
		case ICounter.NOT_COVERED:
			return "red";
		case ICounter.PARTLY_COVERED:
			return "yellow";
		case ICounter.FULLY_COVERED:
			return "green";
		}
		return "";
	}

}