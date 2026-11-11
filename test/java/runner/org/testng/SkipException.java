/*
 * Arbace's own source-compatible subset of TestNG's API (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a)), written
 * clean-room from TestNG's documentation for the check's framework runner (org.arbace.jtreg.Runner); not TestNG's code.
 */
package org.testng;

/** Thrown by a test or a configuration method to skip it (TestNG's). */
public class SkipException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public SkipException(String message) { super(message); }
    public SkipException(String message, Throwable cause) { super(message, cause); }
    public boolean isSkip() { return true; }
}
