/*
 * Arbace's own source-compatible subset of TestNG's API (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a)), written
 * clean-room from TestNG's documentation for the check's framework runner (org.arbace.jtreg.Runner); not TestNG's code.
 */
package org.testng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * TestNG's assertions: the actual value first, then the expected one, then the message; a failure is an
 * AssertionError whose message says "expected [E] but found [A]".
 */
public class Assert {
    protected Assert() {}

    /** A block whose throw assertThrows and expectThrows check. */
    public interface ThrowingRunnable {
        void run() throws Throwable;
    }

    static String format(Object actual, Object expected, String message) {
        return (message == null ? "" : message + " ") + "expected [" + expected + "] but found [" + actual + "]";
    }

    static void failNotEquals(Object actual, Object expected, String message) {
        fail(format(actual, expected, message));
    }

    public static void fail(String message, Throwable realCause) {
        AssertionError e = new AssertionError(message);
        e.initCause(realCause);
        throw e;
    }
    public static void fail(String message) { throw new AssertionError(message); }
    public static void fail() { fail(null); }

    public static void assertTrue(boolean condition, String message) { if (!condition) failNotEquals(condition, Boolean.TRUE, message); }
    public static void assertTrue(boolean condition) { assertTrue(condition, null); }
    public static void assertFalse(boolean condition, String message) { if (condition) failNotEquals(condition, Boolean.FALSE, message); }
    public static void assertFalse(boolean condition) { assertFalse(condition, null); }

    public static void assertNull(Object object, String message) { if (object != null) failNotEquals(object, null, message); }
    public static void assertNull(Object object) { assertNull(object, null); }
    public static void assertNotNull(Object object, String message) {
        if (object == null) fail((message == null ? "" : message + " ") + "expected object to not be null");
    }
    public static void assertNotNull(Object object) { assertNotNull(object, null); }

    public static void assertSame(Object actual, Object expected, String message) {
        if (actual != expected) fail((message == null ? "" : message + " ") + "expected [" + expected + "] but found [" + actual + "] (not the same)");
    }
    public static void assertSame(Object actual, Object expected) { assertSame(actual, expected, null); }
    public static void assertNotSame(Object actual, Object expected, String message) {
        if (actual == expected) fail((message == null ? "" : message + " ") + "expected not same [" + expected + "] but found [" + actual + "]");
    }
    public static void assertNotSame(Object actual, Object expected) { assertNotSame(actual, expected, null); }

    // equality: arrays by their elements (nested ones too), the rest by equals
    static boolean equal(Object actual, Object expected) {
        if (actual == expected) return true;
        if (actual == null || expected == null) return false;
        if (actual.getClass().isArray() && expected.getClass().isArray())
            return Arrays.deepEquals(new Object[] {actual}, new Object[] {expected});
        return expected.equals(actual);
    }

    static String show(Object o) {
        if (o == null || !o.getClass().isArray()) return String.valueOf(o);
        String s = Arrays.deepToString(new Object[] {o});
        return s.substring(1, s.length() - 1);
    }

    public static void assertEquals(Object actual, Object expected, String message) {
        if (!equal(actual, expected)) failNotEquals(show(actual), show(expected), message);
    }
    public static void assertEquals(Object actual, Object expected) { assertEquals(actual, expected, null); }
    public static void assertEquals(String actual, String expected, String message) { assertEquals((Object) actual, (Object) expected, message); }
    public static void assertEquals(String actual, String expected) { assertEquals(actual, expected, null); }

    public static void assertEquals(double actual, double expected, double delta, String message) {
        if (Double.compare(actual, expected) != 0 && !(Math.abs(expected - actual) <= delta)) failNotEquals(actual, expected, message);
    }
    public static void assertEquals(double actual, double expected, double delta) { assertEquals(actual, expected, delta, null); }
    public static void assertEquals(double actual, double expected, String message) { assertEquals(actual, expected, 0.0d, message); }
    public static void assertEquals(double actual, double expected) { assertEquals(actual, expected, 0.0d, null); }
    public static void assertEquals(float actual, float expected, float delta, String message) {
        if (Float.compare(actual, expected) != 0 && !(Math.abs(expected - actual) <= delta)) failNotEquals(actual, expected, message);
    }
    public static void assertEquals(float actual, float expected, float delta) { assertEquals(actual, expected, delta, null); }
    public static void assertEquals(float actual, float expected, String message) { assertEquals(actual, expected, 0.0f, message); }
    public static void assertEquals(float actual, float expected) { assertEquals(actual, expected, 0.0f, null); }
    public static void assertEquals(long actual, long expected, String message) { if (actual != expected) failNotEquals(actual, expected, message); }
    public static void assertEquals(long actual, long expected) { assertEquals(actual, expected, null); }
    public static void assertEquals(boolean actual, boolean expected, String message) { if (actual != expected) failNotEquals(actual, expected, message); }
    public static void assertEquals(boolean actual, boolean expected) { assertEquals(actual, expected, null); }
    public static void assertEquals(byte actual, byte expected, String message) { if (actual != expected) failNotEquals(actual, expected, message); }
    public static void assertEquals(byte actual, byte expected) { assertEquals(actual, expected, null); }
    public static void assertEquals(char actual, char expected, String message) { if (actual != expected) failNotEquals(actual, expected, message); }
    public static void assertEquals(char actual, char expected) { assertEquals(actual, expected, null); }
    public static void assertEquals(short actual, short expected, String message) { if (actual != expected) failNotEquals(actual, expected, message); }
    public static void assertEquals(short actual, short expected) { assertEquals(actual, expected, null); }
    public static void assertEquals(int actual, int expected, String message) { if (actual != expected) failNotEquals(actual, expected, message); }
    public static void assertEquals(int actual, int expected) { assertEquals(actual, expected, null); }
    public static void assertEquals(boolean[] actual, boolean[] expected, String message) {
        if (!Arrays.equals(actual, expected)) failNotEquals(Arrays.toString(actual), Arrays.toString(expected), message);
    }
    public static void assertEquals(boolean[] actual, boolean[] expected) { assertEquals(actual, expected, null); }
    public static void assertEquals(byte[] actual, byte[] expected, String message) {
        if (!Arrays.equals(actual, expected)) failNotEquals(Arrays.toString(actual), Arrays.toString(expected), message);
    }
    public static void assertEquals(byte[] actual, byte[] expected) { assertEquals(actual, expected, null); }
    public static void assertEquals(char[] actual, char[] expected, String message) {
        if (!Arrays.equals(actual, expected)) failNotEquals(Arrays.toString(actual), Arrays.toString(expected), message);
    }
    public static void assertEquals(char[] actual, char[] expected) { assertEquals(actual, expected, null); }
    public static void assertEquals(short[] actual, short[] expected, String message) {
        if (!Arrays.equals(actual, expected)) failNotEquals(Arrays.toString(actual), Arrays.toString(expected), message);
    }
    public static void assertEquals(short[] actual, short[] expected) { assertEquals(actual, expected, null); }
    public static void assertEquals(int[] actual, int[] expected, String message) {
        if (!Arrays.equals(actual, expected)) failNotEquals(Arrays.toString(actual), Arrays.toString(expected), message);
    }
    public static void assertEquals(int[] actual, int[] expected) { assertEquals(actual, expected, null); }
    public static void assertEquals(long[] actual, long[] expected, String message) {
        if (!Arrays.equals(actual, expected)) failNotEquals(Arrays.toString(actual), Arrays.toString(expected), message);
    }
    public static void assertEquals(long[] actual, long[] expected) { assertEquals(actual, expected, null); }
    public static void assertEquals(float[] actual, float[] expected, String message) {
        if (!Arrays.equals(actual, expected)) failNotEquals(Arrays.toString(actual), Arrays.toString(expected), message);
    }
    public static void assertEquals(float[] actual, float[] expected) { assertEquals(actual, expected, null); }
    public static void assertEquals(float[] actual, float[] expected, float delta, String message) {
        if (actual == expected) return;
        if (actual == null || expected == null || actual.length != expected.length) { failNotEquals(Arrays.toString(actual), Arrays.toString(expected), message); return; }
        for (int i = 0; i < actual.length; i++) assertEquals(actual[i], expected[i], delta, (message == null ? "" : message + ": ") + "at index " + i);
    }
    public static void assertEquals(float[] actual, float[] expected, float delta) { assertEquals(actual, expected, delta, null); }
    public static void assertEquals(double[] actual, double[] expected, String message) {
        if (!Arrays.equals(actual, expected)) failNotEquals(Arrays.toString(actual), Arrays.toString(expected), message);
    }
    public static void assertEquals(double[] actual, double[] expected) { assertEquals(actual, expected, null); }
    public static void assertEquals(double[] actual, double[] expected, double delta, String message) {
        if (actual == expected) return;
        if (actual == null || expected == null || actual.length != expected.length) { failNotEquals(Arrays.toString(actual), Arrays.toString(expected), message); return; }
        for (int i = 0; i < actual.length; i++) assertEquals(actual[i], expected[i], delta, (message == null ? "" : message + ": ") + "at index " + i);
    }
    public static void assertEquals(double[] actual, double[] expected, double delta) { assertEquals(actual, expected, delta, null); }
    public static void assertEquals(Object[] actual, Object[] expected, String message) {
        if (!Arrays.deepEquals(actual, expected)) failNotEquals(Arrays.deepToString(actual), Arrays.deepToString(expected), message);
    }
    public static void assertEquals(Object[] actual, Object[] expected) { assertEquals(actual, expected, null); }

    public static void assertEquals(Collection<?> actual, Collection<?> expected, String message) {
        if (actual == expected) return;
        if (actual == null || expected == null) { failNotEquals(actual, expected, message); return; }
        assertEquals(actual.size(), expected.size(), (message == null ? "" : message + ": ") + "lists don't have the same size");
        Iterator<?> a = actual.iterator(), e = expected.iterator();
        for (int i = 0; a.hasNext() && e.hasNext(); i++) {
            Object x = a.next(), y = e.next();
            if (!equal(x, y)) failNotEquals(show(x), show(y), (message == null ? "" : message + ": ") + "Lists differ at element [" + i + "]:");
        }
    }
    public static void assertEquals(Collection<?> actual, Collection<?> expected) { assertEquals(actual, expected, null); }

    public static void assertEquals(Iterator<?> actual, Iterator<?> expected, String message) {
        if (actual == expected) return;
        if (actual == null || expected == null) { failNotEquals(actual, expected, message); return; }
        for (int i = 0; actual.hasNext() || expected.hasNext(); i++) {
            if (!actual.hasNext() || !expected.hasNext()) fail((message == null ? "" : message + ": ") + "iterators don't have the same size at element [" + i + "]");
            Object x = actual.next(), y = expected.next();
            if (!equal(x, y)) failNotEquals(show(x), show(y), (message == null ? "" : message + ": ") + "Iterators differ at element [" + i + "]:");
        }
    }
    public static void assertEquals(Iterator<?> actual, Iterator<?> expected) { assertEquals(actual, expected, null); }

    public static void assertEquals(Iterable<?> actual, Iterable<?> expected, String message) {
        if (actual == expected) return;
        if (actual == null || expected == null) { failNotEquals(actual, expected, message); return; }
        assertEquals(actual.iterator(), expected.iterator(), message);
    }
    public static void assertEquals(Iterable<?> actual, Iterable<?> expected) { assertEquals(actual, expected, null); }

    public static void assertEquals(Set<?> actual, Set<?> expected, String message) {
        if (!Objects.equals(actual, expected)) failNotEquals(actual, expected, (message == null ? "" : message + ": ") + "Sets differ:");
    }
    public static void assertEquals(Set<?> actual, Set<?> expected) { assertEquals(actual, expected, null); }

    public static void assertEquals(Map<?, ?> actual, Map<?, ?> expected, String message) {
        if (!Objects.equals(actual, expected)) failNotEquals(actual, expected, (message == null ? "" : message + ": ") + "Maps differ:");
    }
    public static void assertEquals(Map<?, ?> actual, Map<?, ?> expected) { assertEquals(actual, expected, null); }

    public static void assertEqualsDeep(Map<?, ?> actual, Map<?, ?> expected, String message) {
        if (actual == expected) return;
        if (actual == null || expected == null || actual.size() != expected.size()) { failNotEquals(actual, expected, message); return; }
        for (Map.Entry<?, ?> e : expected.entrySet())
            if (!actual.containsKey(e.getKey()) || !equal(actual.get(e.getKey()), e.getValue())) failNotEquals(actual, expected, message);
    }
    public static void assertEqualsDeep(Map<?, ?> actual, Map<?, ?> expected) { assertEqualsDeep(actual, expected, null); }

    // the same elements, whatever their order
    public static void assertEqualsNoOrder(Object[] actual, Object[] expected, String message) {
        if (actual == expected) return;
        if (actual == null || expected == null || actual.length != expected.length) { failNotEquals(Arrays.toString(actual), Arrays.toString(expected), message); return; }
        List<Object> left = new ArrayList<>(Arrays.asList(actual));
        for (Object e : expected) {
            boolean found = false;
            for (Iterator<Object> i = left.iterator(); i.hasNext();) if (equal(i.next(), e)) { i.remove(); found = true; break; }
            if (!found) failNotEquals(Arrays.toString(actual), Arrays.toString(expected), (message == null ? "" : message + " ") + "(any order)");
        }
    }
    public static void assertEqualsNoOrder(Object[] actual, Object[] expected) { assertEqualsNoOrder(actual, expected, null); }
    public static void assertEqualsNoOrder(Collection<?> actual, Collection<?> expected, String message) {
        assertEqualsNoOrder(actual == null ? null : actual.toArray(), expected == null ? null : expected.toArray(), message);
    }
    public static void assertEqualsNoOrder(Collection<?> actual, Collection<?> expected) { assertEqualsNoOrder(actual, expected, null); }

    public static void assertNotEquals(Object actual, Object expected, String message) {
        if (equal(actual, expected)) fail((message == null ? "" : message + " ") + "did not expect [" + show(actual) + "] but found [" + show(expected) + "]");
    }
    public static void assertNotEquals(Object actual, Object expected) { assertNotEquals(actual, expected, null); }
    public static void assertNotEquals(float actual, float expected, float delta, String message) {
        if (Float.compare(actual, expected) == 0 || Math.abs(expected - actual) <= delta) fail((message == null ? "" : message + " ") + "did not expect [" + actual + "] but found [" + expected + "]");
    }
    public static void assertNotEquals(float actual, float expected, float delta) { assertNotEquals(actual, expected, delta, null); }
    public static void assertNotEquals(double actual, double expected, double delta, String message) {
        if (Double.compare(actual, expected) == 0 || Math.abs(expected - actual) <= delta) fail((message == null ? "" : message + " ") + "did not expect [" + actual + "] but found [" + expected + "]");
    }
    public static void assertNotEquals(double actual, double expected, double delta) { assertNotEquals(actual, expected, delta, null); }
    public static void assertNotEquals(Set<?> actual, Set<?> expected, String message) { assertNotEquals((Object) actual, (Object) expected, message); }
    public static void assertNotEquals(Set<?> actual, Set<?> expected) { assertNotEquals(actual, expected, null); }
    public static void assertNotEquals(Map<?, ?> actual, Map<?, ?> expected, String message) { assertNotEquals((Object) actual, (Object) expected, message); }
    public static void assertNotEquals(Map<?, ?> actual, Map<?, ?> expected) { assertNotEquals(actual, expected, null); }

    // the block's throw: of the class (or a subclass), else a failure
    public static void assertThrows(ThrowingRunnable runnable) { assertThrows(Throwable.class, runnable); }
    public static <T extends Throwable> void assertThrows(Class<T> throwableClass, ThrowingRunnable runnable) {
        expectThrows(throwableClass, runnable);
    }
    public static <T extends Throwable> void assertThrows(String message, Class<T> throwableClass, ThrowingRunnable runnable) {
        expectThrows(message, throwableClass, runnable);
    }
    public static <T extends Throwable> T expectThrows(Class<T> throwableClass, ThrowingRunnable runnable) {
        return expectThrows(null, throwableClass, runnable);
    }
    public static <T extends Throwable> T expectThrows(String message, Class<T> throwableClass, ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            if (throwableClass.isInstance(t)) return throwableClass.cast(t);
            fail((message == null ? "" : message + " ") + "Expected " + throwableClass.getSimpleName() + " to be thrown, but "
                 + t.getClass().getSimpleName() + " was thrown", t);
        }
        fail((message == null ? "" : message + " ") + "Expected " + throwableClass.getSimpleName() + " to be thrown, but nothing was thrown");
        return null;
    }
}
