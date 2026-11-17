/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F15 (java.lang.invoke)"): jrt's
 * java.lang.invoke.ConstantCallSite over jrt's handles. jdk26u's (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/java/lang/invoke/ConstantCallSite.java) keeps its target in a field the JVM links
 * invokedynamic through; here the target is a volatile field and the dynamic invoker a handle that reads it
 * at each invocation (jdk26u's makeDynamicInvoker: exactInvoker folded with getTarget). The checks and their
 * messages are jdk26u's (Copyright (c) 2008, 2025, Oracle and/or its affiliates, GPL 2 with the Classpath
 * Exception: LICENSE.md). Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.lang.invoke;

/** A call site whose target is permanent, and can never be changed. */
public class ConstantCallSite extends CallSite {
    private final boolean isFrozen;

    /** Creates a call site with a permanent target. */
    public ConstantCallSite(MethodHandle target) {
        super(target);
        isFrozen = true;
    }

    /** Creates a call site with a permanent target, possibly bound to the call site itself. */
    protected ConstantCallSite(MethodType targetType, MethodHandle createTargetHook) throws Throwable {
        super(targetType, createTargetHook); // "this" instance leaks into createTargetHook
        isFrozen = true;
    }

    /** Returns the target method of the call site, which behaves like a final field of the ConstantCallSite. */
    @Override public final MethodHandle getTarget() {
        if (!isFrozen)  throw new IllegalStateException();
        return target;
    }

    /** Always throws an UnsupportedOperationException. */
    @Override public final void setTarget(MethodHandle ignore) {
        throw new UnsupportedOperationException();
    }

    /** Returns this call site's permanent target. */
    @Override
    public final MethodHandle dynamicInvoker() {
        return getTarget();
    }
}
