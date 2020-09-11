package JTDog.json;

import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TestSmellList {
    @JsonProperty("list")
	private ArrayList<TestSmellProperty> list;

	public void setList(ArrayList<TestSmellProperty> l) {
		list = l;
	}
}
