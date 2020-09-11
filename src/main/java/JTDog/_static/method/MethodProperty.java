package JTDog._static.method;

import java.util.ArrayList;
import java.util.List;

public class MethodProperty {

	private boolean hasAssertionDirectry;
	private boolean hasTestAnnotation;
	private boolean isMaybeTestMethod;
	private List<String> invocationList;

	public MethodProperty() {
		invocationList = new ArrayList<>();
	}

	public void setHasAssertionDirectry(boolean b) {
		hasAssertionDirectry = b;
	}

	public boolean getHasAssertionDirectry() {
		return hasAssertionDirectry;
	}

	public void setHasTestAnnotation(boolean b) {
		hasTestAnnotation = b;
	}

	public boolean getHasTestAnnotation(){
		return hasTestAnnotation;
	}

	public void setIsMaybeTestMethod(boolean b) {
		isMaybeTestMethod = b;
	}

	public boolean getIsMaybeTestMethod(){
		return isMaybeTestMethod;
	}

	public void addInvocation(String name) {
		invocationList.add(name);
	}

	public List<String> getInvocationList(){
		return invocationList;
	}
}
