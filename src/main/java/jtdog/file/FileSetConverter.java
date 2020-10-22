package jtdog.file;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

public class FileSetConverter {
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
     * for URLClassLoader
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
