package jtdog;

import java.io.File;
import java.net.URL;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import jtdog.dynamic.FlakyTestsDetector;
import jtdog.dynamic.JUnitMemoryClassLoader;
import jtdog.file.FileReader;
import jtdog.file.FileSetConverter;

public class DetectFlakyTestsTask extends DefaultTask {
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
        SourceSet testSourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets()
                .getByName("test");

        boolean isJUnit5;
        if (getProject().hasProperty("jtdog.junit5")) {
            isJUnit5 = getProject().findProperty("jtdog.junit5").equals("true") ? true : false;
        } else {
            isJUnit5 = false;
        }

        // final Set<File> classPaths = FileReader.getExternalJarFiles(getProject());
        final Set<File> classPaths = FileReader.getClassPaths(testSourceSet);
        // classpath にソースファイルのパスを追加
        URL[] urls = FileSetConverter.toURLs(classPaths);
        ClassLoader parent = DetectDependentTestsTask.class.getClassLoader();
        // final URLClassLoader loader = new URLClassLoader(urls, parent);
        final JUnitMemoryClassLoader loader = new JUnitMemoryClassLoader(urls, parent, isJUnit5);
        Thread.currentThread().setContextClassLoader(loader);

        FlakyTestsDetector detector = new FlakyTestsDetector();
        detector.run(loader, isJUnit5);
    }
}
