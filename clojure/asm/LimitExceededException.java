package clojure.asm;

/**
 * Exception thrown when an ASM specific limit is exceeded. This includes limits set by {@link
 * ClassWriter#setComputeLimits}, too many nested annotations or constant dynamic, too many nested
 * type arguments in signatures, etc.
 */
public final class LimitExceededException extends RuntimeException {

  private static final long serialVersionUID = -1007650817078992929L;

  /**
   * Constructs a new {@link LimitExceededException}.
   *
   * @param message details about the exceeded limit.
   */
  public LimitExceededException(final String message) {
    super(message);
  }
}
