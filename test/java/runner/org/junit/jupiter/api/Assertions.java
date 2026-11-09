/*
 * Arbace's own source-compatible subset of JUnit 5's API (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a)), written
 * clean-room from the JUnit 5 user guide and API documentation for the check's framework runner
 * (org.arbace.jtreg.Runner); not JUnit's code.
 */
package org.junit.jupiter.api;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.opentest4j.AssertionFailedError;

/**
 * JUnit 5's assertions: the expected value first, then the actual one, then the message (a String or a
 * Supplier<String>); a failure is an AssertionFailedError whose message says "expected: <E> but was: <A>".
 */
public class Assertions {
    protected Assertions() {}

    static String msg(Object m) {
        Object s = m instanceof Supplier ? ((Supplier<?>) m).get() : m;
        return s == null || String.valueOf(s).isEmpty() ? "" : s + " ==> ";
    }
    static void failEquals(Object expected, Object actual, Object m) {
        throw new AssertionFailedError(msg(m) + "expected: <" + expected + "> but was: <" + actual + ">", expected, actual);
    }
    static void failNotEquals(Object unexpected, Object m) {
        throw new AssertionFailedError(msg(m) + "expected: not equal but was: <" + unexpected + ">");
    }
    static void rethrow(Throwable t) {
        if (t instanceof RuntimeException) throw (RuntimeException) t;
        if (t instanceof Error) throw (Error) t;
        throw new AssertionFailedError("unexpected exception", t);
    }

    public static <V> V fail() { throw new AssertionFailedError(); }
    public static <V> V fail(String message) { throw new AssertionFailedError(message); }
    public static <V> V fail(String message, Throwable cause) { throw new AssertionFailedError(message, cause); }
    public static <V> V fail(Throwable cause) { throw new AssertionFailedError(cause == null ? null : cause.toString(), cause); }
    public static <V> V fail(Supplier<String> messageSupplier) { throw new AssertionFailedError(messageSupplier == null ? null : messageSupplier.get()); }
    public static void assertTrue(boolean c) { if (!c) throw new AssertionFailedError(msg(null) + "expected: <true> but was: <false>"); }
    public static void assertTrue(boolean c, String message) { if (!c) throw new AssertionFailedError(msg(message) + "expected: <true> but was: <false>"); }
    public static void assertTrue(boolean c, Supplier<String> messageSupplier) { if (!c) throw new AssertionFailedError(msg(messageSupplier) + "expected: <true> but was: <false>"); }
    public static void assertTrue(BooleanSupplier s) { boolean c = s.getAsBoolean(); if (!c) throw new AssertionFailedError(msg(null) + "expected: <true> but was: <false>"); }
    public static void assertTrue(BooleanSupplier s, String message) { boolean c = s.getAsBoolean(); if (!c) throw new AssertionFailedError(msg(message) + "expected: <true> but was: <false>"); }
    public static void assertTrue(BooleanSupplier s, Supplier<String> messageSupplier) { boolean c = s.getAsBoolean(); if (!c) throw new AssertionFailedError(msg(messageSupplier) + "expected: <true> but was: <false>"); }
    public static void assertFalse(boolean c) { if (c) throw new AssertionFailedError(msg(null) + "expected: <false> but was: <true>"); }
    public static void assertFalse(boolean c, String message) { if (c) throw new AssertionFailedError(msg(message) + "expected: <false> but was: <true>"); }
    public static void assertFalse(boolean c, Supplier<String> messageSupplier) { if (c) throw new AssertionFailedError(msg(messageSupplier) + "expected: <false> but was: <true>"); }
    public static void assertFalse(BooleanSupplier s) { boolean c = s.getAsBoolean(); if (c) throw new AssertionFailedError(msg(null) + "expected: <false> but was: <true>"); }
    public static void assertFalse(BooleanSupplier s, String message) { boolean c = s.getAsBoolean(); if (c) throw new AssertionFailedError(msg(message) + "expected: <false> but was: <true>"); }
    public static void assertFalse(BooleanSupplier s, Supplier<String> messageSupplier) { boolean c = s.getAsBoolean(); if (c) throw new AssertionFailedError(msg(messageSupplier) + "expected: <false> but was: <true>"); }
    public static void assertNull(Object actual) { if (actual != null) failEquals(null, actual, null); }
    public static void assertNotNull(Object actual) { if (actual == null) throw new AssertionFailedError(msg(null) + "expected: not <null>"); }
    public static void assertSame(Object expected, Object actual) { if (expected != actual) failEquals(expected, actual, null); }
    public static void assertNotSame(Object unexpected, Object actual) { if (unexpected == actual) throw new AssertionFailedError(msg(null) + "expected: not same but was: <" + actual + ">"); }
    public static void assertEquals(Object expected, Object actual) { if (!Objects.equals(expected, actual)) failEquals(expected, actual, null); }
    public static void assertNotEquals(Object unexpected, Object actual) { if (Objects.equals(unexpected, actual)) failNotEquals(actual, null); }
    public static void assertArrayEquals(Object[] expected, Object[] actual) { if (!Arrays.deepEquals(expected, actual)) failEquals(Arrays.deepToString(expected), Arrays.deepToString(actual), null); }
    public static void assertIterableEquals(Iterable<?> expected, Iterable<?> actual) { if (!iterableEquals(expected, actual)) failEquals(expected, actual, null); }
    public static void assertLinesMatch(List<String> expected, List<String> actual) { if (!linesMatch(expected, actual)) failEquals(expected, actual, null); }
    public static void assertArrayEquals(boolean[] expected, boolean[] actual) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), null); }
    public static void assertArrayEquals(char[] expected, char[] actual) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), null); }
    public static void assertArrayEquals(byte[] expected, byte[] actual) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), null); }
    public static void assertArrayEquals(short[] expected, short[] actual) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), null); }
    public static void assertArrayEquals(int[] expected, int[] actual) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), null); }
    public static void assertArrayEquals(long[] expected, long[] actual) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), null); }
    public static void assertArrayEquals(float[] expected, float[] actual) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), null); }
    public static void assertArrayEquals(double[] expected, double[] actual) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), null); }
    public static void assertArrayEquals(float[] expected, float[] actual, float delta) {
        if (expected == actual) return;
        if (expected == null || actual == null || expected.length != actual.length) { failEquals(Arrays.toString(expected), Arrays.toString(actual), null); return; }
        for (int i = 0; i < expected.length; i++) if (!near(expected[i], actual[i], delta)) failEquals(Arrays.toString(expected), Arrays.toString(actual), null);
    }
    public static void assertArrayEquals(double[] expected, double[] actual, double delta) {
        if (expected == actual) return;
        if (expected == null || actual == null || expected.length != actual.length) { failEquals(Arrays.toString(expected), Arrays.toString(actual), null); return; }
        for (int i = 0; i < expected.length; i++) if (!near(expected[i], actual[i], delta)) failEquals(Arrays.toString(expected), Arrays.toString(actual), null);
    }
    public static void assertEquals(byte expected, byte actual) { if (!(Objects.equals((Byte) expected, (Byte) actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(byte unexpected, byte actual) { if (Objects.equals((Byte) unexpected, (Byte) actual)) failNotEquals(actual, null); }
    public static void assertEquals(byte expected, Byte actual) { if (!(Objects.equals((Byte) expected, (Byte) actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(byte unexpected, Byte actual) { if (Objects.equals((Byte) unexpected, (Byte) actual)) failNotEquals(actual, null); }
    public static void assertEquals(Byte expected, byte actual) { if (!(Objects.equals((Byte) expected, (Byte) actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(Byte unexpected, byte actual) { if (Objects.equals((Byte) unexpected, (Byte) actual)) failNotEquals(actual, null); }
    public static void assertEquals(Byte expected, Byte actual) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(Byte unexpected, Byte actual) { if (Objects.equals((Byte) unexpected, (Byte) actual)) failNotEquals(actual, null); }
    public static void assertEquals(short expected, short actual) { if (!(Objects.equals((Short) expected, (Short) actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(short unexpected, short actual) { if (Objects.equals((Short) unexpected, (Short) actual)) failNotEquals(actual, null); }
    public static void assertEquals(short expected, Short actual) { if (!(Objects.equals((Short) expected, (Short) actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(short unexpected, Short actual) { if (Objects.equals((Short) unexpected, (Short) actual)) failNotEquals(actual, null); }
    public static void assertEquals(Short expected, short actual) { if (!(Objects.equals((Short) expected, (Short) actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(Short unexpected, short actual) { if (Objects.equals((Short) unexpected, (Short) actual)) failNotEquals(actual, null); }
    public static void assertEquals(Short expected, Short actual) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(Short unexpected, Short actual) { if (Objects.equals((Short) unexpected, (Short) actual)) failNotEquals(actual, null); }
    public static void assertEquals(int expected, int actual) { if (!(Objects.equals((Integer) expected, (Integer) actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(int unexpected, int actual) { if (Objects.equals((Integer) unexpected, (Integer) actual)) failNotEquals(actual, null); }
    public static void assertEquals(int expected, Integer actual) { if (!(Objects.equals((Integer) expected, (Integer) actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(int unexpected, Integer actual) { if (Objects.equals((Integer) unexpected, (Integer) actual)) failNotEquals(actual, null); }
    public static void assertEquals(Integer expected, int actual) { if (!(Objects.equals((Integer) expected, (Integer) actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(Integer unexpected, int actual) { if (Objects.equals((Integer) unexpected, (Integer) actual)) failNotEquals(actual, null); }
    public static void assertEquals(Integer expected, Integer actual) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(Integer unexpected, Integer actual) { if (Objects.equals((Integer) unexpected, (Integer) actual)) failNotEquals(actual, null); }
    public static void assertEquals(long expected, long actual) { if (!(Objects.equals((Long) expected, (Long) actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(long unexpected, long actual) { if (Objects.equals((Long) unexpected, (Long) actual)) failNotEquals(actual, null); }
    public static void assertEquals(long expected, Long actual) { if (!(Objects.equals((Long) expected, (Long) actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(long unexpected, Long actual) { if (Objects.equals((Long) unexpected, (Long) actual)) failNotEquals(actual, null); }
    public static void assertEquals(Long expected, long actual) { if (!(Objects.equals((Long) expected, (Long) actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(Long unexpected, long actual) { if (Objects.equals((Long) unexpected, (Long) actual)) failNotEquals(actual, null); }
    public static void assertEquals(Long expected, Long actual) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(Long unexpected, Long actual) { if (Objects.equals((Long) unexpected, (Long) actual)) failNotEquals(actual, null); }
    public static void assertEquals(char expected, char actual) { if (!(Objects.equals((Character) expected, (Character) actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(char unexpected, char actual) { if (Objects.equals((Character) unexpected, (Character) actual)) failNotEquals(actual, null); }
    public static void assertEquals(char expected, Character actual) { if (!(Objects.equals((Character) expected, (Character) actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(char unexpected, Character actual) { if (Objects.equals((Character) unexpected, (Character) actual)) failNotEquals(actual, null); }
    public static void assertEquals(Character expected, char actual) { if (!(Objects.equals((Character) expected, (Character) actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(Character unexpected, char actual) { if (Objects.equals((Character) unexpected, (Character) actual)) failNotEquals(actual, null); }
    public static void assertEquals(Character expected, Character actual) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(Character unexpected, Character actual) { if (Objects.equals((Character) unexpected, (Character) actual)) failNotEquals(actual, null); }
    public static void assertEquals(float expected, float actual) { if (!(near((float) (Float) expected, (float) (Float) actual, 0))) failEquals(expected, actual, null); }
    public static void assertNotEquals(float unexpected, float actual) { if (Objects.equals((Float) unexpected, (Float) actual)) failNotEquals(actual, null); }
    public static void assertEquals(float expected, Float actual) { if (!(near((float) (Float) expected, (float) (Float) actual, 0))) failEquals(expected, actual, null); }
    public static void assertNotEquals(float unexpected, Float actual) { if (Objects.equals((Float) unexpected, (Float) actual)) failNotEquals(actual, null); }
    public static void assertEquals(Float expected, float actual) { if (!(near((float) (Float) expected, (float) (Float) actual, 0))) failEquals(expected, actual, null); }
    public static void assertNotEquals(Float unexpected, float actual) { if (Objects.equals((Float) unexpected, (Float) actual)) failNotEquals(actual, null); }
    public static void assertEquals(Float expected, Float actual) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(Float unexpected, Float actual) { if (Objects.equals((Float) unexpected, (Float) actual)) failNotEquals(actual, null); }
    public static void assertEquals(float expected, float actual, float delta) { if (!near(expected, actual, delta)) failEquals(expected, actual, null); }
    public static void assertNotEquals(float unexpected, float actual, float delta) { if (near(unexpected, actual, delta)) failNotEquals(actual, null); }
    public static void assertEquals(double expected, double actual) { if (!(near((double) (Double) expected, (double) (Double) actual, 0))) failEquals(expected, actual, null); }
    public static void assertNotEquals(double unexpected, double actual) { if (Objects.equals((Double) unexpected, (Double) actual)) failNotEquals(actual, null); }
    public static void assertEquals(double expected, Double actual) { if (!(near((double) (Double) expected, (double) (Double) actual, 0))) failEquals(expected, actual, null); }
    public static void assertNotEquals(double unexpected, Double actual) { if (Objects.equals((Double) unexpected, (Double) actual)) failNotEquals(actual, null); }
    public static void assertEquals(Double expected, double actual) { if (!(near((double) (Double) expected, (double) (Double) actual, 0))) failEquals(expected, actual, null); }
    public static void assertNotEquals(Double unexpected, double actual) { if (Objects.equals((Double) unexpected, (Double) actual)) failNotEquals(actual, null); }
    public static void assertEquals(Double expected, Double actual) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, null); }
    public static void assertNotEquals(Double unexpected, Double actual) { if (Objects.equals((Double) unexpected, (Double) actual)) failNotEquals(actual, null); }
    public static void assertEquals(double expected, double actual, double delta) { if (!near(expected, actual, delta)) failEquals(expected, actual, null); }
    public static void assertNotEquals(double unexpected, double actual, double delta) { if (near(unexpected, actual, delta)) failNotEquals(actual, null); }
    public static <T extends Throwable> T assertThrows(Class<T> expectedType, Executable executable) { return throwsOf(expectedType, executable, false, null); }
    public static <T extends Throwable> T assertThrowsExactly(Class<T> expectedType, Executable executable) { return throwsOf(expectedType, executable, true, null); }
    public static void assertDoesNotThrow(Executable executable) {
        try { executable.execute(); } catch (Throwable t) { throw new AssertionFailedError(msg(null) + "Unexpected exception thrown: " + t, t); }
    }
    public static <T> T assertDoesNotThrow(ThrowingSupplier<T> supplier) {
        try { return supplier.get(); } catch (Throwable t) { throw new AssertionFailedError(msg(null) + "Unexpected exception thrown: " + t, t); }
    }
    public static void assertTimeout(Duration timeout, Executable executable) { timed(timeout, () -> { executable.execute(); return null; }, null); }
    public static <T> T assertTimeout(Duration timeout, ThrowingSupplier<T> supplier) { return timed(timeout, supplier, null); }
    public static void assertTimeoutPreemptively(Duration timeout, Executable executable) { timed(timeout, () -> { executable.execute(); return null; }, null); }
    public static <T> T assertTimeoutPreemptively(Duration timeout, ThrowingSupplier<T> supplier) { return timed(timeout, supplier, null); }
    public static <T> T assertInstanceOf(Class<T> expectedType, Object actual) {
        if (!expectedType.isInstance(actual)) throw new AssertionFailedError(msg(null) + (actual == null ? "Unexpected null value" : "Unexpected type") + ", expected: <" + expectedType.getName() + "> but was: <" + (actual == null ? null : actual.getClass().getName()) + ">");
        return expectedType.cast(actual);
    }
    public static void assertNull(Object actual, String message) { if (actual != null) failEquals(null, actual, message); }
    public static void assertNotNull(Object actual, String message) { if (actual == null) throw new AssertionFailedError(msg(message) + "expected: not <null>"); }
    public static void assertSame(Object expected, Object actual, String message) { if (expected != actual) failEquals(expected, actual, message); }
    public static void assertNotSame(Object unexpected, Object actual, String message) { if (unexpected == actual) throw new AssertionFailedError(msg(message) + "expected: not same but was: <" + actual + ">"); }
    public static void assertEquals(Object expected, Object actual, String message) { if (!Objects.equals(expected, actual)) failEquals(expected, actual, message); }
    public static void assertNotEquals(Object unexpected, Object actual, String message) { if (Objects.equals(unexpected, actual)) failNotEquals(actual, message); }
    public static void assertArrayEquals(Object[] expected, Object[] actual, String message) { if (!Arrays.deepEquals(expected, actual)) failEquals(Arrays.deepToString(expected), Arrays.deepToString(actual), message); }
    public static void assertIterableEquals(Iterable<?> expected, Iterable<?> actual, String message) { if (!iterableEquals(expected, actual)) failEquals(expected, actual, message); }
    public static void assertLinesMatch(List<String> expected, List<String> actual, String message) { if (!linesMatch(expected, actual)) failEquals(expected, actual, message); }
    public static void assertArrayEquals(boolean[] expected, boolean[] actual, String message) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), message); }
    public static void assertArrayEquals(char[] expected, char[] actual, String message) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), message); }
    public static void assertArrayEquals(byte[] expected, byte[] actual, String message) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), message); }
    public static void assertArrayEquals(short[] expected, short[] actual, String message) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), message); }
    public static void assertArrayEquals(int[] expected, int[] actual, String message) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), message); }
    public static void assertArrayEquals(long[] expected, long[] actual, String message) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), message); }
    public static void assertArrayEquals(float[] expected, float[] actual, String message) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), message); }
    public static void assertArrayEquals(double[] expected, double[] actual, String message) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), message); }
    public static void assertArrayEquals(float[] expected, float[] actual, float delta, String message) {
        if (expected == actual) return;
        if (expected == null || actual == null || expected.length != actual.length) { failEquals(Arrays.toString(expected), Arrays.toString(actual), message); return; }
        for (int i = 0; i < expected.length; i++) if (!near(expected[i], actual[i], delta)) failEquals(Arrays.toString(expected), Arrays.toString(actual), message);
    }
    public static void assertArrayEquals(double[] expected, double[] actual, double delta, String message) {
        if (expected == actual) return;
        if (expected == null || actual == null || expected.length != actual.length) { failEquals(Arrays.toString(expected), Arrays.toString(actual), message); return; }
        for (int i = 0; i < expected.length; i++) if (!near(expected[i], actual[i], delta)) failEquals(Arrays.toString(expected), Arrays.toString(actual), message);
    }
    public static void assertEquals(byte expected, byte actual, String message) { if (!(Objects.equals((Byte) expected, (Byte) actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(byte unexpected, byte actual, String message) { if (Objects.equals((Byte) unexpected, (Byte) actual)) failNotEquals(actual, message); }
    public static void assertEquals(byte expected, Byte actual, String message) { if (!(Objects.equals((Byte) expected, (Byte) actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(byte unexpected, Byte actual, String message) { if (Objects.equals((Byte) unexpected, (Byte) actual)) failNotEquals(actual, message); }
    public static void assertEquals(Byte expected, byte actual, String message) { if (!(Objects.equals((Byte) expected, (Byte) actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(Byte unexpected, byte actual, String message) { if (Objects.equals((Byte) unexpected, (Byte) actual)) failNotEquals(actual, message); }
    public static void assertEquals(Byte expected, Byte actual, String message) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(Byte unexpected, Byte actual, String message) { if (Objects.equals((Byte) unexpected, (Byte) actual)) failNotEquals(actual, message); }
    public static void assertEquals(short expected, short actual, String message) { if (!(Objects.equals((Short) expected, (Short) actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(short unexpected, short actual, String message) { if (Objects.equals((Short) unexpected, (Short) actual)) failNotEquals(actual, message); }
    public static void assertEquals(short expected, Short actual, String message) { if (!(Objects.equals((Short) expected, (Short) actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(short unexpected, Short actual, String message) { if (Objects.equals((Short) unexpected, (Short) actual)) failNotEquals(actual, message); }
    public static void assertEquals(Short expected, short actual, String message) { if (!(Objects.equals((Short) expected, (Short) actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(Short unexpected, short actual, String message) { if (Objects.equals((Short) unexpected, (Short) actual)) failNotEquals(actual, message); }
    public static void assertEquals(Short expected, Short actual, String message) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(Short unexpected, Short actual, String message) { if (Objects.equals((Short) unexpected, (Short) actual)) failNotEquals(actual, message); }
    public static void assertEquals(int expected, int actual, String message) { if (!(Objects.equals((Integer) expected, (Integer) actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(int unexpected, int actual, String message) { if (Objects.equals((Integer) unexpected, (Integer) actual)) failNotEquals(actual, message); }
    public static void assertEquals(int expected, Integer actual, String message) { if (!(Objects.equals((Integer) expected, (Integer) actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(int unexpected, Integer actual, String message) { if (Objects.equals((Integer) unexpected, (Integer) actual)) failNotEquals(actual, message); }
    public static void assertEquals(Integer expected, int actual, String message) { if (!(Objects.equals((Integer) expected, (Integer) actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(Integer unexpected, int actual, String message) { if (Objects.equals((Integer) unexpected, (Integer) actual)) failNotEquals(actual, message); }
    public static void assertEquals(Integer expected, Integer actual, String message) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(Integer unexpected, Integer actual, String message) { if (Objects.equals((Integer) unexpected, (Integer) actual)) failNotEquals(actual, message); }
    public static void assertEquals(long expected, long actual, String message) { if (!(Objects.equals((Long) expected, (Long) actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(long unexpected, long actual, String message) { if (Objects.equals((Long) unexpected, (Long) actual)) failNotEquals(actual, message); }
    public static void assertEquals(long expected, Long actual, String message) { if (!(Objects.equals((Long) expected, (Long) actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(long unexpected, Long actual, String message) { if (Objects.equals((Long) unexpected, (Long) actual)) failNotEquals(actual, message); }
    public static void assertEquals(Long expected, long actual, String message) { if (!(Objects.equals((Long) expected, (Long) actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(Long unexpected, long actual, String message) { if (Objects.equals((Long) unexpected, (Long) actual)) failNotEquals(actual, message); }
    public static void assertEquals(Long expected, Long actual, String message) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(Long unexpected, Long actual, String message) { if (Objects.equals((Long) unexpected, (Long) actual)) failNotEquals(actual, message); }
    public static void assertEquals(char expected, char actual, String message) { if (!(Objects.equals((Character) expected, (Character) actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(char unexpected, char actual, String message) { if (Objects.equals((Character) unexpected, (Character) actual)) failNotEquals(actual, message); }
    public static void assertEquals(char expected, Character actual, String message) { if (!(Objects.equals((Character) expected, (Character) actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(char unexpected, Character actual, String message) { if (Objects.equals((Character) unexpected, (Character) actual)) failNotEquals(actual, message); }
    public static void assertEquals(Character expected, char actual, String message) { if (!(Objects.equals((Character) expected, (Character) actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(Character unexpected, char actual, String message) { if (Objects.equals((Character) unexpected, (Character) actual)) failNotEquals(actual, message); }
    public static void assertEquals(Character expected, Character actual, String message) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(Character unexpected, Character actual, String message) { if (Objects.equals((Character) unexpected, (Character) actual)) failNotEquals(actual, message); }
    public static void assertEquals(float expected, float actual, String message) { if (!(near((float) (Float) expected, (float) (Float) actual, 0))) failEquals(expected, actual, message); }
    public static void assertNotEquals(float unexpected, float actual, String message) { if (Objects.equals((Float) unexpected, (Float) actual)) failNotEquals(actual, message); }
    public static void assertEquals(float expected, Float actual, String message) { if (!(near((float) (Float) expected, (float) (Float) actual, 0))) failEquals(expected, actual, message); }
    public static void assertNotEquals(float unexpected, Float actual, String message) { if (Objects.equals((Float) unexpected, (Float) actual)) failNotEquals(actual, message); }
    public static void assertEquals(Float expected, float actual, String message) { if (!(near((float) (Float) expected, (float) (Float) actual, 0))) failEquals(expected, actual, message); }
    public static void assertNotEquals(Float unexpected, float actual, String message) { if (Objects.equals((Float) unexpected, (Float) actual)) failNotEquals(actual, message); }
    public static void assertEquals(Float expected, Float actual, String message) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(Float unexpected, Float actual, String message) { if (Objects.equals((Float) unexpected, (Float) actual)) failNotEquals(actual, message); }
    public static void assertEquals(float expected, float actual, float delta, String message) { if (!near(expected, actual, delta)) failEquals(expected, actual, message); }
    public static void assertNotEquals(float unexpected, float actual, float delta, String message) { if (near(unexpected, actual, delta)) failNotEquals(actual, message); }
    public static void assertEquals(double expected, double actual, String message) { if (!(near((double) (Double) expected, (double) (Double) actual, 0))) failEquals(expected, actual, message); }
    public static void assertNotEquals(double unexpected, double actual, String message) { if (Objects.equals((Double) unexpected, (Double) actual)) failNotEquals(actual, message); }
    public static void assertEquals(double expected, Double actual, String message) { if (!(near((double) (Double) expected, (double) (Double) actual, 0))) failEquals(expected, actual, message); }
    public static void assertNotEquals(double unexpected, Double actual, String message) { if (Objects.equals((Double) unexpected, (Double) actual)) failNotEquals(actual, message); }
    public static void assertEquals(Double expected, double actual, String message) { if (!(near((double) (Double) expected, (double) (Double) actual, 0))) failEquals(expected, actual, message); }
    public static void assertNotEquals(Double unexpected, double actual, String message) { if (Objects.equals((Double) unexpected, (Double) actual)) failNotEquals(actual, message); }
    public static void assertEquals(Double expected, Double actual, String message) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, message); }
    public static void assertNotEquals(Double unexpected, Double actual, String message) { if (Objects.equals((Double) unexpected, (Double) actual)) failNotEquals(actual, message); }
    public static void assertEquals(double expected, double actual, double delta, String message) { if (!near(expected, actual, delta)) failEquals(expected, actual, message); }
    public static void assertNotEquals(double unexpected, double actual, double delta, String message) { if (near(unexpected, actual, delta)) failNotEquals(actual, message); }
    public static <T extends Throwable> T assertThrows(Class<T> expectedType, Executable executable, String message) { return throwsOf(expectedType, executable, false, message); }
    public static <T extends Throwable> T assertThrowsExactly(Class<T> expectedType, Executable executable, String message) { return throwsOf(expectedType, executable, true, message); }
    public static void assertDoesNotThrow(Executable executable, String message) {
        try { executable.execute(); } catch (Throwable t) { throw new AssertionFailedError(msg(message) + "Unexpected exception thrown: " + t, t); }
    }
    public static <T> T assertDoesNotThrow(ThrowingSupplier<T> supplier, String message) {
        try { return supplier.get(); } catch (Throwable t) { throw new AssertionFailedError(msg(message) + "Unexpected exception thrown: " + t, t); }
    }
    public static void assertTimeout(Duration timeout, Executable executable, String message) { timed(timeout, () -> { executable.execute(); return null; }, message); }
    public static <T> T assertTimeout(Duration timeout, ThrowingSupplier<T> supplier, String message) { return timed(timeout, supplier, message); }
    public static void assertTimeoutPreemptively(Duration timeout, Executable executable, String message) { timed(timeout, () -> { executable.execute(); return null; }, message); }
    public static <T> T assertTimeoutPreemptively(Duration timeout, ThrowingSupplier<T> supplier, String message) { return timed(timeout, supplier, message); }
    public static <T> T assertInstanceOf(Class<T> expectedType, Object actual, String message) {
        if (!expectedType.isInstance(actual)) throw new AssertionFailedError(msg(message) + (actual == null ? "Unexpected null value" : "Unexpected type") + ", expected: <" + expectedType.getName() + "> but was: <" + (actual == null ? null : actual.getClass().getName()) + ">");
        return expectedType.cast(actual);
    }
    public static void assertNull(Object actual, Supplier<String> messageSupplier) { if (actual != null) failEquals(null, actual, messageSupplier); }
    public static void assertNotNull(Object actual, Supplier<String> messageSupplier) { if (actual == null) throw new AssertionFailedError(msg(messageSupplier) + "expected: not <null>"); }
    public static void assertSame(Object expected, Object actual, Supplier<String> messageSupplier) { if (expected != actual) failEquals(expected, actual, messageSupplier); }
    public static void assertNotSame(Object unexpected, Object actual, Supplier<String> messageSupplier) { if (unexpected == actual) throw new AssertionFailedError(msg(messageSupplier) + "expected: not same but was: <" + actual + ">"); }
    public static void assertEquals(Object expected, Object actual, Supplier<String> messageSupplier) { if (!Objects.equals(expected, actual)) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(Object unexpected, Object actual, Supplier<String> messageSupplier) { if (Objects.equals(unexpected, actual)) failNotEquals(actual, messageSupplier); }
    public static void assertArrayEquals(Object[] expected, Object[] actual, Supplier<String> messageSupplier) { if (!Arrays.deepEquals(expected, actual)) failEquals(Arrays.deepToString(expected), Arrays.deepToString(actual), messageSupplier); }
    public static void assertIterableEquals(Iterable<?> expected, Iterable<?> actual, Supplier<String> messageSupplier) { if (!iterableEquals(expected, actual)) failEquals(expected, actual, messageSupplier); }
    public static void assertLinesMatch(List<String> expected, List<String> actual, Supplier<String> messageSupplier) { if (!linesMatch(expected, actual)) failEquals(expected, actual, messageSupplier); }
    public static void assertArrayEquals(boolean[] expected, boolean[] actual, Supplier<String> messageSupplier) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), messageSupplier); }
    public static void assertArrayEquals(char[] expected, char[] actual, Supplier<String> messageSupplier) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), messageSupplier); }
    public static void assertArrayEquals(byte[] expected, byte[] actual, Supplier<String> messageSupplier) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), messageSupplier); }
    public static void assertArrayEquals(short[] expected, short[] actual, Supplier<String> messageSupplier) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), messageSupplier); }
    public static void assertArrayEquals(int[] expected, int[] actual, Supplier<String> messageSupplier) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), messageSupplier); }
    public static void assertArrayEquals(long[] expected, long[] actual, Supplier<String> messageSupplier) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), messageSupplier); }
    public static void assertArrayEquals(float[] expected, float[] actual, Supplier<String> messageSupplier) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), messageSupplier); }
    public static void assertArrayEquals(double[] expected, double[] actual, Supplier<String> messageSupplier) { if (!Arrays.equals(expected, actual)) failEquals(Arrays.toString(expected), Arrays.toString(actual), messageSupplier); }
    public static void assertArrayEquals(float[] expected, float[] actual, float delta, Supplier<String> messageSupplier) {
        if (expected == actual) return;
        if (expected == null || actual == null || expected.length != actual.length) { failEquals(Arrays.toString(expected), Arrays.toString(actual), messageSupplier); return; }
        for (int i = 0; i < expected.length; i++) if (!near(expected[i], actual[i], delta)) failEquals(Arrays.toString(expected), Arrays.toString(actual), messageSupplier);
    }
    public static void assertArrayEquals(double[] expected, double[] actual, double delta, Supplier<String> messageSupplier) {
        if (expected == actual) return;
        if (expected == null || actual == null || expected.length != actual.length) { failEquals(Arrays.toString(expected), Arrays.toString(actual), messageSupplier); return; }
        for (int i = 0; i < expected.length; i++) if (!near(expected[i], actual[i], delta)) failEquals(Arrays.toString(expected), Arrays.toString(actual), messageSupplier);
    }
    public static void assertEquals(byte expected, byte actual, Supplier<String> messageSupplier) { if (!(Objects.equals((Byte) expected, (Byte) actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(byte unexpected, byte actual, Supplier<String> messageSupplier) { if (Objects.equals((Byte) unexpected, (Byte) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(byte expected, Byte actual, Supplier<String> messageSupplier) { if (!(Objects.equals((Byte) expected, (Byte) actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(byte unexpected, Byte actual, Supplier<String> messageSupplier) { if (Objects.equals((Byte) unexpected, (Byte) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(Byte expected, byte actual, Supplier<String> messageSupplier) { if (!(Objects.equals((Byte) expected, (Byte) actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(Byte unexpected, byte actual, Supplier<String> messageSupplier) { if (Objects.equals((Byte) unexpected, (Byte) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(Byte expected, Byte actual, Supplier<String> messageSupplier) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(Byte unexpected, Byte actual, Supplier<String> messageSupplier) { if (Objects.equals((Byte) unexpected, (Byte) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(short expected, short actual, Supplier<String> messageSupplier) { if (!(Objects.equals((Short) expected, (Short) actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(short unexpected, short actual, Supplier<String> messageSupplier) { if (Objects.equals((Short) unexpected, (Short) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(short expected, Short actual, Supplier<String> messageSupplier) { if (!(Objects.equals((Short) expected, (Short) actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(short unexpected, Short actual, Supplier<String> messageSupplier) { if (Objects.equals((Short) unexpected, (Short) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(Short expected, short actual, Supplier<String> messageSupplier) { if (!(Objects.equals((Short) expected, (Short) actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(Short unexpected, short actual, Supplier<String> messageSupplier) { if (Objects.equals((Short) unexpected, (Short) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(Short expected, Short actual, Supplier<String> messageSupplier) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(Short unexpected, Short actual, Supplier<String> messageSupplier) { if (Objects.equals((Short) unexpected, (Short) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(int expected, int actual, Supplier<String> messageSupplier) { if (!(Objects.equals((Integer) expected, (Integer) actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(int unexpected, int actual, Supplier<String> messageSupplier) { if (Objects.equals((Integer) unexpected, (Integer) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(int expected, Integer actual, Supplier<String> messageSupplier) { if (!(Objects.equals((Integer) expected, (Integer) actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(int unexpected, Integer actual, Supplier<String> messageSupplier) { if (Objects.equals((Integer) unexpected, (Integer) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(Integer expected, int actual, Supplier<String> messageSupplier) { if (!(Objects.equals((Integer) expected, (Integer) actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(Integer unexpected, int actual, Supplier<String> messageSupplier) { if (Objects.equals((Integer) unexpected, (Integer) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(Integer expected, Integer actual, Supplier<String> messageSupplier) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(Integer unexpected, Integer actual, Supplier<String> messageSupplier) { if (Objects.equals((Integer) unexpected, (Integer) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(long expected, long actual, Supplier<String> messageSupplier) { if (!(Objects.equals((Long) expected, (Long) actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(long unexpected, long actual, Supplier<String> messageSupplier) { if (Objects.equals((Long) unexpected, (Long) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(long expected, Long actual, Supplier<String> messageSupplier) { if (!(Objects.equals((Long) expected, (Long) actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(long unexpected, Long actual, Supplier<String> messageSupplier) { if (Objects.equals((Long) unexpected, (Long) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(Long expected, long actual, Supplier<String> messageSupplier) { if (!(Objects.equals((Long) expected, (Long) actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(Long unexpected, long actual, Supplier<String> messageSupplier) { if (Objects.equals((Long) unexpected, (Long) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(Long expected, Long actual, Supplier<String> messageSupplier) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(Long unexpected, Long actual, Supplier<String> messageSupplier) { if (Objects.equals((Long) unexpected, (Long) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(char expected, char actual, Supplier<String> messageSupplier) { if (!(Objects.equals((Character) expected, (Character) actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(char unexpected, char actual, Supplier<String> messageSupplier) { if (Objects.equals((Character) unexpected, (Character) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(char expected, Character actual, Supplier<String> messageSupplier) { if (!(Objects.equals((Character) expected, (Character) actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(char unexpected, Character actual, Supplier<String> messageSupplier) { if (Objects.equals((Character) unexpected, (Character) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(Character expected, char actual, Supplier<String> messageSupplier) { if (!(Objects.equals((Character) expected, (Character) actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(Character unexpected, char actual, Supplier<String> messageSupplier) { if (Objects.equals((Character) unexpected, (Character) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(Character expected, Character actual, Supplier<String> messageSupplier) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(Character unexpected, Character actual, Supplier<String> messageSupplier) { if (Objects.equals((Character) unexpected, (Character) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(float expected, float actual, Supplier<String> messageSupplier) { if (!(near((float) (Float) expected, (float) (Float) actual, 0))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(float unexpected, float actual, Supplier<String> messageSupplier) { if (Objects.equals((Float) unexpected, (Float) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(float expected, Float actual, Supplier<String> messageSupplier) { if (!(near((float) (Float) expected, (float) (Float) actual, 0))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(float unexpected, Float actual, Supplier<String> messageSupplier) { if (Objects.equals((Float) unexpected, (Float) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(Float expected, float actual, Supplier<String> messageSupplier) { if (!(near((float) (Float) expected, (float) (Float) actual, 0))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(Float unexpected, float actual, Supplier<String> messageSupplier) { if (Objects.equals((Float) unexpected, (Float) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(Float expected, Float actual, Supplier<String> messageSupplier) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(Float unexpected, Float actual, Supplier<String> messageSupplier) { if (Objects.equals((Float) unexpected, (Float) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(float expected, float actual, float delta, Supplier<String> messageSupplier) { if (!near(expected, actual, delta)) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(float unexpected, float actual, float delta, Supplier<String> messageSupplier) { if (near(unexpected, actual, delta)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(double expected, double actual, Supplier<String> messageSupplier) { if (!(near((double) (Double) expected, (double) (Double) actual, 0))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(double unexpected, double actual, Supplier<String> messageSupplier) { if (Objects.equals((Double) unexpected, (Double) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(double expected, Double actual, Supplier<String> messageSupplier) { if (!(near((double) (Double) expected, (double) (Double) actual, 0))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(double unexpected, Double actual, Supplier<String> messageSupplier) { if (Objects.equals((Double) unexpected, (Double) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(Double expected, double actual, Supplier<String> messageSupplier) { if (!(near((double) (Double) expected, (double) (Double) actual, 0))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(Double unexpected, double actual, Supplier<String> messageSupplier) { if (Objects.equals((Double) unexpected, (Double) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(Double expected, Double actual, Supplier<String> messageSupplier) { if (!(Objects.equals(expected, actual))) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(Double unexpected, Double actual, Supplier<String> messageSupplier) { if (Objects.equals((Double) unexpected, (Double) actual)) failNotEquals(actual, messageSupplier); }
    public static void assertEquals(double expected, double actual, double delta, Supplier<String> messageSupplier) { if (!near(expected, actual, delta)) failEquals(expected, actual, messageSupplier); }
    public static void assertNotEquals(double unexpected, double actual, double delta, Supplier<String> messageSupplier) { if (near(unexpected, actual, delta)) failNotEquals(actual, messageSupplier); }
    public static <T extends Throwable> T assertThrows(Class<T> expectedType, Executable executable, Supplier<String> messageSupplier) { return throwsOf(expectedType, executable, false, messageSupplier); }
    public static <T extends Throwable> T assertThrowsExactly(Class<T> expectedType, Executable executable, Supplier<String> messageSupplier) { return throwsOf(expectedType, executable, true, messageSupplier); }
    public static void assertDoesNotThrow(Executable executable, Supplier<String> messageSupplier) {
        try { executable.execute(); } catch (Throwable t) { throw new AssertionFailedError(msg(messageSupplier) + "Unexpected exception thrown: " + t, t); }
    }
    public static <T> T assertDoesNotThrow(ThrowingSupplier<T> supplier, Supplier<String> messageSupplier) {
        try { return supplier.get(); } catch (Throwable t) { throw new AssertionFailedError(msg(messageSupplier) + "Unexpected exception thrown: " + t, t); }
    }
    public static void assertTimeout(Duration timeout, Executable executable, Supplier<String> messageSupplier) { timed(timeout, () -> { executable.execute(); return null; }, messageSupplier); }
    public static <T> T assertTimeout(Duration timeout, ThrowingSupplier<T> supplier, Supplier<String> messageSupplier) { return timed(timeout, supplier, messageSupplier); }
    public static void assertTimeoutPreemptively(Duration timeout, Executable executable, Supplier<String> messageSupplier) { timed(timeout, () -> { executable.execute(); return null; }, messageSupplier); }
    public static <T> T assertTimeoutPreemptively(Duration timeout, ThrowingSupplier<T> supplier, Supplier<String> messageSupplier) { return timed(timeout, supplier, messageSupplier); }
    public static <T> T assertInstanceOf(Class<T> expectedType, Object actual, Supplier<String> messageSupplier) {
        if (!expectedType.isInstance(actual)) throw new AssertionFailedError(msg(messageSupplier) + (actual == null ? "Unexpected null value" : "Unexpected type") + ", expected: <" + expectedType.getName() + "> but was: <" + (actual == null ? null : actual.getClass().getName()) + ">");
        return expectedType.cast(actual);
    }

    static boolean near(double expected, double actual, double delta) {
        return Double.doubleToLongBits(expected) == Double.doubleToLongBits(actual) || Math.abs(expected - actual) <= delta;
    }
    static boolean near(float expected, float actual, float delta) {
        return Float.floatToIntBits(expected) == Float.floatToIntBits(actual) || Math.abs(expected - actual) <= delta;
    }

    static boolean iterableEquals(Iterable<?> expected, Iterable<?> actual) {
        if (expected == actual) return true;
        if (expected == null || actual == null) return false;
        Iterator<?> e = expected.iterator(), a = actual.iterator();
        while (e.hasNext() && a.hasNext()) {
            Object x = e.next(), y = a.next();
            if (x instanceof Iterable && y instanceof Iterable) { if (!iterableEquals((Iterable<?>) x, (Iterable<?>) y)) return false; }
            else if (!Objects.deepEquals(x, y)) return false;
        }
        return !e.hasNext() && !a.hasNext();
    }

    // a line matches when equal, or when the expected line, as a regular expression, matches it; ">> ... >>" skips lines
    static boolean linesMatch(List<String> expected, List<String> actual) {
        if (expected.size() != actual.size()) return false;
        for (int i = 0; i < expected.size(); i++) {
            String e = expected.get(i), a = actual.get(i);
            if (!e.equals(a) && !a.matches(e)) return false;
        }
        return true;
    }

    static <T extends Throwable> T throwsOf(Class<T> expectedType, Executable executable, boolean exactly, Object m) {
        try {
            executable.execute();
        } catch (Throwable t) {
            if (exactly ? t.getClass() == expectedType : expectedType.isInstance(t)) return expectedType.cast(t);
            throw new AssertionFailedError(msg(m) + "Unexpected exception type thrown, expected: <" + expectedType.getName()
                                           + "> but was: <" + t.getClass().getName() + ">", t);
        }
        throw new AssertionFailedError(msg(m) + "Expected " + expectedType.getName() + " to be thrown, but nothing was thrown.");
    }

    // the block run, then its time against the limit (the check's runner does not preempt it)
    static <T> T timed(Duration timeout, ThrowingSupplier<T> supplier, Object m) {
        long t0 = System.nanoTime();
        T v;
        try { v = supplier.get(); } catch (Throwable t) { rethrow(t); return null; }
        long ms = (System.nanoTime() - t0) / 1000000;
        if (ms > timeout.toMillis()) throw new AssertionFailedError(msg(m) + "execution exceeded timeout of " + timeout.toMillis() + " ms by " + (ms - timeout.toMillis()) + " ms");
        return v;
    }

    public static void assertAll(Executable... executables) { assertAll((String) null, Arrays.asList(executables)); }
    public static void assertAll(String heading, Executable... executables) { assertAll(heading, Arrays.asList(executables)); }
    public static void assertAll(Collection<Executable> executables) { assertAll((String) null, executables); }
    public static void assertAll(Stream<Executable> executables) { assertAll((String) null, executables.toList()); }
    public static void assertAll(String heading, Stream<Executable> executables) { assertAll(heading, executables.toList()); }
    public static void assertAll(String heading, Collection<Executable> executables) {
        List<Throwable> failures = new ArrayList<>();
        for (Executable e : executables) {
            try { e.execute(); } catch (Throwable t) { failures.add(t); }
        }
        if (!failures.isEmpty()) {
            AssertionFailedError e = new AssertionFailedError((heading == null ? "Multiple Failures" : heading) + " (" + failures.size() + " failures) " + failures);
            for (Throwable t : failures) e.addSuppressed(t);
            throw e;
        }
    }
}
