# JTDog

JTDog is a Gradle plugin for dynamic test smells detection.

https://plugins.gradle.org/plugin/com.github.m-tanigt.jtdog

---

## Apply Gradle Plugin
Using the [plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block):
```groovy
plugins {
    id 'com.github.m-tanigt.jtdog' version '1.0.2'
}
```

Using [legacy plugin application](https://docs.gradle.org/current/userguide/plugins.html#sec:old_plugin_application):
```groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "com.github.m-tanigt:jtdog-plugin:1.0.2"
  }
}

apply plugin: "com.github.m-tanigt.jtdog"
```

## Task
JTDog provides a `sniff` task and sniff out test smells by executing this task.
```
$ ./gradlew sniff
```
The detection results are output as a JSON file in the `out` directory.


## Configuration
```groovy
sniff {
  junitVersion = 4
  rerunFailure = 10
  runInRondomOrder = 10
  detectStaticSmells = false
}
```
| Property | Type | Description |
|----------|------|-------------|
| `junitVersion` | `integer` | The JUnit version you use. Default to `4`. You can use JUnit5 by setting the value of this property to `5`. If you are using JUnit3, set it to `4` (because JUnit4 runner can run JUnit3 test).|
| `rerunFailure` | `integer` | Number of times to rerun a failed test. Default to `10`.|
| `runInRondomOrder` | `integer` | Number of times to run tests in random order. Default to `10`.|
| `detectStaticSmells` | `boolean` | Whether to detect static test smells. Default to `false`|

## Dynamic Test Smells
| Name | Description |
|------|------|
| `Fully Rotten` (Roten Green Test) | A test that passes, contains assertions (either directly, or indirectly through a helper), but in which at least one assertion is not executed. Rotten Green Test is divided into Four Categories. Fully Rotten do not fall into any of the following categories.
| `Context-dependent` (Rotten Green Test) | A test that contains conditionals with different assertions in the different branches. |
| `Missed Fail`  (Rotten Green Test) | A test that contains an assertion which was forced to fail. |
| `Skip Test` (Rotten Green Test) | A test that contains guards to stop their execution early under certain conditions. |
| `Flaky Test` | A test that exhibits both a passing and a failing result with the same code. |
| `Dependent Test` | A test that produces different results depending on the execution order. |

## Static Test Smells
JTDog can detect test smells other than the above.
These are **NOT** dynamic test smells.
This feature is in its testing phase.

| Name | Description |
| ---- | ---- |
| `Annotation Free Test` | A method considered to be test method. The depeloper forgot to add `@Test` annotation or commented it out.|
| `Empty Test` | A test that do not contain executable statements. |
| `Ignored Test` | A test annotated by the @Ignore. |
| `Smoke Test` | A test that do not contain any assertions. |
