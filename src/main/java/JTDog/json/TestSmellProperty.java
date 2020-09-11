package JTDog.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestSmellProperty {
    @JsonProperty("path")
	private String path;

	public void setPath(String p) {
		path = p;
	}
}
