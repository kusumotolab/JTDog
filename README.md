# JTDog

JTDog is a Gradle plugin for dynamic test smells detection.

---

## Apply Gradle Plugin
Using the [plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block):
```groovy
plugins {
    id 'com.github.m-tanigt.jtdog' version '0.8.0'
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
    classpath "gradle.plugin.com.github.m-tanigt:jtdog-plugin:0.8.0"
  }
}

apply plugin: "com.github.m-tanigt.jtdog"
```

## Configuring The Plugin
```groovy
sniff {
  junitVersion = version
  rerunFailure = times
  runInRondomOrder = times
}
```
| Property | Type | Description |
|----------|------|-------------|
| junitVersion | integer | The JUnit version you use. Default to `4`. You can use JUnit5 by setting the value of this property to `5`. If you are using JUnit3, set it to `4` (because JUnit4 runner can run JUnit3 test).|
| rerunFailure | integer | Number of times to rerun a failed test. Default to `10`.|
| runInRondomOrder | integer | Number of times to run tests in random order. Default to `10`.|