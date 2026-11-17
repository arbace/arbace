/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F15 (java.lang.invoke)"): the
 * call sites of java.lang.invoke in the Go build. The JVM links a signature polymorphic call
 * (MethodHandle.invokeExact and invoke, JVMS 2.9.3) to the handle's code by the call site's
 * descriptor, and MethodHandles.lookup() to its caller's class. The Go build has neither linker nor
 * caller frames, so the Java forms' analyzer (arbace.classes.analyze, SPEC section 5.6: a polymorphic
 * call takes its descriptor from the call) writes such a call as a call of this class, which serves
 * the interpreter and j2g alike:
 *
 *   (int) mh.invokeExact(a, b)  ->  Invokers.invokeExactI(mh, int.class, new Class<?>[] {A, B},
 *                                                         new Object[] {a, b})
 *   MethodHandles.lookup()      ->  Invokers.lookup(C.class)          (C: the class of the code)
 *
 * One method per kind of return type (V Z B C S I J F D, and L: the analyzer casts its result to
 * the call's type, as javac's checkcast after the call): the arguments boxed as the call site's
 * types box them (an int as an Integer), the site's type made of its classes, the handle invoked
 * exactly (its type equal to the site's, else WrongMethodTypeException) or after asType to the
 * site's type (invoke). java.lang.invoke registers its side (the Access below) in MethodHandles'
 * static initializer, as jdk.internal.access.SharedSecrets' accessors are registered.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/** The Go build's call sites of MethodHandle.invokeExact, invoke and MethodHandles.lookup(). */
public final class Invokers {
    private Invokers() { }

    /** java.lang.invoke's side: a lookup of a class with full privilege, a handle's invocations. */
    public interface Access {
        MethodHandles.Lookup lookup(Class<?> caller);
        Object invokeExact(MethodHandle mh, MethodType site, Object[] args) throws Throwable;
        Object invoke(MethodHandle mh, MethodType site, Object[] args) throws Throwable;
    }

    private static volatile Access access;

    /** Called once, by MethodHandles' static initializer. */
    public static void setAccess(Access a) {
        access = a;
    }

    private static Access access() {
        Access a = access;
        if (a == null) {
            // MethodHandles' static initializer registers it
            MethodHandles.publicLookup();
            a = access;
        }
        return a;
    }

    /** MethodHandles.lookup() in code of class caller. */
    public static MethodHandles.Lookup lookup(Class<?> caller) {
        return access().lookup(caller);
    }

    private static MethodType site(Class<?> rtype, Class<?>[] ptypes) {
        return MethodType.methodType(rtype, ptypes);
    }

    // ---------------------------------------------------------------------------------------
    // invokeExact, by the call's return type

    public static void invokeExactV(MethodHandle mh, Class<?> r, Class<?>[] p, Object[] a) throws Throwable {
        access().invokeExact(mh, site(r, p), a);
    }
    public static boolean invokeExactZ(MethodHandle mh, Class<?> r, Class<?>[] p, Object[] a) throws Throwable {
        return (Boolean) access().invokeExact(mh, site(r, p), a);
    }
    public static byte invokeExactB(MethodHandle mh, Class<?> r, Class<?>[] p, Object[] a) throws Throwable {
        return (Byte) access().invokeExact(mh, site(r, p), a);
    }
    public static char invokeExactC(MethodHandle mh, Class<?> r, Class<?>[] p, Object[] a) throws Throwable {
        return (Character) access().invokeExact(mh, site(r, p), a);
    }
    public static short invokeExactS(MethodHandle mh, Class<?> r, Class<?>[] p, Object[] a) throws Throwable {
        return (Short) access().invokeExact(mh, site(r, p), a);
    }
    public static int invokeExactI(MethodHandle mh, Class<?> r, Class<?>[] p, Object[] a) throws Throwable {
        return (Integer) access().invokeExact(mh, site(r, p), a);
    }
    public static long invokeExactJ(MethodHandle mh, Class<?> r, Class<?>[] p, Object[] a) throws Throwable {
        return (Long) access().invokeExact(mh, site(r, p), a);
    }
    public static float invokeExactF(MethodHandle mh, Class<?> r, Class<?>[] p, Object[] a) throws Throwable {
        return (Float) access().invokeExact(mh, site(r, p), a);
    }
    public static double invokeExactD(MethodHandle mh, Class<?> r, Class<?>[] p, Object[] a) throws Throwable {
        return (Double) access().invokeExact(mh, site(r, p), a);
    }
    public static Object invokeExactL(MethodHandle mh, Class<?> r, Class<?>[] p, Object[] a) throws Throwable {
        return access().invokeExact(mh, site(r, p), a);
    }

    // ---------------------------------------------------------------------------------------
    // invoke, by the call's return type

    public static void invokeV(MethodHandle mh, Class<?> r, Class<?>[] p, Object[] a) throws Throwable {
        access().invoke(mh, site(r, p), a);
    }
    public static boolean invokeZ(MethodHandle mh, Class<?> r, Class<?>[] p, Object[] a) throws Throwable {
        return (Boolean) access().invoke(mh, site(r, p), a);
    }
    public static byte invokeB(MethodHandle mh, Class<?> r, Class<?>[] p, Object[] a) throws Throwable {
        return (Byte) access().invoke(mh, site(r, p), a);
    }
    public static char invokeC(MethodHandle mh, Class<?> r, Class<?>[] p, Object[] a) throws Throwable {
        return (Character) access().invoke(mh, site(r, p), a);
    }
    public static short invokeS(MethodHandle mh, Class<?> r, Class<?>[] p, Object[] a) throws Throwable {
        return (Short) access().invoke(mh, site(r, p), a);
    }
    public static int invokeI(MethodHandle mh, Class<?> r, Class<?>[] p, Object[] a) throws Throwable {
        return (Integer) access().invoke(mh, site(r, p), a);
    }
    public static long invokeJ(MethodHandle mh, Class<?> r, Class<?>[] p, Object[] a) throws Throwable {
        return (Long) access().invoke(mh, site(r, p), a);
    }
    public static float invokeF(MethodHandle mh, Class<?> r, Class<?>[] p, Object[] a) throws Throwable {
        return (Float) access().invoke(mh, site(r, p), a);
    }
    public static double invokeD(MethodHandle mh, Class<?> r, Class<?>[] p, Object[] a) throws Throwable {
        return (Double) access().invoke(mh, site(r, p), a);
    }
    public static Object invokeL(MethodHandle mh, Class<?> r, Class<?>[] p, Object[] a) throws Throwable {
        return access().invoke(mh, site(r, p), a);
    }
}
