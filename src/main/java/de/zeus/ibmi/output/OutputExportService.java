package de.zeus.ibmi.output;

import de.zeus.ibmi.transform.QueryResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OutputExportService {

    private final Map<String, OutputWriter> writersByFormat;

    public OutputExportService(Map<String, OutputWriter> writersByFormat) {
        this.writersByFormat = writersByFormat;
    }

    public List<Path> writeAll(QueryResult result, Path outputDirectory, String baseName, List<String> formats) {
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException e) {
            throw new OutputWriteException("Cannot create output directory: " + outputDirectory, e, List.of());
        }

        List<Path> writtenFiles = new ArrayList<>();
        for (String format : formats) {
            OutputWriter writer = writersByFormat.get(format);
            if (writer == null) {
                throw new OutputWriteException("Unsupported output format: " + format, writtenFiles);
            }

            String content;
            try {
                content = writer.render(result);
            } catch (RuntimeException e) {
                throw new OutputWriteException("Cannot render output format: " + format, e, writtenFiles);
            }
            Path filePath = outputDirectory.resolve(baseName + "." + format);
            try {
                Files.writeString(filePath, content, StandardCharsets.UTF_8);
                writtenFiles.add(filePath);
            } catch (IOException e) {
                throw new OutputWriteException("Cannot write output file: " + filePath, e, writtenFiles);
            }
        }
        return List.copyOf(writtenFiles);
    }
}
