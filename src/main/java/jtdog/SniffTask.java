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

        // 外部 jar のパス
        final Set<File> classPaths = FileReader.getExternalJarFiles(project);
        final String[] externalJarFilePaths = FileSetConverter.toAbsolutePathArray(classPaths);

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
        sa.run(methodList);

        // classpath にソースファイルのパスを追加
        SourceSetOutput mainOutput = mainSourceSet.getOutput();
        Set<File> mainClassesDirs = mainOutput.getClassesDirs().getFiles();

        for (File dir : mainClassesDirs) {
            classPaths.add(new File(dir.getAbsolutePath()));
        }

        // URLClassLoader 生成
        URL[] urls = FileSetConverter.toURLs(classPaths);
        ClassLoader parent = DynamicAnalyzer.class.getClassLoader();
        final MemoryClassLoader loader = new MemoryClassLoader(urls, parent);

        String testClassesDirPath = "";
        SourceSetOutput testOutput = testSourceSet.getOutput();
        Set<File> testClassesDirs = testOutput.getClassesDirs().getFiles();

        // 現在の方式ではディレクトリのパスとテストクラスのバイナリ名から .class ファイルを読み込むため，
        // テストクラスのビルド時の生成ファイルを出力するディレクトリが複数の場合はとりあえず解析不可能としておく
        if (testClassesDirs.size() == 1) {
            for (File dir : testClassesDirs) {
                testClassesDirPath = dir.getAbsolutePath();
            }
        } else {
            loader.close();
            throw new Exception();
        }

        // 動的解析
        final DynamicAnalyzer da = new DynamicAnalyzer(sa.getTestClassNames(), sa.getTestClassNamesToExecuted(),
                testClassesDirPath);
        da.run(methodList, loader);

        // generate result JSON file
        final TaskResult result = new TaskResult();
        final JSONWriter jw = new JSONWriter();
        final ArrayList<MethodProperty> list = new ArrayList<>();
        result.setList(list);

        int rotten = 0;
        int smoke = 0;
        int annotationFree = 0;
        int ignored = 0;
        int empty = 0;
        // メソッドのリストから test smell を取り出す
        for (MethodIdentifier identifier : methodList.getMethodIdentifierList()) {
            MethodProperty property = methodList.getPropertyByIdentifier(identifier);
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
                if (testSmellTypes.contains(MethodProperty.IGNORED)) {
                    ignored++;
                }
                if (testSmellTypes.contains(MethodProperty.EMPTY)) {
                    empty++;
                }
            }
        }

        result.setNumberOfRotten(rotten);
        result.setNumberOfSmoke(smoke);
        result.setNumberOfAnnotationFree(annotationFree);
        result.setNumberOfIgnored(ignored);
        result.setNumberOfEmpty(empty);

        jw.writeJSONFile(result, "out", "result");
    }
}