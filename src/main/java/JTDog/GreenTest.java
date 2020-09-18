package JTDog;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import JTDog._static.StaticAnalyzer;
import JTDog.fileop.JSONWriter;
import JTDog.json.TestSmellList;
import JTDog.json.TestSmellProperty;

import org.gradle.api.Project;
import java.io.*;
import java.util.ArrayList;

public class GreenTest extends DefaultTask {
    private Project project;

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    @TaskAction
    void analyzeJavaTests() throws IOException {
        String[] testDirs = project.findProperty("JTDog.testDirs").toString().split("\\s+");
        String[] sourcepathDirs = project.hasProperty("JTDog.sourceDirs") ? project.findProperty("JTDog.sourceDirs").toString().split("\\s+") : null; 
        String[] classpaths = project.hasProperty("JTDog.classpaths") ? project.findProperty("JTDog.classpaths").toString().split("\\s+") : null;

        TestSmellList testSmells = new TestSmellList();

        // 静的解析
        StaticAnalyzer sa = new StaticAnalyzer(testDirs, sourcepathDirs, classpaths);
        sa.analyze(testSmells);

        JSONWriter jw = new JSONWriter();
        ArrayList<TestSmellProperty> list = new ArrayList<>();
        testSmells.setList(list);
/*
         for (File file : fileArray) {
				TestSmellProperty tsp = new TestSmellProperty();
				tsp.setPath(file.getPath());
				list.add(tsp);
			}
*/
        System.out.println(jw.toJSON(testSmells));

        jw.writeJSONFile(testSmells, "out");

        System.out.printf("%s !\n", getProject().getProjectDir().getPath()); 
    }
}