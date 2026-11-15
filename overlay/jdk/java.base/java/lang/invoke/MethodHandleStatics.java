/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F15 (java.lang.invoke)"): jrt's
 * java.lang.invoke.MethodHandleStatics, the helpers jdk26u's java.lang.invoke shares that the Go build's
 * classes use: the exceptions' factories and Unsafe, their bodies jdk26u's (https://github.com/openjdk/jdk26u
 * at baf63fb, src/java.base/share/classes/java/lang/invoke/MethodHandleStatics.java; Copyright (c) 2011, 2025,
 * Oracle and/or its affiliates, GPL 2 with the Classpath Exception: LICENSE.md). jdk26u's flags of the
 * LambdaForm compiler, its tracing and its class-file dumper are left out: the Go build has no LambdaForms.
 * The selection Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.lang.invoke;

import jdk.internal.misc.Unsafe;

/** The helpers java.lang.invoke's classes share (jdk26u's, less the LambdaForm compiler's flags). */
class MethodHandleStatics {

    private MethodHandleStatics() { }  // do not instantiate

    static final Unsafe UNSAFE = Unsafe.getUnsafe();

    /*non-public*/
    static InternalError newInternalError(String message) {
        return new InternalError(message);
    }
    /*non-public*/
    static InternalError newInternalError(String message, Exception cause) {
        return new InternalError(message, cause);
    }
    /*non-public*/
    static InternalError newInternalError(Exception cause) {
        return new InternalError(cause);
    }
    /*non-public*/
    static RuntimeException newIllegalStateException(String message) {
        return new IllegalStateException(message);
    }
    /*non-public*/
    static RuntimeException newIllegalStateException(String message, Object obj) {
        return new IllegalStateException(message(message, obj));
    }
    /*non-public*/
    static RuntimeException newIllegalArgumentException(String message) {
        return new IllegalArgumentException(message);
    }
    /*non-public*/
    static RuntimeException newIllegalArgumentException(String message, Object obj) {
        return new IllegalArgumentException(message(message, obj));
    }
    /*non-public*/
    static RuntimeException newIllegalArgumentException(String message, Object obj, Object obj2) {
        return new IllegalArgumentException(message(message, obj, obj2));
    }
    /** Propagate unchecked exceptions and errors, but wrap anything checked and throw that instead. */
    /*non-public*/
    static Error uncaughtException(Throwable ex) {
        if (ex instanceof Error error) throw error;
        if (ex instanceof RuntimeException re) throw re;
        throw new InternalError("uncaught exception", ex);
    }
    private static String message(String message, Object obj) {
        if (obj != null)  message = message + ": " + obj;
        return message;
    }
    private static String message(String message, Object obj, Object obj2) {
        if (obj != null || obj2 != null)  message = message + ": " + obj + ", " + obj2;
        return message;
    }
}
