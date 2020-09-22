package jtdog.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Stream;

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
            // getPath に変えてみる？
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

}
