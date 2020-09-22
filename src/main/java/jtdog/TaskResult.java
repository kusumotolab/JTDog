package jtdog;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

import jtdog.method.MethodProperty;

public class TaskResult {
    @JsonProperty("rotten")
    private int numberOfRotten;
    @JsonProperty("smoke")
    private int numberOfSmoke;
    @JsonProperty("assertion_free")
    private int numberOfAssertionFree;
    @JsonProperty("test_smells")
    private ArrayList<MethodProperty> test_smells;

    public TaskResult() {
        this.numberOfSmoke = 0;
        this.numberOfAssertionFree = 0;
    }

    public void setList(ArrayList<MethodProperty> list) {
        this.test_smells = list;
    }

    public void setNumberOfSmoke(int number) {
        this.numberOfSmoke = number;
    }

    public void setNumberOfAssertionFree(int number) {
        this.numberOfAssertionFree = number;
    }

    public void setNumberOfRotten(int number) {
        this.numberOfRotten = number;
    }
}
