package jtdog.file;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import jtdog.TaskResult;

public class JSONWriter {

    public void writeJSONFile(final Object obj, final String dirPath, final String fileName, boolean mixIn)
            throws JsonGenerationException, JsonMappingException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        // JSON 文字列をインデントして見やすくする
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        if (mixIn) {
            mapper.addMixInAnnotations(TaskResult.class, MixInForIgnoreStaticSmellDetectionResult.class);
        }
        final ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());

        new File(dirPath).mkdirs();
        writer.writeValue(new File(dirPath + "/" + fileName + ".json"), obj);
    }
}