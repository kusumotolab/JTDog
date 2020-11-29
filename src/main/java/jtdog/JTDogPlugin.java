/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package jtdog;

import java.util.Set;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * A simple 'hello world' plugin.
 */
public class JTDogPlugin implements Plugin<Project> {
    public void apply(final Project project) {
        // Register a task
        Set<Project> subProjects = project.getSubprojects();

        if (subProjects.isEmpty()) {
            registerTasks(project, true);
        } else {
            for (Project subProject : subProjects) {
                registerTasks(subProject, false);
            }
        }

    }

    private void registerTasks(Project project, Boolean isRootProject) {
        // String sniff = getTaskName("sniff", isRootProject, project);
        String compileJava = getTaskName("compileJava", isRootProject, project);
        String compileTestJava = getTaskName("compileTestJava", isRootProject, project);
        // String detectDependentTests = getTaskName("detectDependentTests",
        // isRootProject, project);

        project.getTasks().register("sniff", SniffTask.class, task -> {
            task.dependsOn(compileJava);
            task.dependsOn(compileTestJava);
            task.setProject(project);
            task.setDescription("Detects Java test smells.");
            task.doLast(s -> System.out.println("Done."));
        });

        project.getTasks().register("detectDependentTests", DetectDependentTestsTask.class, task -> {
            task.setProject(project);
            task.setDescription("Do not use.");
        });
    }

    private String getTaskName(String taskName, Boolean isRootProject, Project project) {
        return isRootProject ? taskName : ":" + project.getName() + ":" + taskName;
    }
}
