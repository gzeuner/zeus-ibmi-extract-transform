package de.zeus.ibmi.cli;

import de.zeus.ibmi.config.ConfigValidationException;
import de.zeus.ibmi.output.OutputWriteException;
import de.zeus.ibmi.runmanifest.ManifestException;
import de.zeus.ibmi.selection.QueryExecutionException;
import de.zeus.ibmi.selection.QueryGuardException;

public final class ExitCodeMapper {

    private ExitCodeMapper() {
    }

    public static ExitCode map(Throwable error) {
        if (error == null) {
            return ExitCode.SUCCESS;
        }
        if (contains(error, ConfigValidationException.class)) {
            return ExitCode.CONFIG_ERROR;
        }
        if (contains(error, QueryGuardException.class)) {
            return ExitCode.QUERY_GUARD_ERROR;
        }
        if (contains(error, QueryExecutionException.class)) {
            return ExitCode.JDBC_ERROR;
        }
        if (contains(error, OutputWriteException.class)) {
            return ExitCode.OUTPUT_ERROR;
        }
        if (contains(error, ManifestException.class)) {
            return ExitCode.MANIFEST_ERROR;
        }
        return ExitCode.GENERAL_ERROR;
    }

    public static ExitCode mapErrorClassName(String errorClassName) {
        if (errorClassName == null || errorClassName.isBlank()) {
            return ExitCode.GENERAL_ERROR;
        }
        if (errorClassName.equals(ConfigValidationException.class.getName())) {
            return ExitCode.CONFIG_ERROR;
        }
        if (errorClassName.equals(QueryGuardException.class.getName())) {
            return ExitCode.QUERY_GUARD_ERROR;
        }
        if (errorClassName.equals(QueryExecutionException.class.getName())) {
            return ExitCode.JDBC_ERROR;
        }
        if (errorClassName.equals(OutputWriteException.class.getName())) {
            return ExitCode.OUTPUT_ERROR;
        }
        if (errorClassName.equals(ManifestException.class.getName())) {
            return ExitCode.MANIFEST_ERROR;
        }
        return ExitCode.GENERAL_ERROR;
    }

    private static boolean contains(Throwable error, Class<? extends Throwable> type) {
        Throwable current = error;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
