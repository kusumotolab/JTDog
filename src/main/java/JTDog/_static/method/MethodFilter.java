package JTDog._static.method;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MethodFilter {

	private MethodList methodList;
	private List<String> assertionFreeTestList;
	private List<String> smokeTestList;

	public MethodFilter(MethodList ml) {
		methodList = ml;
		assertionFreeTestList = new ArrayList<>();
		smokeTestList = new ArrayList<>();
	}

	// methodList から (maybe) bad test を抽出
	public void selectBadTests(){
		for (String name : methodList.getMethodNameList()) {
			MethodProperty mp = methodList.getMethodNameToProperty().get(name);
			// assertion free test
			boolean hasAssertionIndirectry = hasAssertionIndirectly(mp);
			if(!mp.getHasTestAnnotation() && mp.getIsMaybeTestMethod()
					&& (mp.getHasAssertionDirectry() || hasAssertionIndirectry)) {
				assertionFreeTestList.add(name);
				continue;
			}
			// smoke test
			if(mp.getHasTestAnnotation() && mp.getIsMaybeTestMethod()
					&& !mp.getHasAssertionDirectry() && !hasAssertionIndirectry) {
				smokeTestList.add(name);
				continue;
			}
		}
	}

	public List<String> getAssertionFreeTestList(){
		return assertionFreeTestList;
	}

	public List<String> getSmokeTestList(){
		return smokeTestList;
	}

	private boolean hasAssertionIndirectly(MethodProperty mp) {
		boolean hasAssertion = false;
		Map<String, MethodProperty> properties = methodList.getMethodNameToProperty();

		for (String name : mp.getInvocationList()) {
			MethodProperty tmp;
			if((tmp = properties.get(name)) == null) {
				continue;
			}

			if(tmp.getHasAssertionDirectry()) {
				hasAssertion = true;
				break;
			} else {
				hasAssertion = hasAssertionIndirectly(tmp);
			}
		}

		return hasAssertion;
	}
}
