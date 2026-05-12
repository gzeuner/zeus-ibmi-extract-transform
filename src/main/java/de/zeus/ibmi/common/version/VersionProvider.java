package de.zeus.ibmi.common.version;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

public final class VersionProvider {

  private static final String POM_PROPERTIES_PATH =
      "/META-INF/maven/de.zeus.ibmi/zeus-ibmi-extract-transform/pom.properties";

  public static final String DEVELOPMENT_FALLBACK_VERSION = "0.2.0";

  private final Supplier<String> implementationVersionSupplier;
  private final Supplier<String> buildMetadataVersionSupplier;
  private final String fallbackVersion;

  public VersionProvider() {
    this(
        () ->
            readNormalized(
                VersionProvider.class.getPackage() == null
                    ? null
                    : VersionProvider.class.getPackage().getImplementationVersion()),
        VersionProvider::readVersionFromPomProperties,
        DEVELOPMENT_FALLBACK_VERSION);
  }

  public VersionProvider(
      Supplier<String> implementationVersionSupplier,
      Supplier<String> buildMetadataVersionSupplier,
      String fallbackVersion) {
    this.implementationVersionSupplier = Objects.requireNonNull(implementationVersionSupplier);
    this.buildMetadataVersionSupplier = Objects.requireNonNull(buildMetadataVersionSupplier);
    this.fallbackVersion =
        readNormalized(fallbackVersion) == null
            ? DEVELOPMENT_FALLBACK_VERSION
            : readNormalized(fallbackVersion);
  }

  public String resolve() {
    String fromImplementation = readNormalized(implementationVersionSupplier.get());
    if (fromImplementation != null) {
      return fromImplementation;
    }
    String fromBuildMetadata = readNormalized(buildMetadataVersionSupplier.get());
    if (fromBuildMetadata != null) {
      return fromBuildMetadata;
    }
    return fallbackVersion;
  }

  private static String readVersionFromPomProperties() {
    try (InputStream in = VersionProvider.class.getResourceAsStream(POM_PROPERTIES_PATH)) {
      if (in == null) {
        return null;
      }
      Properties properties = new Properties();
      properties.load(in);
      return readNormalized(properties.getProperty("version"));
    } catch (IOException ex) {
      return null;
    }
  }

  private static String readNormalized(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
