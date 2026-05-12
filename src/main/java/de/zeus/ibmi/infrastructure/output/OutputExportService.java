package de.zeus.ibmi.infrastructure.output;

import de.zeus.ibmi.transform.QueryResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OutputExportService {

  private final OutputWriterRegistry writerRegistry;

  public OutputExportService(OutputWriterRegistry writerRegistry) {
    this.writerRegistry = writerRegistry;
  }

  public OutputExportService(Map<String, OutputWriter> writersByFormat) {
    this(new OutputWriterRegistry(List.copyOf(writersByFormat.values())));
  }

  public List<Path> writeAll(
      QueryResult result, Path outputDirectory, String baseName, List<String> formats) {
    try {
      Files.createDirectories(outputDirectory);
    } catch (IOException e) {
      throw new OutputWriteException(
          "Cannot create output directory: " + outputDirectory, e, List.of());
    }

    List<Path> writtenFiles = new ArrayList<>();
    for (String format : formats) {
      try {
        OutputWriter writer = writerRegistry.writerFor(format);
        Path filePath = outputDirectory.resolve(baseName + "." + format);
        writer.write(result, filePath);
        writtenFiles.add(filePath);
      } catch (IllegalArgumentException e) {
        throw new OutputWriteException("Unsupported output format: " + format, writtenFiles);
      } catch (RuntimeException e) {
        throw new OutputWriteException("Cannot render output format: " + format, e, writtenFiles);
      }
    }
    return List.copyOf(writtenFiles);
  }
}
