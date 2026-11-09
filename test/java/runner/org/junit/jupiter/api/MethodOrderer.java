/*
 * Arbace's own source-compatible subset of JUnit 5's API (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a)), written
 * clean-room from the JUnit 5 user guide and API documentation for the check's framework runner
 * (org.arbace.jtreg.Runner); not JUnit's code.
 */
package org.junit.jupiter.api;

/** The orders of a class's tests that @TestMethodOrder names (the check's runner knows these three). */
public interface MethodOrderer {
    /** By the methods' names. */
    class MethodName implements MethodOrderer { public MethodName() {} }
    /** By @Order's values. */
    class OrderAnnotation implements MethodOrderer { public OrderAnnotation() {} }
    /** By @DisplayName's names. */
    class DisplayName implements MethodOrderer { public DisplayName() {} }
}
