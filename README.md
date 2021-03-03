# JTDog

JTDog is a Gradle plugin for dynamic test smells detection.

---

## Apply Gradle Plugin
Using the [plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block):
```groovy
plugins {
    id 'com.github.m-tanigt.jtdog' version '1.0.0'
}
```

Using [legacy plugin application](https://docs.gradle.org/current/userguide/plugins.html#sec:old_plugin_application):
```groovy
buildscript {
    repositories {
        maven {
            url 'https://plugins.gradle.org/m2/'
        }
    }

    dependencies {
        classpath 'com.github.kusumotolab:jtdog-plugin:1.0.0'
    }
}

apply plugin: 'com.github.m-tanigt.jtdog'
```

