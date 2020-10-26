package jtdog;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;
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
    void sniffTaskAction() throws Exception {
        Set<Project> subProjects = getProject().getSubprojects();
        if (subProjects.isEmpty()) {
            analyzeJavaTests(getProject());
        } else {
            for (Project subProject : subProjects) {
                analyzeJavaTests(subProject);
            }
        }
    }

    void analyzeJavaTests(Project project) throws Exception {
        final MethodList methodList = new MethodList();

        SourceSet mainSourceSet = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets()
                .getByName("main");
        SourceSet testSourceSet = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets()
                .getByName("test");

        // ここどうにかしたい
        // final AssertionList assertions = new
        // AssertionList(org.assertj.core.api.Assertions.class);
        final AssertionList assertions = new AssertionList(org.junit.Assert.class);

        // 外部 jar のパス
        final Set<File> classPaths = FileReader.getExternalJarFiles(project);
        final String[] externalJarFilePaths = FileSetConverter.toAbsolutePathArray(classPaths);

        // 解析するソースコードのパス
        // final String projectDir = getProject().getProjectDir().getPath();
        // final String[] test = { projectDir + "/src/test" };
        // final String[] sources = FileReader.getFilePaths(test, "java");
        final Set<File> testSourceFiles = testSourceSet.getJava().getFiles();
        final String[] sources = FileSetConverter.toAbsolutePathArray(testSourceFiles);

        // java ファイルが直下にあるすべてのディレクトリのパス
        // sourceSets.main.java.srcDirs から取るべき
        // sourceSets.test.output.classesDirs のようにクラスファイルも同様
        // final String[] sourcepathDirs = { projectDir + "/src/main/java" };
        final String[] sourcepathDirs = FileSetConverter.toAbsolutePathArray(mainSourceSet.getJava().getSrcDirs());

        // 静的解析
        final StaticAnalyzer sa = new StaticAnalyzer(sources, sourcepathDirs, externalJarFilePaths);
        sa.run(methodList, assertions);

        // 動的解析
        // classpath にソースファイルのパスを追加
        SourceSetOutput mainOutput = mainSourceSet.getOutput();
        Set<File> mainClassesDirs = mainOutput.getClassesDirs().getFiles();

        for (File dir : mainClassesDirs) {
            classPaths.add(new File(dir.getAbsolutePath()));
        }

        // classPaths.add(new File(projectDir + "/build/classes/java/main/"));

        // URLClassLoader 生成
        URL[] urls = FileSetConverter.toURLs(classPaths);
        ClassLoader parent = DynamicAnalyzer.class.getClassLoader();
        final MemoryClassLoader loader = new MemoryClassLoader(urls, parent);
        // final DynamicAnalyzer da = new DynamicAnalyzer(sa.getTestClassNames(),
        // sa.getTestClassNamesToExecuted(),
        // projectDir);

        String testClassesDirPath = "";
        SourceSetOutput testOutput = testSourceSet.getOutput();
        Set<File> testClassesDirs = testOutput.getClassesDirs().getFiles();
        if (testClassesDirs.size() == 1) {
            for (File dir : testClassesDirs) {
                testClassesDirPath = dir.getAbsolutePath();
            }
        } else {
            throw new Exception();
        }
        final DynamicAnalyzer da = new DynamicAnalyzer(sa.getTestClassNames(), sa.getTestClassNamesToExecuted(),
                testClassesDirPath);

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