package de.zeus.ibmi.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ReadOnlyQueryGuardTest {

    private final ReadOnlyQueryGuard guard = new ReadOnlyQueryGuard();

    @Test
    void shouldAllowSimpleSelect() {
        String normalized = guard.validateOrNormalize("SELECT * FROM QSYS2.SYSTABLES");
        assertEquals("SELECT * FROM QSYS2.SYSTABLES", normalized);
    }

    @Test
    void shouldAllowWithQuery() {
        String normalized = guard.validateOrNormalize("WITH X AS (SELECT 1 AS V FROM SYSIBM.SYSDUMMY1) SELECT * FROM X");
        assertEquals("WITH X AS (SELECT 1 AS V FROM SYSIBM.SYSDUMMY1) SELECT * FROM X", normalized);
    }

    @Test
    void shouldAllowMixedCaseAndWhitespace() {
        String normalized = guard.validateOrNormalize("   SeLeCt 1 from SYSIBM.SYSDUMMY1   ");
        assertEquals("SeLeCt 1 from SYSIBM.SYSDUMMY1", normalized);
    }

    @Test
    void shouldAllowTrailingSemicolon() {
        String normalized = guard.validateOrNormalize("SELECT 1 FROM SYSIBM.SYSDUMMY1;");
        assertEquals("SELECT 1 FROM SYSIBM.SYSDUMMY1", normalized);
    }

    @Test
    void shouldAllowLeadingComments() {
        String normalized = guard.validateOrNormalize("/* comment */\n-- comment line\nSELECT 1 FROM SYSIBM.SYSDUMMY1;");
        assertEquals("SELECT 1 FROM SYSIBM.SYSDUMMY1", normalized);
    }

    @Test
    void shouldRejectInsert() {
        assertThrows(QueryGuardException.class, () -> guard.validateOrNormalize("INSERT INTO T VALUES (1)"));
    }

    @Test
    void shouldRejectUpdate() {
        assertThrows(QueryGuardException.class, () -> guard.validateOrNormalize("UPDATE T SET A = 1"));
    }

    @Test
    void shouldRejectDelete() {
        assertThrows(QueryGuardException.class, () -> guard.validateOrNormalize("DELETE FROM T"));
    }

    @Test
    void shouldRejectMerge() {
        assertThrows(QueryGuardException.class, () -> guard.validateOrNormalize("MERGE INTO T USING U ON 1=1 WHEN MATCHED THEN UPDATE SET A=1"));
    }

    @Test
    void shouldRejectDropAlterCreateTruncateCall() {
        assertThrows(QueryGuardException.class, () -> guard.validateOrNormalize("DROP TABLE T"));
        assertThrows(QueryGuardException.class, () -> guard.validateOrNormalize("ALTER TABLE T ADD COLUMN C INT"));
        assertThrows(QueryGuardException.class, () -> guard.validateOrNormalize("CREATE TABLE T (C INT)"));
        assertThrows(QueryGuardException.class, () -> guard.validateOrNormalize("TRUNCATE TABLE T"));
        assertThrows(QueryGuardException.class, () -> guard.validateOrNormalize("CALL QSYS2.QCMDEXC('DLTLIB LIB(X)')"));
    }

    @Test
    void shouldRejectMultiStatement() {
        assertThrows(QueryGuardException.class, () -> guard.validateOrNormalize("SELECT 1 FROM SYSIBM.SYSDUMMY1; SELECT 2 FROM SYSIBM.SYSDUMMY1"));
    }

    @Test
    void shouldRejectEmptyQuery() {
        assertThrows(QueryGuardException.class, () -> guard.validateOrNormalize(""));
        assertThrows(QueryGuardException.class, () -> guard.validateOrNormalize("   "));
        assertThrows(QueryGuardException.class, () -> guard.validateOrNormalize(null));
    }

    @Test
    void shouldIgnoreForbiddenKeywordsInsideStringLiteral() {
        String normalized = guard.validateOrNormalize("SELECT 'DROP TABLE X' AS TXT FROM SYSIBM.SYSDUMMY1");
        assertEquals("SELECT 'DROP TABLE X' AS TXT FROM SYSIBM.SYSDUMMY1", normalized);
    }

    @Test
    void shouldRejectDataChangingCte() {
        assertThrows(QueryGuardException.class, () ->
                guard.validateOrNormalize("WITH x AS (DELETE FROM T) SELECT * FROM x"));
    }

    @Test
    void shouldAllowCommentContainingForbiddenKeywords() {
        String normalized = guard.validateOrNormalize("SELECT 1 FROM SYSIBM.SYSDUMMY1 /* DROP TABLE T */");
        assertEquals("SELECT 1 FROM SYSIBM.SYSDUMMY1", normalized);
    }

    @Test
    void shouldRejectClCommandHints() {
        assertThrows(QueryGuardException.class, () -> guard.validateOrNormalize("SELECT QCMDEXC FROM SYSIBM.SYSDUMMY1"));
    }
}
