/*
 * Arbace's own source-compatible subset of JUnit 5's API (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a)), written
 * clean-room from the JUnit 5 user guide and API documentation for the check's framework runner
 * (org.arbace.jtreg.Runner); not JUnit's code.
 */
package org.junit.jupiter.api;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.TestAbortedException;

/** Assumptions: a false one aborts the test (TestAbortedException), which is no failure. */
public class Assumptions {
    protected Assumptions() {}
    static void aborted(String message) { throw new TestAbortedException("Assumption failed: " + (message == null ? "assumption is not true" : message)); }
    public static void assumeTrue(boolean assumption) { if (!assumption) aborted(null); }
    public static void assumeTrue(boolean assumption, String message) { if (!assumption) aborted(message); }
    public static void assumeTrue(boolean assumption, Supplier<String> message) { if (!assumption) aborted(message.get()); }
    public static void assumeTrue(BooleanSupplier assumption) { assumeTrue(assumption.getAsBoolean()); }
    public static void assumeTrue(BooleanSupplier assumption, String message) { assumeTrue(assumption.getAsBoolean(), message); }
    public static void assumeTrue(BooleanSupplier assumption, Supplier<String> message) { assumeTrue(assumption.getAsBoolean(), message); }
    public static void assumeFalse(boolean assumption) { if (assumption) aborted(null); }
    public static void assumeFalse(boolean assumption, String message) { if (assumption) aborted(message); }
    public static void assumeFalse(boolean assumption, Supplier<String> message) { if (assumption) aborted(message.get()); }
    public static void assumeFalse(BooleanSupplier assumption) { assumeFalse(assumption.getAsBoolean()); }
    public static void assumeFalse(BooleanSupplier assumption, String message) { assumeFalse(assumption.getAsBoolean(), message); }
    public static void assumingThat(boolean assumption, Executable executable) {
        if (assumption) {
            try { executable.execute(); } catch (Throwable t) { Assertions.rethrow(t); }
        }
    }
    public static void assumingThat(BooleanSupplier assumption, Executable executable) { assumingThat(assumption.getAsBoolean(), executable); }
    public static <V> V abort() { aborted(null); return null; }
    public static <V> V abort(String message) { throw new TestAbortedException(message); }
}
