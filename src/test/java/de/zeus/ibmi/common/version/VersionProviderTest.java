package de.zeus.ibmi.common.version;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VersionProviderTest {

  @Test
  void resolve_shouldPreferImplementationVersion() {
    VersionProvider provider = new VersionProvider(() -> "2.1.0", () -> "1.9.0", "0.0.0-dev");

    assertEquals("2.1.0", provider.resolve());
  }

  @Test
  void resolve_shouldUseBuildMetadataVersionWhenImplementationVersionMissing() {
    VersionProvider provider = new VersionProvider(() -> " ", () -> "2.2.0", "0.0.0-dev");

    assertEquals("2.2.0", provider.resolve());
  }

  @Test
  void resolve_shouldFallbackWhenNoBuildMetadataAvailable() {
    VersionProvider provider = new VersionProvider(() -> null, () -> null, "0.0.0-dev");

    assertEquals("0.0.0-dev", provider.resolve());
  }
}
