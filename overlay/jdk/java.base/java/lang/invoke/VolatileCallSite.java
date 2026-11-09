/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F15 (java.lang.invoke)"): jrt's
 * java.lang.invoke.VolatileCallSite over jrt's handles. jdk26u's (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/java/lang/invoke/VolatileCallSite.java) keeps its target in a field the JVM links
 * invokedynamic through; here the target is a volatile field and the dynamic invoker a handle that reads it
 * at each invocation (jdk26u's makeDynamicInvoker: exactInvoker folded with getTarget). The checks and their
 * messages are jdk26u's (Copyright (c) 2008, 2025, Oracle and/or its affiliates, GPL 2 with the Classpath
 * Exception: LICENSE.md). Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.lang.invoke;

/** A call site whose target variable behaves like a volatile variable. */
public class VolatileCallSite extends CallSite {
    /** Creates a call site with a volatile binding to its target. */
    public VolatileCallSite(MethodType type) {
        super(type);
    }

    /** Creates a call site with a volatile binding to its target. */
    public VolatileCallSite(MethodHandle target) {
        super(target);
    }

    /** Returns the target method of the call site, which behaves like a volatile field. */
    @Override public final MethodHandle getTarget() {
        return getTargetVolatile();
    }

    /** Updates the target method of this call site, as a volatile variable. */
    @Override public void setTarget(MethodHandle newTarget) {
        setTargetVolatile(newTarget);
    }

    /** Produces a method handle equivalent to an invokedynamic instruction linked to this call site. */
    @Override
    public final MethodHandle dynamicInvoker() {
        return makeDynamicInvoker();
    }
}
