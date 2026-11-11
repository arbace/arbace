/*
 * Arbace's own source-compatible subset of JUnit 4's API (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a)), which jtreg's
 * @run junit runs too, written clean-room from JUnit 4's API documentation for the check's framework runner
 * (org.arbace.jtreg.Runner); not JUnit's code.
 */
package org.junit;

import java.util.Arrays;
import java.util.Objects;
import org.junit.function.ThrowingRunnable;

/** JUnit 4's assertions: the message first when there is one, then the expected value, then the actual one. */
public class Assert {
    protected Assert() {}

    static String format(String message, Object expected, Object actual) {
        return (message == null || message.isEmpty() ? "" : message + " ") + "expected:<" + expected + "> but was:<" + actual + ">";
    }

    public static void fail(String message) { throw new AssertionError(message == null ? "" : message); }
    public static void fail() { fail(null); }
    public static void assertTrue(String message, boolean condition) { if (!condition) fail(message); }
    public static void assertTrue(boolean condition) { assertTrue(null, condition); }
    public static void assertFalse(String message, boolean condition) { assertTrue(message, !condition); }
    public static void assertFalse(boolean condition) { assertFalse(null, condition); }
    public static void assertNull(String message, Object object) { if (object != null) fail(format(message, null, object)); }
    public static void assertNull(Object object) { assertNull(null, object); }
    public static void assertNotNull(String message, Object object) { if (object == null) fail(message); }
    public static void assertNotNull(Object object) { assertNotNull(null, object); }
    public static void assertSame(String message, Object expected, Object actual) { if (expected != actual) fail(format(message, expected, actual)); }
    public static void assertSame(Object expected, Object actual) { assertSame(null, expected, actual); }
    public static void assertNotSame(String message, Object unexpected, Object actual) { if (unexpected == actual) fail((message == null ? "" : message + " ") + "expected not same"); }
    public static void assertNotSame(Object unexpected, Object actual) { assertNotSame(null, unexpected, actual); }
    public static void assertEquals(String message, Object expected, Object actual) {
        if (!Objects.deepEquals(expected, actual)) fail(format(message, expected, actual));
    }
    public static void assertEquals(Object expected, Object actual) { assertEquals(null, expected, actual); }
    public static void assertNotEquals(String message, Object unexpected, Object actual) {
        if (Objects.equals(unexpected, actual)) fail((message == null ? "" : message + ". ") + "Values should be different. Actual: " + actual);
    }
    public static void assertNotEquals(Object unexpected, Object actual) { assertNotEquals(null, unexpected, actual); }
    public static void assertEquals(String message, long expected, long actual) { if (expected != actual) fail(format(message, expected, actual)); }
    public static void assertEquals(long expected, long actual) { assertEquals(null, expected, actual); }
    public static void assertNotEquals(String message, long unexpected, long actual) { if (unexpected == actual) fail((message == null ? "" : message + ". ") + "Values should be different. Actual: " + actual); }
    public static void assertNotEquals(long unexpected, long actual) { assertNotEquals(null, unexpected, actual); }
    public static void assertEquals(String message, double expected, double actual, double delta) {
        if (Double.compare(expected, actual) != 0 && !(Math.abs(expected - actual) <= delta)) fail(format(message, expected, actual));
    }
    public static void assertEquals(double expected, double actual, double delta) { assertEquals(null, expected, actual, delta); }
    public static void assertEquals(String message, float expected, float actual, float delta) {
        if (Float.compare(expected, actual) != 0 && !(Math.abs(expected - actual) <= delta)) fail(format(message, expected, actual));
    }
    public static void assertEquals(float expected, float actual, float delta) { assertEquals(null, expected, actual, delta); }
    public static void assertArrayEquals(String message, Object[] expecteds, Object[] actuals) {
        if (!Arrays.deepEquals(expecteds, actuals)) fail(format(message, Arrays.deepToString(expecteds), Arrays.deepToString(actuals)));
    }
    public static void assertArrayEquals(Object[] expecteds, Object[] actuals) { assertArrayEquals(null, expecteds, actuals); }
    public static void assertArrayEquals(String message, boolean[] expecteds, boolean[] actuals) {
        if (!Arrays.equals(expecteds, actuals)) fail(format(message, Arrays.toString(expecteds), Arrays.toString(actuals)));
    }
    public static void assertArrayEquals(boolean[] expecteds, boolean[] actuals) { assertArrayEquals(null, expecteds, actuals); }
    public static void assertArrayEquals(String message, byte[] expecteds, byte[] actuals) {
        if (!Arrays.equals(expecteds, actuals)) fail(format(message, Arrays.toString(expecteds), Arrays.toString(actuals)));
    }
    public static void assertArrayEquals(byte[] expecteds, byte[] actuals) { assertArrayEquals(null, expecteds, actuals); }
    public static void assertArrayEquals(String message, char[] expecteds, char[] actuals) {
        if (!Arrays.equals(expecteds, actuals)) fail(format(message, Arrays.toString(expecteds), Arrays.toString(actuals)));
    }
    public static void assertArrayEquals(char[] expecteds, char[] actuals) { assertArrayEquals(null, expecteds, actuals); }
    public static void assertArrayEquals(String message, short[] expecteds, short[] actuals) {
        if (!Arrays.equals(expecteds, actuals)) fail(format(message, Arrays.toString(expecteds), Arrays.toString(actuals)));
    }
    public static void assertArrayEquals(short[] expecteds, short[] actuals) { assertArrayEquals(null, expecteds, actuals); }
    public static void assertArrayEquals(String message, int[] expecteds, int[] actuals) {
        if (!Arrays.equals(expecteds, actuals)) fail(format(message, Arrays.toString(expecteds), Arrays.toString(actuals)));
    }
    public static void assertArrayEquals(int[] expecteds, int[] actuals) { assertArrayEquals(null, expecteds, actuals); }
    public static void assertArrayEquals(String message, long[] expecteds, long[] actuals) {
        if (!Arrays.equals(expecteds, actuals)) fail(format(message, Arrays.toString(expecteds), Arrays.toString(actuals)));
    }
    public static void assertArrayEquals(long[] expecteds, long[] actuals) { assertArrayEquals(null, expecteds, actuals); }
    public static void assertArrayEquals(String message, float[] expecteds, float[] actuals, float delta) {
        if (expecteds == actuals) return;
        if (expecteds == null || actuals == null || expecteds.length != actuals.length) { fail(format(message, Arrays.toString(expecteds), Arrays.toString(actuals))); return; }
        for (int i = 0; i < expecteds.length; i++) assertEquals(message, expecteds[i], actuals[i], delta);
    }
    public static void assertArrayEquals(float[] expecteds, float[] actuals, float delta) { assertArrayEquals(null, expecteds, actuals, delta); }
    public static void assertArrayEquals(String message, double[] expecteds, double[] actuals, double delta) {
        if (expecteds == actuals) return;
        if (expecteds == null || actuals == null || expecteds.length != actuals.length) { fail(format(message, Arrays.toString(expecteds), Arrays.toString(actuals))); return; }
        for (int i = 0; i < expecteds.length; i++) assertEquals(message, expecteds[i], actuals[i], delta);
    }
    public static void assertArrayEquals(double[] expecteds, double[] actuals, double delta) { assertArrayEquals(null, expecteds, actuals, delta); }
    public static <T extends Throwable> T assertThrows(Class<T> expectedThrowable, ThrowingRunnable runnable) {
        return assertThrows(null, expectedThrowable, runnable);
    }
    public static <T extends Throwable> T assertThrows(String message, Class<T> expectedThrowable, ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            if (expectedThrowable.isInstance(t)) return expectedThrowable.cast(t);
            AssertionError e = new AssertionError((message == null ? "" : message + ": ") + "unexpected exception type thrown; expected:<"
                                                  + expectedThrowable.getSimpleName() + "> but was:<" + t.getClass().getSimpleName() + ">");
            e.initCause(t);
            throw e;
        }
        throw new AssertionError((message == null ? "" : message + ": ") + "expected " + expectedThrowable.getSimpleName() + " to be thrown, but nothing was thrown");
    }
}
