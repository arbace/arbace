/*
 * Arbace's own source-compatible subset of JUnit 5's API (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a)), written
 * clean-room from the JUnit 5 user guide and API documentation for the check's framework runner
 * (org.arbace.jtreg.Runner); not JUnit's code.
 */
package org.junit.jupiter.api.function;

/** A block that may throw. */
@FunctionalInterface
public interface Executable {
    void execute() throws Throwable;
}
