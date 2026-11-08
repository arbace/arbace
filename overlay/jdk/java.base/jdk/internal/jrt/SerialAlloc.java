/*
 * Arbace (CDR10, doc/CLOJURE-DESIGN-REVIEW.md, the user's decision of 2026-11-06; JCS167 (a), the user's decision of
 * 2026-11-07, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS167 (write-ahead)"; doc/go/JRT-NOTES.md,
 * "Serialization"): the allocation of a deserialized object, what
 * jdk.internal.reflect.ReflectionFactory.newConstructorForSerialization's constructor does on a JVM: an
 * object of class cl made by the no-argument constructor of cl's first superclass that is not
 * Serializable, cl's own constructors and those of its serializable superclasses not run, or none when
 * that constructor is not accessible (ReflectionFactory's rule, superHasAccessibleConstructor's included).
 * Its natives are jrt's Go (go/arbace/jrt/serial.ae): the class's ClassInfo.SerialNew, which j2g writes for
 * each instantiated Serializable class of the world the rule allows (arbace.j2g.model/serial-super), and
 * the Java forms' host sets for an interpreted class (Compiler$CF$Rt.serialPlan, the rule over the
 * interpreter's classes, then the nearest class of the world's part: j2g's cfSerialFor).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.io.InvalidClassException;

/** The allocation of objects for deserialization (java.io.ObjectStreamClass). */
public final class SerialAlloc {

    private SerialAlloc() {
    }

    /** Whether cl's objects can be made for deserialization. */
    public static boolean canAllocate(Class<?> cl) {
        return canAllocate0(cl);
    }

    /** A new object of cl, made as newConstructorForSerialization's constructor makes it. */
    public static Object allocate(Class<?> cl) throws InvalidClassException {
        if (!canAllocate0(cl)) {
            throw new InvalidClassException(cl.getName(), "no valid constructor");
        }
        return allocate0(cl);
    }

    private static native boolean canAllocate0(Class<?> cl);

    private static native Object allocate0(Class<?> cl);
}
