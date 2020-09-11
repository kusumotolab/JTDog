package JTDog;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import JTDog._static.StaticAnalyzer;

import org.gradle.api.Project;
import java.io.*;

public class GreenTest extends DefaultTask {
    private Project project;

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    @TaskAction
    void analyzeJavaTests() {
        String[] testDirs = project.findProperty("testdirs").toString().split("\\s+");
        String[] sourcepathDirs = project.hasProperty("sourcedirs") ? project.findProperty("sourcedirs").toString().split("\\s+") : null; 
        String[] classpaths = project.hasProperty("classpaths") ? project.findProperty("classpaths").toString().split("\\s+") : null;

        // 静的解析
        StaticAnalyzer sa = new StaticAnalyzer(testDirs, sourcepathDirs, classpaths);
        try {
            sa.analyze();   
        } catch (IOException e) {
            System.err.println("IOEception.");
        }

        System.out.printf("%s !\n", getProject().getProjectDir().getPath()); 
    }
}