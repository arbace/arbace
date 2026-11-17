/*
 * Arbace's own source-compatible subset of TestNG's API (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a)), written
 * clean-room from TestNG's documentation for the check's framework runner (org.arbace.jtreg.Runner); not TestNG's code.
 */
package org.testng;

/** A test's retry decision (TestNG's): the check's runner runs a test once and asks none. */
public interface IRetryAnalyzer {
    boolean retry(ITestResult result);
}
