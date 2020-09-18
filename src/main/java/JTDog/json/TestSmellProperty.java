package JTDog.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestSmellProperty {
	@JsonProperty("class")
	private String class_name;
    @JsonProperty("path")
	private String path;

	public void setPath(String p) {
		path = p;
	}
}
