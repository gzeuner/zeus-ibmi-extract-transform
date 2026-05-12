package de.zeus.ibmi.infrastructure.output;

import de.zeus.ibmi.transform.QueryResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract non-sealed class AbstractStringOutputWriter implements OutputWriter {

  @Override
  public final void write(QueryResult result, Path outputFile) {
    try {
      Files.writeString(outputFile, render(result), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to write output file: " + outputFile, ex);
    }
  }

  public abstract String render(QueryResult result);
}
