package JTDog._static.method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodList {

	private List<String> methodNameList;
	private Map<String, MethodProperty> methodNameToProperty;

	public MethodList() {
		methodNameList = new ArrayList<>();
		methodNameToProperty = new HashMap<>();
	}

	public void addMethodName(String name) {
		methodNameList.add(name);
	}

	public List<String> getMethodNameList(){
		return methodNameList;
	}

	public void addMethodProperty(String name, MethodProperty property) {
		methodNameToProperty.put(name, property);
	}

	public Map<String, MethodProperty> getMethodNameToProperty(){
		return methodNameToProperty;
	}

}
