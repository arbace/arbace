/*
 * Arbace's own source-compatible subset of JUnit 4's API (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a)), which jtreg's
 * @run junit runs too, written clean-room from JUnit 4's API documentation for the check's framework runner
 * (org.arbace.jtreg.Runner); not JUnit's code.
 */
package org.junit.function;

/** A block that may throw, for Assert.assertThrows. */
public interface ThrowingRunnable {
    void run() throws Throwable;
}
