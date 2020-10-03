package jtdog;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import jtdog._static.StaticAnalyzer;
import jtdog.dynamic.DynamicAnalyzer;
import jtdog.dynamic.MemoryClassLoader;
import jtdog.file.FileReader;
import jtdog.file.JSONWriter;
import jtdog.method.MethodList;
import jtdog.method.MethodProperty;

public class SniffTask extends DefaultTask {
    private Project project;

    public Project getProject() {
        return project;
    }

    public void setProject(final Project project) {
        this.project = project;
    }

    @TaskAction
    void analyzeJavaTests() throws Exception {
        // できれば project から取り出したい
        final String[] testDirs = project.hasProperty("JTDog.testDirs")
                ? project.findProperty("JTDog.testDirs").toString().split("\\s+")
                : null;
        /*
         * final String[] sourcepathDirs = project.hasProperty("JTDog.sourceDirs") ?
         * project.findProperty("JTDog.sourceDirs").toString().split("\\s+") : null;
         */

        // ユーザーホームの .gradle/caches/module-2 以下から jar ファイルが直下にある
        // ディレクトリのパスを全部渡す？
        /*
         * final String[] classpaths = project.hasProperty("JTDog.classpaths") ?
         * project.findProperty("JTDog.classpaths").toString().split("\\s+") : null;
         */

        // 外部 jar
        final Set<File> externalJarFiles = FileReader.getExternalJarFiles(getProject());
        final String[] classpaths = FileReader.toAbsolutePathArray(externalJarFiles);
        for (String string : classpaths) {
            System.out.println("ex jar: " + string);
        }

        final String projectDir = getProject().getProjectDir().getPath();
        final String[] test = { projectDir + "/src/test" };
        final String[] sources = FileReader.getFilePaths(test, "java");
        // final String[] sources = JavaFileReader.getFilePaths(testDirs, "java");

        // java ファイルが直下にあるすべてのディレクトリのパス
        // or ネストしたディレクトリに対してこのままでもいける？ 要調査
        final String[] sourcepathDirs = { projectDir + "/src/main/java" };

        final MethodList methodList = new MethodList();

        // 静的解析
        final StaticAnalyzer sa = new StaticAnalyzer(sources, sourcepathDirs, classpaths);
        sa.run(methodList);

        // 動的解析
        externalJarFiles.add(new File(projectDir + "/build/classes/java/main/"));
        // URLClassloader を使ってみるための準備
        URL[] urls = FileReader.toURLs(externalJarFiles);
        ClassLoader parent = DynamicAnalyzer.class.getClassLoader();// .getParent();
        final MemoryClassLoader memoryClassLoader = new MemoryClassLoader(urls, parent);

        /*
         * final DynamicAnalyzer da = new DynamicAnalyzer(sa.getTestClasses(),
         * projectDir); da.run(methodList, memoryClassLoader);
         */
        String runnerClassName = DynamicAnalyzer.class.getName();
        Class<?> runnerClass = memoryClassLoader.loadClass(runnerClassName);

        Object runner = runnerClass.getDeclaredConstructor(List.class, String.class).newInstance(sa.getTestClasses(),
                projectDir);
        Method method = runner.getClass().getMethod("run", MethodList.class, MemoryClassLoader.class);
        method.invoke(runner, methodList, memoryClassLoader);

        // generate result JSON file
        final TaskResult result = new TaskResult();
        final JSONWriter jw = new JSONWriter();
        final ArrayList<MethodProperty> list = new ArrayList<>();
        result.setList(list);

        for (String smell : methodList.getTestSmellList()) {
            list.add(methodList.getMethodNameToProperty().get(smell));
        }

        jw.writeJSONFile(result, "out", "result");
    }
}