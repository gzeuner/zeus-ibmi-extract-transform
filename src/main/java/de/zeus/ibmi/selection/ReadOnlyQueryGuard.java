package de.zeus.ibmi.selection;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReadOnlyQueryGuard {

  private static final Pattern FIRST_TOKEN_PATTERN = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)");
  private static final Pattern FORBIDDEN_SQL_KEYWORDS =
      Pattern.compile(
          "\\b(INSERT|UPDATE|DELETE|MERGE|DROP|ALTER|CREATE|TRUNCATE|CALL)\\b",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern FORBIDDEN_COMMAND_HINTS =
      Pattern.compile(
          "\\b(CHGCURLIB|CHGLIBL|DLTLIB|CRTLIB|SBMJOB|RUNSQL|QCMDEXC)\\b",
          Pattern.CASE_INSENSITIVE);

  public String validateOrNormalize(String query) {
    if (query == null || query.trim().isEmpty()) {
      throw new QueryGuardException("Query must not be empty.");
    }

    String withoutComments = stripComments(query);
    String trimmed = withoutComments.trim();
    if (trimmed.isEmpty()) {
      throw new QueryGuardException("Query must not be empty after removing comments.");
    }

    if (containsMultipleStatements(trimmed)) {
      throw new QueryGuardException("Multiple SQL statements are not allowed.");
    }

    String normalized = stripTrailingSemicolon(trimmed).trim();
    String firstToken = firstToken(normalized);
    if (!"SELECT".equals(firstToken) && !"WITH".equals(firstToken)) {
      throw new QueryGuardException("Only SELECT and WITH statements are allowed.");
    }

    String tokenSafe = stripStringLiterals(normalized);
    Matcher forbiddenSql = FORBIDDEN_SQL_KEYWORDS.matcher(tokenSafe);
    if (forbiddenSql.find()) {
      throw new QueryGuardException(
          "Forbidden SQL keyword detected: " + forbiddenSql.group(1).toUpperCase(Locale.ROOT));
    }
    Matcher forbiddenCommands = FORBIDDEN_COMMAND_HINTS.matcher(tokenSafe);
    if (forbiddenCommands.find()) {
      throw new QueryGuardException(
          "Potential command execution pattern detected: "
              + forbiddenCommands.group(1).toUpperCase(Locale.ROOT));
    }

    return normalized;
  }

  static String stripComments(String input) {
    StringBuilder out = new StringBuilder(input.length());
    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;
    boolean inLineComment = false;
    boolean inBlockComment = false;

    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      char next = (i + 1) < input.length() ? input.charAt(i + 1) : '\0';

      if (inLineComment) {
        if (c == '\n') {
          inLineComment = false;
          out.append(c);
        }
        continue;
      }
      if (inBlockComment) {
        if (c == '*' && next == '/') {
          inBlockComment = false;
          i++;
        }
        continue;
      }

      if (!inSingleQuote && !inDoubleQuote) {
        if (c == '-' && next == '-') {
          inLineComment = true;
          i++;
          continue;
        }
        if (c == '/' && next == '*') {
          inBlockComment = true;
          i++;
          continue;
        }
      }

      if (c == '\'' && !inDoubleQuote) {
        if (inSingleQuote && next == '\'') {
          out.append(c).append(next);
          i++;
          continue;
        }
        inSingleQuote = !inSingleQuote;
      } else if (c == '"' && !inSingleQuote) {
        inDoubleQuote = !inDoubleQuote;
      }

      out.append(c);
    }
    return out.toString();
  }

  static String stripStringLiterals(String input) {
    StringBuilder out = new StringBuilder(input.length());
    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;

    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      char next = (i + 1) < input.length() ? input.charAt(i + 1) : '\0';

      if (c == '\'' && !inDoubleQuote) {
        if (inSingleQuote && next == '\'') {
          out.append(' ').append(' ');
          i++;
          continue;
        }
        inSingleQuote = !inSingleQuote;
        out.append(' ');
        continue;
      }

      if (c == '"' && !inSingleQuote) {
        inDoubleQuote = !inDoubleQuote;
        out.append(' ');
        continue;
      }

      if (inSingleQuote || inDoubleQuote) {
        out.append(' ');
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }

  static boolean containsMultipleStatements(String sql) {
    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;
    int semicolons = 0;
    int lastSemicolonIndex = -1;

    for (int i = 0; i < sql.length(); i++) {
      char c = sql.charAt(i);
      char next = (i + 1) < sql.length() ? sql.charAt(i + 1) : '\0';

      if (c == '\'' && !inDoubleQuote) {
        if (inSingleQuote && next == '\'') {
          i++;
          continue;
        }
        inSingleQuote = !inSingleQuote;
      } else if (c == '"' && !inSingleQuote) {
        inDoubleQuote = !inDoubleQuote;
      }

      if (!inSingleQuote && !inDoubleQuote && c == ';') {
        semicolons++;
        lastSemicolonIndex = i;
      }
    }

    if (semicolons == 0) {
      return false;
    }
    if (semicolons > 1) {
      return true;
    }
    for (int i = lastSemicolonIndex + 1; i < sql.length(); i++) {
      if (!Character.isWhitespace(sql.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  private static String stripTrailingSemicolon(String sql) {
    int end = sql.length();
    while (end > 0 && Character.isWhitespace(sql.charAt(end - 1))) {
      end--;
    }
    if (end > 0 && sql.charAt(end - 1) == ';') {
      end--;
    }
    return sql.substring(0, end);
  }

  private static String firstToken(String sql) {
    Matcher matcher = FIRST_TOKEN_PATTERN.matcher(sql);
    if (!matcher.find()) {
      throw new QueryGuardException("Unable to detect a valid SQL starting token.");
    }
    return matcher.group(1).toUpperCase(Locale.ROOT);
  }
}
