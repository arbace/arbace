/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F15 (java.lang.invoke)"): jrt's
 * java.lang.invoke.MutableCallSite over jrt's handles. jdk26u's (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/java/lang/invoke/MutableCallSite.java) keeps its target in a field the JVM links
 * invokedynamic through; here the target is a volatile field and the dynamic invoker a handle that reads it
 * at each invocation (jdk26u's makeDynamicInvoker: exactInvoker folded with getTarget). The checks and their
 * messages are jdk26u's (Copyright (c) 2008, 2025, Oracle and/or its affiliates, GPL 2 with the Classpath
 * Exception: LICENSE.md). Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.lang.invoke;

import java.util.Objects;

/** A call site whose target variable behaves like an ordinary field. */
public class MutableCallSite extends CallSite {
    /** Creates a blank call site object with the given method type. */
    public MutableCallSite(MethodType type) {
        super(type);
    }

    /** Creates a call site object with an initial target method handle. */
    public MutableCallSite(MethodHandle target) {
        super(target);
    }

    /** Returns the target method of the call site. */
    @Override public final MethodHandle getTarget() {
        return target;
    }

    /** Updates the target method of this call site, as a normal variable. */
    @Override public void setTarget(MethodHandle newTarget) {
        setTargetNormal(newTarget);
    }

    /** Produces a method handle equivalent to an invokedynamic instruction linked to this call site. */
    @Override
    public final MethodHandle dynamicInvoker() {
        return makeDynamicInvoker();
    }

    /**
     * Performs a synchronization operation on each call site in the given array. jrt: the target is a volatile
     * field, so every thread sees its latest value already.
     */
    public static void syncAll(MutableCallSite[] sites) {
        if (sites.length == 0)  return;
        for (MutableCallSite site : sites) {
            Objects.requireNonNull(site); // trigger NPE on first null
        }
    }
}
