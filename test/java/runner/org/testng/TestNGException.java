/*
 * Arbace's own source-compatible subset of TestNG's API (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a)), written
 * clean-room from TestNG's documentation for the check's framework runner (org.arbace.jtreg.Runner); not TestNG's code.
 */
package org.testng;

/** TestNG's own failures. */
public class TestNGException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public TestNGException(String message) { super(message); }
    public TestNGException(Throwable cause) { super(cause); }
    public TestNGException(String message, Throwable cause) { super(message, cause); }
}
