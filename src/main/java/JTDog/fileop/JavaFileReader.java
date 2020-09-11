package JTDog.fileop;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import JTDog.json.TestSmellList;
import JTDog.json.TestSmellProperty;

public class JavaFileReader {
    
    // 不要？
    // listUpFiles 使えば十分
    /**
     * get all java files under　the specified directory (dirPath)　recursively.
     * @param dirPath   : root directory
     * @return          : list of files
     */
    public File[] getAllJavaFilesUnderDirectory(String dirPath) throws IOException{
        Path rootDir = Paths.get(dirPath);
        String extension  = "java";

        if (!Pattern.matches("^[0-9a-zA-Z]+$", extension)) {
            System.err.println("Error: set correct extension. (only alphabet and numeric)");
        } else {
            ArrayList<File> fileList = listUpFiles(rootDir, extension);
            System.out.println(fileList.size());

            JSONWriter jw = new JSONWriter();
            TestSmellList testSmells = new TestSmellList();
            ArrayList<TestSmellProperty> list = new ArrayList<>();
            testSmells.setList(list);

            // convert to array
            File[] fileArray = fileList.toArray(new File[fileList.size()]);
            for (File file : fileArray) {
				//System.out.println(file.getAbsolutePath());
				TestSmellProperty tsp = new TestSmellProperty();
				tsp.setPath(file.getPath());
				list.add(tsp);
			}

            System.out.println(jw.toJSON(testSmells));

            return fileArray;
        }

        return null;
    }

    /**
     * list up files with specified file extension (extension)
     * under the specified directory (rootDir) recursively.
     * @param rootDir   : root directory
     * @param extension : string needs to be contained as file extension
     * @return          : list of files
     */
    public ArrayList<File> listUpFiles(Path rootDir, String extension){
        String extensionPattern = "." + extension.toLowerCase();
        final ArrayList<File> fileList = new ArrayList<File>();

        try (final Stream<Path> pathStream = Files.walk(rootDir)) {
            pathStream
                    .map(path -> path.toFile())
                    .filter(file -> !file.isDirectory())
                    .filter(file -> file.getName().toLowerCase().endsWith(extensionPattern))
                    .forEach(fileList::add);
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return (fileList);
    }
}
