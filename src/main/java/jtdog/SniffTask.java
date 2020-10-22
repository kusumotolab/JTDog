package jtdog;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import jtdog._static.StaticAnalyzer;
import jtdog.dynamic.DynamicAnalyzer;
import jtdog.dynamic.MemoryClassLoader;
import jtdog.file.FileReader;
import jtdog.file.FileSetConverter;
import jtdog.file.JSONWriter;
import jtdog.method.MethodList;
import jtdog.method.MethodProperty;

public class SniffTask extends DefaultTask {
    @Input
    private Project project;

    public Project getProject() {
        return project;
    }

    public void setProject(final Project project) {
        this.project = project;
    }

    @TaskAction
    void analyzeJavaTests() throws Exception {
        final MethodList methodList = new MethodList();
        // ここどうにかしたい
        // final AssertionList assertions = new
        // AssertionList(org.assertj.core.api.Assertions.class);
        final AssertionList assertions = new AssertionList(org.junit.Assert.class);

        // 外部 jar のパス
        final Set<File> externalJarFiles = FileReader.getExternalJarFiles(getProject());
        final String[] classpaths = FileSetConverter.toAbsolutePathArray(externalJarFiles);

        // 解析するソースコードのパス
        final String projectDir = getProject().getProjectDir().getPath();
        final String[] test = { projectDir + "/src/test" };
        final String[] sources = FileReader.getFilePaths(test, "java");

        // java ファイルが直下にあるすべてのディレクトリのパス
        // or ネストしたディレクトリに対してこのままでもいける？ 要調査
        final String[] sourcepathDirs = { projectDir + "/src/main/java" };

        // 静的解析
        final StaticAnalyzer sa = new StaticAnalyzer(sources, sourcepathDirs, classpaths);
        sa.run(methodList, assertions);

        // 動的解析
        externalJarFiles.add(new File(projectDir + "/build/classes/java/main/"));
        // externalJarFiles.add(new File(projectDir + "/build/classes/java/test/"));

        // URLClassLoader 生成
        URL[] urls = FileSetConverter.toURLs(externalJarFiles);
        ClassLoader parent = DynamicAnalyzer.class.getClassLoader();
        final MemoryClassLoader loader = new MemoryClassLoader(urls, parent);
        final DynamicAnalyzer da = new DynamicAnalyzer(sa.getTestClasses(), projectDir);
        da.run(methodList, assertions, loader);

        // generate result JSON file
        final TaskResult result = new TaskResult();
        final JSONWriter jw = new JSONWriter();
        final ArrayList<MethodProperty> list = new ArrayList<>();
        result.setList(list);

        int rotten = 0;
        int smoke = 0;
        int annotationFree = 0;
        // メソッドのリストから test smell を取り出す
        for (IMethodBinding method : methodList.getMethodBindingList()) {
            MethodProperty property = methodList.getPropertyByBinding(method);
            Set<String> testSmellTypes = property.getTestSmellTypes();
            if (testSmellTypes.size() != 0) {
                list.add(property);
                if (testSmellTypes.contains(MethodProperty.ROTTEN)) {
                    rotten++;
                }
                if (testSmellTypes.contains(MethodProperty.SMOKE)) {
                    smoke++;
                }
                if (testSmellTypes.contains(MethodProperty.ANNOTATION_FREE)) {
                    annotationFree++;
                }
            }
        }

        result.setNumberOfRotten(rotten);
        result.setNumberOfSmoke(smoke);
        result.setNumberOfAnnotationFree(annotationFree);

        jw.writeJSONFile(result, "out", "result");
    }
}