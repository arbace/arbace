/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F16, "As built: F16 (write-ahead)"): the
 * operations jrt's java.util.concurrent.atomic.Atomic{Integer,Long,Reference}FieldUpdater (overlay,
 * jdk26u's with this class in place of jdk.internal.misc.Unsafe's field offsets) perform on a
 * reflected field. The Go build has no field offsets for reflection to hand Unsafe (a class's
 * fields are Go struct fields, which jrt's java.lang.reflect.Field reads and writes by name), so
 * an updater keeps its Field and every operation goes through Field.get and Field.set under a lock
 * of the receiver and the field: the updaters of one field of one object exclude each other (a
 * compare-and-set and an increment are atomic with respect to each other, as the JDK guarantees
 * them, "only with respect to other invocations of compareAndSet and set on the same updater"),
 * the lock's monitor gives the volatile reads and writes their ordering, and the locks are striped
 * (64 objects, by the receiver's identity hash code and the field's name) so that no lock is
 * allocated per object.
 *
 * Access: jdk26u checks the field against the caller's class (Reflection.getCallerClass, which jrt
 * has not). Without the caller, a field that is not public is accessible unless its class is the
 * platform's (java., javax., jdk., sun., com.sun.): a program's updaters are on its own classes,
 * and a test's private field of the platform's (AtomicInteger.value) is refused as the JDK refuses
 * it to a caller outside the package. A protected field's receivers are not narrowed to the
 * caller's class.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Field updaters' operations over reflection (Unsafe's offsets in the JDK). */
public final class FieldUpdaters {

    private static final Object[] LOCKS = new Object[64];
    static {
        for (int i = 0; i < LOCKS.length; i++) {
            LOCKS[i] = new Object();
        }
    }

    private FieldUpdaters() {
    }

    private static Object lock(Field f, Object obj) {
        return LOCKS[(System.identityHashCode(obj) * 31 + f.getName().hashCode()) & (LOCKS.length - 1)];
    }

    /**
     * jdk26u's sun.reflect.misc.ReflectUtil.ensureMemberAccess(caller, tclass, null, modifiers)
     * without the caller (above): throws IllegalAccessException for a field of the platform's
     * that is not public.
     */
    public static void ensureMemberAccess(Class<?> tclass, int modifiers) throws IllegalAccessException {
        if (Modifier.isPublic(modifiers) && Modifier.isPublic(tclass.getModifiers())) {
            return;
        }
        String n = tclass.getName();
        if (n.startsWith("java.") || n.startsWith("javax.") || n.startsWith("jdk.")
                || n.startsWith("sun.") || n.startsWith("com.sun.")) {
            throw new IllegalAccessException("class java.util.concurrent.atomic cannot access a member of "
                    + tclass + " with modifiers \"" + Modifier.toString(modifiers) + "\"");
        }
    }

    /** The field, made accessible for the updater's reads and writes. */
    public static Field accessible(Field f) {
        f.setAccessible(true);
        return f;
    }

    private static Object get(Field f, Object obj) {
        try {
            return f.get(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void set(Field f, Object obj, Object v) {
        try {
            f.set(obj, v);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    // int

    public static boolean compareAndSetInt(Field f, Object obj, int expect, int update) {
        synchronized (lock(f, obj)) {
            if ((Integer) get(f, obj) != expect) {
                return false;
            }
            set(f, obj, update);
            return true;
        }
    }

    public static int getIntVolatile(Field f, Object obj) {
        synchronized (lock(f, obj)) {
            return (Integer) get(f, obj);
        }
    }

    public static void putIntVolatile(Field f, Object obj, int v) {
        synchronized (lock(f, obj)) {
            set(f, obj, v);
        }
    }

    public static void putIntRelease(Field f, Object obj, int v) {
        putIntVolatile(f, obj, v);
    }

    public static int getAndSetInt(Field f, Object obj, int v) {
        synchronized (lock(f, obj)) {
            int old = (Integer) get(f, obj);
            set(f, obj, v);
            return old;
        }
    }

    public static int getAndAddInt(Field f, Object obj, int delta) {
        synchronized (lock(f, obj)) {
            int old = (Integer) get(f, obj);
            set(f, obj, old + delta);
            return old;
        }
    }

    // long

    public static boolean compareAndSetLong(Field f, Object obj, long expect, long update) {
        synchronized (lock(f, obj)) {
            if ((Long) get(f, obj) != expect) {
                return false;
            }
            set(f, obj, update);
            return true;
        }
    }

    public static long getLongVolatile(Field f, Object obj) {
        synchronized (lock(f, obj)) {
            return (Long) get(f, obj);
        }
    }

    public static void putLongVolatile(Field f, Object obj, long v) {
        synchronized (lock(f, obj)) {
            set(f, obj, v);
        }
    }

    public static void putLongRelease(Field f, Object obj, long v) {
        putLongVolatile(f, obj, v);
    }

    public static long getAndSetLong(Field f, Object obj, long v) {
        synchronized (lock(f, obj)) {
            long old = (Long) get(f, obj);
            set(f, obj, v);
            return old;
        }
    }

    public static long getAndAddLong(Field f, Object obj, long delta) {
        synchronized (lock(f, obj)) {
            long old = (Long) get(f, obj);
            set(f, obj, old + delta);
            return old;
        }
    }

    // reference

    public static boolean compareAndSetReference(Field f, Object obj, Object expect, Object update) {
        synchronized (lock(f, obj)) {
            if (get(f, obj) != expect) {
                return false;
            }
            set(f, obj, update);
            return true;
        }
    }

    public static Object getReferenceVolatile(Field f, Object obj) {
        synchronized (lock(f, obj)) {
            return get(f, obj);
        }
    }

    public static void putReferenceVolatile(Field f, Object obj, Object v) {
        synchronized (lock(f, obj)) {
            set(f, obj, v);
        }
    }

    public static void putReferenceRelease(Field f, Object obj, Object v) {
        putReferenceVolatile(f, obj, v);
    }

    public static Object getAndSetReference(Field f, Object obj, Object v) {
        synchronized (lock(f, obj)) {
            Object old = get(f, obj);
            set(f, obj, v);
            return old;
        }
    }
}
