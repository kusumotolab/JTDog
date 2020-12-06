package jtdog;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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
import jtdog.method.MethodIdentifier;
import jtdog.method.MethodList;
import jtdog.method.MethodProperty;

public class SniffTask extends DefaultTask {
    @Input
    private Project project;
    @Input
    private boolean isRootProject;

    public Project getProject() {
        return project;
    }

    public void setProject(final Project project) {
        this.project = project;
    }

    public boolean isRootProject() {
        return isRootProject;
    }

    public void setIsRootProject(final boolean isRootProject) {
        this.isRootProject = isRootProject;
    }

    @TaskAction
    void sniffTaskAction() throws Exception {
        analyzeJavaTests(getProject());
    }

    void analyzeJavaTests(Project project) throws Exception {
        final MethodList methodList = new MethodList();

        SourceSet mainSourceSet = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets()
                .getByName("main");
        SourceSet testSourceSet = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets()
                .getByName("test");

        // 外部 jar のパス
        final Set<File> classPaths = FileReader.getExternalJarFiles(project);
        final String[] externalJarFilePaths = FileSetConverter.toAbsolutePathArray(classPaths);

        // property により JUnit5 かどうか判断
        boolean isJUnit5;
        if (project.hasProperty("junit5")) {
            isJUnit5 = project.findProperty("junit5").equals("true") ? true : false;
        } else {
            isJUnit5 = false;
        }

        // 解析するソースコードのパス
        final Set<File> testSourceFiles = testSourceSet.getJava().getFiles();
        final String[] sources = FileSetConverter.toAbsolutePathArray(testSourceFiles);

        // java ファイルが直下にあるすべてのディレクトリのパス
        Set<File> sourceFiles = new HashSet<>();
        sourceFiles.addAll(testSourceSet.getJava().getSrcDirs());
        sourceFiles.addAll(mainSourceSet.getJava().getSrcDirs());
        final String[] sourcepathDirs = FileSetConverter.toAbsolutePathArray(sourceFiles);

        // 静的解析
        final StaticAnalyzer sa = new StaticAnalyzer(sources, sourcepathDirs, externalJarFilePaths);
        sa.run(methodList, isJUnit5);

        // classpath にソースファイルのパスを追加
        SourceSetOutput mainOutput = mainSourceSet.getOutput();
        Set<File> mainClassesDirs = mainOutput.getClassesDirs().getFiles();

        for (File dir : mainClassesDirs) {
            System.out.println("main: " + dir.getAbsolutePath());
            classPaths.add(new File(dir.getAbsolutePath()));
        }

        // URLClassLoader 生成
        URL[] urls = FileSetConverter.toURLs(classPaths);
        ClassLoader parent = SniffTask.class.getClassLoader();
        final MemoryClassLoader loader = new MemoryClassLoader(urls, parent);

        String testClassesDirPath = "";
        SourceSetOutput testOutput = testSourceSet.getOutput();
        Set<File> testClassesDirs = testOutput.getClassesDirs().getFiles();

        // 現在の方式ではディレクトリのパスとテストクラスのバイナリ名から .class ファイルを読み込むため，
        // テストクラスのビルド時の生成ファイルを出力するディレクトリが複数の場合はとりあえず解析不可能としておく
        if (testClassesDirs.size() == 1) {
            for (File dir : testClassesDirs) {
                System.out.println("test: " + dir.getAbsolutePath());
                testClassesDirPath = dir.getAbsolutePath();
            }
        } else {
            for (File dir : testClassesDirs) {
                System.out.println("test: " + dir.getAbsolutePath());
                if (dir.getAbsolutePath().contains("/java/")) {
                    testClassesDirPath = dir.getAbsolutePath();
                    break;
                }
            }
        }

        // 動的解析
        final DynamicAnalyzer da = new DynamicAnalyzer(sa.getTestClassNames(), sa.getTestClassNamesToExecuted(),
                testClassesDirPath);
        String projectName = isRootProject() ? null : getProject().getName();
        da.run(methodList, loader, projectName, isJUnit5);

        // generate result JSON file
        final TaskResult result = new TaskResult();
        final JSONWriter jw = new JSONWriter();
        final ArrayList<MethodProperty> testSmellList = new ArrayList<>();
        result.setList(testSmellList);

        int rotten = 0;
        int smoke = 0;
        int annotationFree = 0;
        int ignored = 0;
        int empty = 0;
        int flaky = 0;
        int testDependency = 0;
        int contextDependent = 0;
        int missedFail = 0;
        int skip = 0;
        // メソッドのリストから test smell を取り出す
        for (MethodIdentifier identifier : methodList.getMethodIdentifierList()) {
            MethodProperty property = methodList.getPropertyByIdentifier(identifier);
            Set<String> testSmellTypes = property.getTestSmellTypes();
            if (testSmellTypes.size() != 0) {
                testSmellList.add(property);
                if (testSmellTypes.contains(MethodProperty.ROTTEN)) {
                    rotten++;
                }
                if (testSmellTypes.contains(MethodProperty.SMOKE)) {
                    smoke++;
                }
                if (testSmellTypes.contains(MethodProperty.ANNOTATION_FREE)) {
                    annotationFree++;
                }
                if (testSmellTypes.contains(MethodProperty.IGNORED)) {
                    ignored++;
                }
                if (testSmellTypes.contains(MethodProperty.EMPTY)) {
                    empty++;
                }
                if (testSmellTypes.contains(MethodProperty.FLAKY)) {
                    flaky++;
                }
                if (testSmellTypes.contains(MethodProperty.TEST_DEPENDENCY)) {
                    testDependency++;
                }
                if (testSmellTypes.contains(MethodProperty.CONTEXT_DEPENDENT)) {
                    contextDependent++;
                }
                if (testSmellTypes.contains(MethodProperty.MISSED_FAIL)) {
                    missedFail++;
                }
                if (testSmellTypes.contains(MethodProperty.SKIP)) {
                    skip++;
                }
            }
        }

        result.setNumberOfRotten(rotten);
        result.setNumberOfSmoke(smoke);
        result.setNumberOfAnnotationFree(annotationFree);
        result.setNumberOfIgnored(ignored);
        result.setNumberOfEmpty(empty);
        result.setNumberOfFlaky(flaky);
        result.setNumberOfTestDependency(testDependency);
        result.setNumberOfContextDependent(contextDependent);
        result.setNumberOfMissedFail(missedFail);
        result.setNumberOfSkip(skip);

        jw.writeJSONFile(result, "out", project.getName() + "_result");
    }
}