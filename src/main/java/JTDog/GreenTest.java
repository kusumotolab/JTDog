package JTDog;

import java.util.ArrayList;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import JTDog._static.StaticAnalyzer;
import JTDog.dynamic.DynamicAnalyzer;
import JTDog.fileop.JSONWriter;
import JTDog.fileop.JavaFileReader;
import JTDog.json.TestSmellList;
import JTDog.json.TestSmellProperty;

public class GreenTest extends DefaultTask {
    private Project project;

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    @TaskAction
    void analyzeJavaTests() throws Exception {
        String[] testDirs = project.findProperty("JTDog.testDirs").toString().split("\\s+");
        String[] sourcepathDirs = project.hasProperty("JTDog.sourceDirs")
                ? project.findProperty("JTDog.sourceDirs").toString().split("\\s+")
                : null;
        String[] classpaths = project.hasProperty("JTDog.classpaths")
                ? project.findProperty("JTDog.classpaths").toString().split("\\s+")
                : null;

        TestSmellList testSmells = new TestSmellList();

        String[] sources = JavaFileReader.getFilePaths(testDirs, "java");

        // 静的解析s
        StaticAnalyzer sa = new StaticAnalyzer(sources, sourcepathDirs, classpaths);
        sa.run(testSmells);

        // 動的解析
        DynamicAnalyzer da = new DynamicAnalyzer(sources);
        da.run();

        JSONWriter jw = new JSONWriter();
        ArrayList<TestSmellProperty> list = new ArrayList<>();
        testSmells.setList(list);
        /*
         * for (File file : fileArray) { TestSmellProperty tsp = new
         * TestSmellProperty(); tsp.setPath(file.getPath()); list.add(tsp); }
         */
        System.out.println(jw.toJSON(testSmells));

        jw.writeJSONFile(testSmells, "out");

        System.out.printf("%s !\n", getProject().getProjectDir().getPath());
    }
}