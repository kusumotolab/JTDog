package jtdog.file;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class MixInForIgnoreStaticSmellDetectionResult {
    @JsonIgnore
    int numberOfSmoke;
    @JsonIgnore
    int numberOfAnnotationFree;
    @JsonIgnore
    int numberOfIgnored;
    @JsonIgnore
    int numberOfEmpty;
}
