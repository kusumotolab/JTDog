package jtdog;

import java.io.File;
import java.io.FilenameFilter;
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
import jtdog.dynamic.JUnitMemoryClassLoader;
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
    private int junitVersion = 4;
    @Input
    private int rerunsFailure = 10;
    @Input
    private int runsInRandomOrder = 10;
    @Input
    private boolean detectStaticSmells = false;
    @Input
    private String outputDirectory = "out";

    @TaskAction
    void sniffTaskAction() throws Exception {
        try {
            analyzeJavaTests(getProject());
        } catch (Exception e) {
            recursiveDeleteFile(new File("jtdog_tmp"));
        }
    }

    void analyzeJavaTests(Project project) throws Exception {
        final MethodList methodList = new MethodList();

        SourceSet mainSourceSet = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets()
                .getByName("main");
        SourceSet testSourceSet = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets()
                .getByName("test");

        // 外部 jar のパス
        final Set<File> externalJarFiles = FileReader.getExternalJarFiles(testSourceSet);// (project);
        final String[] externalJarFilePaths = FileSetConverter.toAbsolutePathArray(externalJarFiles);

        // property により JUnit5 かどうか判断
        boolean isJUnit5 = getJunitVersion() == 5 ? true : false;
        /*
         * if (project.hasProperty("junit5")) { isJUnit5 =
         * project.findProperty("junit5").equals("true") ? true : false; } else {
         * isJUnit5 = false; }
         */

        // 解析するソースコードのパス
        final Set<File> testSourceFiles = testSourceSet.getJava().getFiles();
        final String[] sources = FileSetConverter.toAbsolutePathArray(testSourceFiles);

        // java ファイルが直下にあるすべてのディレクトリのパス
        Set<File> sourceFiles = new HashSet<>();
        // sourceFiles.addAll(testSourceSet.getJava().getSrcDirs());
        // sourceFiles.addAll(mainSourceSet.getJava().getSrcDirs());
        addDirsContainingJava(sourceFiles, mainSourceSet);
        addDirsContainingJava(sourceFiles, testSourceSet);
        final String[] sourcepathDirs = FileSetConverter.toAbsolutePathArray(sourceFiles);
        // for (String string : sourcepathDirs) {
        // System.out.println("dir: "+string);
        // }

        System.out.println("Start static analysis.");
        // 静的解析
        final StaticAnalyzer sa = new StaticAnalyzer(sources, sourcepathDirs, externalJarFilePaths);
        sa.run(methodList, isJUnit5, detectStaticSmells);
        System.out.println("Done.");

        // classpath にソースファイルのパスを追加
        String testClassesDirPath = "";
        SourceSetOutput testOutput = testSourceSet.getOutput();
        Set<File> testClassesDirs = testOutput.getClassesDirs().getFiles();

        if (testClassesDirs.size() == 1) {
            for (File dir : testClassesDirs) {
                // System.out.println("test: " + dir.getAbsolutePath());
                testClassesDirPath = dir.getAbsolutePath();
            }
        } else {
            for (File dir : testClassesDirs) {
                // System.out.println("test: " + dir.getAbsolutePath());
                if (dir.getAbsolutePath().contains("/java/")) {
                    testClassesDirPath = dir.getAbsolutePath();
                    break;
                }
            }
        }

        // URLClassLoader 生成
        Set<File> classPaths = FileReader.getClassPaths(testSourceSet);
        URL[] urls = FileSetConverter.toURLs(classPaths);
        ClassLoader parent = SniffTask.class.getClassLoader();
        final JUnitMemoryClassLoader loader = new JUnitMemoryClassLoader(urls, parent, isJUnit5);
        Thread.currentThread().setContextClassLoader(loader);
        // 動的解析
        final DynamicAnalyzer da = new DynamicAnalyzer(sa.getTestClassNames(), sa.getTestClassNamesToExecuted(),
                testClassesDirPath);

        String projectName = "";
        Project p = getProject();
        while (p.getParent() != null) {
            projectName = p.getName() + ":" + projectName;
            p = p.getParent();
        }

        System.out.println("Start dynamic analysis.");

        da.run(methodList, loader, projectName, isJUnit5, getRerunsFailure(), getRunsInRandomOrder());

        System.out.println("Finish dynamic analysis.");

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
                if (testSmellTypes.contains(MethodProperty.DEPENDENT)) {
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

        jw.writeJSONFile(result, outputDirectory, projectName + "_result", !detectStaticSmells);
    }

    private void recursiveDeleteFile(final File file) throws Exception {
        // 存在しない場合は処理終了
        if (!file.exists()) {
            return;
        }
        // 対象がディレクトリの場合は再帰処理
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                recursiveDeleteFile(child);
            }
        }
        // 対象がファイルもしくは配下が空のディレクトリの場合は削除する
        file.delete();
    }

    private void addDirsContainingJava(Set<File> files, SourceSet sourceSet) {
        for (File file : sourceSet.getJava().getSrcDirs()) {
            if (containsJavaFile(file)) {
                files.add(file);
            }
        }
    }

    private boolean containsJavaFile(File root) {
        if (!root.exists()) {
            return false;
        }
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                if (name.endsWith(".java")) {
                    return true;
                }
                return false;
            }
        };

        if (root.list(filter).length > 0) {
            return true;
        }

        for (File file : root.listFiles()) {
            if (file.isDirectory() && containsJavaFile(file)) {
                return true;
            }
        }

        return false;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(final Project project) {
        this.project = project;
    }

    public int getJunitVersion() {
        return junitVersion;
    }

    public void setJunitVersion(int junitVersion) {
        this.junitVersion = junitVersion;
    }

    public int getRerunsFailure() {
        return rerunsFailure;
    }

    public void setRerunsFailure(int rerunFailure) {
        this.rerunsFailure = rerunFailure;
    }

    public int getRunsInRandomOrder() {
        return runsInRandomOrder;
    }

    public void setRunsInRandomOrder(int runInRandomOrder) {
        this.runsInRandomOrder = runInRandomOrder;
    }

    public boolean isDetectStaticSmells() {
        return detectStaticSmells;
    }

    public void setDetectStaticSmells(boolean detectStaticSmells) {
        this.detectStaticSmells = detectStaticSmells;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

}
