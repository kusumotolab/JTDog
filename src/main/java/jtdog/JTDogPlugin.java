/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package jtdog;

import org.gradle.StartParameter;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * A simple 'hello world' plugin.
 */
public class JTDogPlugin implements Plugin<Project> {
    public void apply(final Project project) {
        // Register a task
        project.getTasks().register("sniff", SniffOutJavaTests.class, task -> {
            /*
             * task.doFirst(s -> { if (!project.hasProperty("JTDog.testDirs")) { //
             * gradle.build に Sourceset がないと意味ない？ // final SourceSet test =
             * ((SourceSetContainer) //
             * project.getProperties().get("sourceSets")).getByName("test"); // これ使えばいける？ //
             * project.getDependencies(); throw new
             * GradleException("Please set the JTDog.testDirs property."); } });
             */
            System.out.println("home: " + StartParameter.DEFAULT_GRADLE_USER_HOME.getPath());
            task.setProject(project);

            task.doLast(s -> System.out.println("Done."));
        });
    }
}
