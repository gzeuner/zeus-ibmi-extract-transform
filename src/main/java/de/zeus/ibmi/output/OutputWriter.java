package de.zeus.ibmi.output;

import de.zeus.ibmi.transform.QueryResult;

public interface OutputWriter {
    String formatName();

    String render(QueryResult result);
}
