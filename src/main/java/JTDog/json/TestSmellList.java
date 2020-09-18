package JTDog.json;

import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TestSmellList {
    @JsonProperty("test_smells")
	private ArrayList<TestSmellProperty> test_smells;

	public void setList(ArrayList<TestSmellProperty> l) {
		test_smells = l;
	}
}
