package jtdog;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

import jtdog.method.MethodProperty;

public class TaskResult {
    @JsonProperty("rotten")
    private int numberOfRotten;
    @JsonProperty("smoke")
    private int numberOfSmoke;
    @JsonProperty("annotation_free")
    private int numberOfAnnotationFree;
    @JsonProperty("ignore")
    private int numberOfIgnore;

    @JsonProperty("test_smells")
    private ArrayList<MethodProperty> test_smells;

    public TaskResult() {
        this.numberOfRotten = 0;
        this.numberOfSmoke = 0;
        this.numberOfAnnotationFree = 0;
        this.numberOfIgnore = 0;
    }

    public void setList(ArrayList<MethodProperty> list) {
        this.test_smells = list;
    }

    public void setNumberOfSmoke(int number) {
        this.numberOfSmoke = number;
    }

    public void setNumberOfAnnotationFree(int number) {
        this.numberOfAnnotationFree = number;
    }

    public void setNumberOfRotten(int number) {
        this.numberOfRotten = number;
    }

    public void setNumberOfIgnore(int number) {
        this.numberOfIgnore = number;
    }
}
