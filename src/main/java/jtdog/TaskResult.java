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
    @JsonProperty("ignored")
    private int numberOfIgnored;
    @JsonProperty("empty")
    private int numberOfEmpty;
    @JsonProperty("flaky")
    private int numberOfFlaky;
    @JsonProperty("test_dependency")
    private int numberOfTestDependency;

    @JsonProperty("test_smells")
    private ArrayList<MethodProperty> test_smells;

    public TaskResult() {
        this.numberOfRotten = 0;
        this.numberOfSmoke = 0;
        this.numberOfAnnotationFree = 0;
        this.numberOfIgnored = 0;
        this.numberOfEmpty = 0;
        this.numberOfFlaky = 0;
        this.numberOfTestDependency = 0;
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

    public void setNumberOfIgnored(int number) {
        this.numberOfIgnored = number;
    }

    public void setNumberOfEmpty(int number) {
        this.numberOfEmpty = number;
    }

    public void setNumberOfFlaky(int numberOfFlaky) {
        this.numberOfFlaky = numberOfFlaky;
    }

    public void setNumberOfTestDependency(int numberOfTestDependency) {
        this.numberOfTestDependency = numberOfTestDependency;
    }
}
