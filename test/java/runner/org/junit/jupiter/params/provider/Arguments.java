/*
 * Arbace's own source-compatible subset of JUnit 5's API (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a)), written
 * clean-room from the JUnit 5 user guide and API documentation for the check's framework runner
 * (org.arbace.jtreg.Runner); not JUnit's code.
 */
package org.junit.jupiter.params.provider;

/** One invocation's arguments, the elements of a @MethodSource. */
public interface Arguments {
    Object[] get();

    static Arguments of(Object... arguments) { return () -> arguments; }
    static Arguments arguments(Object... arguments) { return of(arguments); }
}
