package jtdog.file;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;

public class FileReader {

    /**
     * get file paths with specified file extension (extension) under the specified
     * directories (rootDirs) recursively.
     * 
     * @param rootDirs  : root directoryies
     * @param extension : string needs to be contained as file extension
     * @return : array of file absolute paths
     */
    public static String[] getFilePaths(String[] rootDirs, String extension) {
        final ArrayList<String> fileList = new ArrayList<String>();
        for (String rootDir : rootDirs) {
            listUpFilePaths(Paths.get(rootDir), extension, fileList);
        }
        return fileList.toArray(new String[fileList.size()]);
    }

    /**
     * list up file paths with specified file extension (extension) under the
     * specified directory (rootDir) recursively, and add those to list.
     * 
     * @param rootDir   : root directory
     * @param extension : string needs to be contained as file extension
     * @param fileList  : list to add file paths
     * @return : list of file absolute paths
     */
    public static void listUpFilePaths(Path rootDir, String extension, ArrayList<String> fileList) {
        String extensionPattern = "." + extension.toLowerCase();
        try (final Stream<Path> pathStream = Files.walk(rootDir)) {
            pathStream.map(path -> path.toFile()).filter(file -> !file.isDirectory())
                    .filter(file -> file.getName().toLowerCase().endsWith(extensionPattern))
                    .map(file -> file.getAbsolutePath()).forEach(fileList::add);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * get externla jar files as Set<File>.
     * 
     * @param project
     * @return
     */
    public static Set<File> getExternalJarFiles(Project project) {
        Set<File> files = new HashSet<>();
        ConfigurationContainer container = project.getConfigurations();
        addFilesBySpecifiedConfiguration(files, container, "implementation");
        addFilesBySpecifiedConfiguration(files, container, "testImplementation");

        // setCanBeResolved を false にすればいける？
        // そもそも必要があるか調べる必要あり
        // addFilesBySpecifiedConfiguration(files, container, "compile");
        // addFilesBySpecifiedConfiguration(files, container, "testCompile");

        return files;
    }

    /**
     * for getExternalJarFiles() method.
     * 
     * @param files
     * @param container
     * @param name
     */
    private static void addFilesBySpecifiedConfiguration(Set<File> files, ConfigurationContainer container,
            String name) {
        Configuration configuration = container.getByName(name);
        if (configuration == null) {
            return;
        }
        configuration.setCanBeResolved(true);
        configuration.forEach(file -> {
            files.add(file);
        });
    }

    /**
     * for static analyzer.
     * 
     * @param files
     * @return
     */
    public static String[] toAbsolutePathArray(Set<File> files) {
        ArrayList<String> fileList = new ArrayList<>();
        for (File file : files) {
            fileList.add(file.getAbsolutePath());
        }
        return fileList.toArray(new String[fileList.size()]);
    }

    /**
     * for URLloader
     * 
     * @param files
     * @return
     */
    public static URL[] toURLs(Set<File> files) {
        URL[] urls = files.stream().map(file -> getURL(file)).collect(Collectors.toList()).toArray(new URL[0]);
        return urls;
    }

    /**
     * for toURLs
     * 
     * @param file
     * @return
     */
    private static URL getURL(File file) {
        try {
            return file.toURI().toURL();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
