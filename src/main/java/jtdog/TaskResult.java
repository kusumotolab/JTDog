package jtdog;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

import jtdog.method.MethodProperty;

public class TaskResult {
    @JsonProperty("fully_rotten")
    private int numberOfRotten;
    @JsonProperty("rotten_context_dependent")
    private int numberOfContextDependent;
    @JsonProperty("rotten_missed_fail")
    private int numberOfMissedFail;
    @JsonProperty("rotten_skip_test")
    private int numberOfSkip;
    @JsonProperty("flaky_test")
    private int numberOfFlaky;
    @JsonProperty("dependent_test")
    private int numberOfTestDependency;
    @JsonProperty("smoke")
    private int numberOfSmoke;
    @JsonProperty("annotation_free")
    private int numberOfAnnotationFree;
    @JsonProperty("ignored")
    private int numberOfIgnored;
    @JsonProperty("empty")
    private int numberOfEmpty;

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
        this.numberOfContextDependent = 0;
        this.numberOfMissedFail = 0;
        this.numberOfSkip = 0;
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

    public void setNumberOfContextDependent(int numberOfContextDependent) {
        this.numberOfContextDependent = numberOfContextDependent;
    }

    public void setNumberOfMissedFail(int numberOfMissedFail) {
        this.numberOfMissedFail = numberOfMissedFail;
    }

    public void setNumberOfSkip(int numberOfSkip) {
        this.numberOfSkip = numberOfSkip;
    }
}
