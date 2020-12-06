package jtdog;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.TaskAction;

import jtdog.dynamic.TestDependencyDetector;
import jtdog.file.FileReader;
import jtdog.file.FileSetConverter;

public class DetectDependentTestsTask extends DefaultTask {
    @Input
    private Project project;

    public Project getProject() {
        return project;
    }

    public void setProject(final Project project) {
        this.project = project;
    }

    @TaskAction
    void detectTaskAction() throws Exception {
        SourceSet mainSourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets()
                .getByName("main");
        SourceSet testSourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets()
                .getByName("test");

        boolean isJUnit5;
        if (getProject().hasProperty("junit5")) {
            isJUnit5 = getProject().findProperty("junit5").equals("true") ? true : false;
        } else {
            isJUnit5 = false;
        }

        final Set<File> classPaths = FileReader.getExternalJarFiles(getProject());
        // classpath にソースファイルのパスを追加
        SourceSetOutput mainOutput = mainSourceSet.getOutput();
        Set<File> mainClassesDirs = mainOutput.getClassesDirs().getFiles();
        for (File dir : mainClassesDirs) {
            classPaths.add(new File(dir.getAbsolutePath()));
        }
        SourceSetOutput testOutput = testSourceSet.getOutput();
        Set<File> testClassesDirs = testOutput.getClassesDirs().getFiles();
        for (File dir : testClassesDirs) {
            classPaths.add(new File(dir.getAbsolutePath()));
        }

        URL[] urls = FileSetConverter.toURLs(classPaths);
        ClassLoader parent = DetectDependentTestsTask.class.getClassLoader();
        final URLClassLoader loader = new URLClassLoader(urls, parent);

        TestDependencyDetector detector = new TestDependencyDetector();
        detector.run(loader, isJUnit5);

    }
}
