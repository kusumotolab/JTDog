package jtdog.file;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CoverageWriter {
    public static void write(String str, String name) {
        try {
            File file = new File("out_coverage/" + name + ".coverage");
            file.getParentFile().mkdirs();
            file.createNewFile();

            if (checkBeforeWriteFile(file)) {
                FileWriter fileWriter = new FileWriter(file, true);
                fileWriter.write(str + "\n");
                fileWriter.close();
            } else {
                System.out.println("ファイルに書き込めません");
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private static boolean checkBeforeWriteFile(File file) {
        if (file.exists()) {
            if (file.isFile() && file.canWrite()) {
                return true;
            }
        }

        return false;
    }
}
