package de.zeus.ibmi.infrastructure.output;

import de.zeus.ibmi.transform.QueryResult;
import java.nio.file.Path;

public sealed interface OutputWriter permits AbstractStringOutputWriter {

  String formatName();

  void write(QueryResult result, Path outputFile);
}
