/*
 * Arbace's own source-compatible subset of TestNG's API (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a)), written
 * clean-room from TestNG's documentation for the check's framework runner (org.arbace.jtreg.Runner); not TestNG's code.
 */
package org.testng;

/** A test's failure that is not an assertion's (TestNG's). */
public class TestException extends TestNGException {
    private static final long serialVersionUID = 1L;
    public TestException(String message) { super(message); }
    public TestException(Throwable cause) { super(cause); }
    public TestException(String message, Throwable cause) { super(message, cause); }
}
