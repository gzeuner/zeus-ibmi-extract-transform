package de.zeus.ibmi.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SecretMaskerTest {

  @Test
  void shouldMaskKeyValuePairs() {
    String masked = SecretMasker.maskSensitive("password=secret123 token=abc apiKey=xyz");
    assertEquals("password=*** token=*** apiKey=***", masked);
  }

  @Test
  void shouldMaskJsonLikeSecrets() {
    String masked = SecretMasker.maskSensitive("{\"password\":\"secret123\",\"token\":\"abc\"}");
    assertEquals("{\"password\":\"***\",\"token\":\"***\"}", masked);
  }

  @Test
  void shouldMaskJdbcUserInfoCredentials() {
    String masked =
        SecretMasker.maskSensitive("jdbc:as400://alice:topsecret@ibmi.example.local/naming=sql");
    assertEquals("jdbc:as400://alice:***@ibmi.example.local/naming=sql", masked);
  }

  @Test
  void shouldReturnNullForNullInput() {
    assertNull(SecretMasker.maskSensitive(null));
  }

  @Test
  void shouldLeaveNonSecretTextUnchanged() {
    String source = "SELECT * FROM QSYS2.SYSTABLES";
    String masked = SecretMasker.maskSensitive(source);
    assertTrue(masked.contains("QSYS2.SYSTABLES"));
    assertEquals(source, masked);
  }
}
