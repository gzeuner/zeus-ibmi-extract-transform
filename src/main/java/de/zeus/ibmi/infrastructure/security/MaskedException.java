package de.zeus.ibmi.infrastructure.security;

public final class MaskedException extends RuntimeException {

  public MaskedException(String message, Throwable cause) {
    super(SecurityUtils.maskSecrets(message), cause);
  }

  public static MaskedException from(Throwable cause) {
    String message = cause == null ? "Unexpected error" : cause.getMessage();
    return new MaskedException(message, cause);
  }
}
