/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F15 (java.lang.invoke)"): jrt's
 * java.lang.invoke.CallSite over jrt's handles. jdk26u's (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/java/lang/invoke/CallSite.java) keeps its target in a field the JVM links
 * invokedynamic through; here the target is a volatile field and the dynamic invoker a handle that reads it
 * at each invocation (jdk26u's makeDynamicInvoker: exactInvoker folded with getTarget). The checks and their
 * messages are jdk26u's (Copyright (c) 2008, 2025, Oracle and/or its affiliates, GPL 2 with the Classpath
 * Exception: LICENSE.md). Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.lang.invoke;

import static java.lang.invoke.MethodHandleStatics.*;

/** A holder for a variable method handle, the target of a dynamic call site. */
public abstract class CallSite {

    /*package-private*/
    volatile MethodHandle target;

    /*package-private*/
    CallSite(MethodType type) {
        target = makeUninitializedCallSite(type);
    }

    /*package-private*/
    CallSite(MethodHandle target) {
        target.type();  // null check
        this.target = target;
    }

    /*package-private*/
    CallSite(MethodType targetType, MethodHandle createTargetHook) throws Throwable {
        this(targetType); // need to initialize target to make CallSite.type() work in createTargetHook
        ConstantCallSite selfCCS = (ConstantCallSite) this;
        MethodHandle boundTarget = (MethodHandle) createTargetHook.invokeWithArguments(selfCCS);
        setTargetNormal(boundTarget); // ConstantCallSite doesn't publish CallSite.target
    }

    /** Returns the type of this call site's target. */
    public MethodType type() {
        return target.type();
    }

    /** Returns the target method of the call site. */
    public abstract MethodHandle getTarget();

    /** Updates the target method of this call site. */
    public abstract void setTarget(MethodHandle newTarget);

    private void checkTargetChange(MethodHandle newTarget) {
        MethodType oldType = target.type(); // target is always present
        MethodType newType = newTarget.type();  // null check!
        if (newType != oldType && !newType.equals(oldType)) {
            throw wrongTargetType(newTarget, oldType);
        }
    }

    private static WrongMethodTypeException wrongTargetType(MethodHandle target, MethodType type) {
        return new WrongMethodTypeException(String.valueOf(target)+" should be of type "+type);
    }

    /** Produces a method handle equivalent to an invokedynamic instruction which has been linked to this call site. */
    public abstract MethodHandle dynamicInvoker();

    /*package-private*/
    MethodHandle makeDynamicInvoker() {
        CallSite site = this;
        return MethodHandleImpl.adapter(type(), a -> site.getTarget().invokeExactFrom(site.type(), a));
    }

    private MethodHandle makeUninitializedCallSite(MethodType targetType) {
        return MethodHandleImpl.adapter(targetType, a -> {
            throw new IllegalStateException("uninitialized call site");
        });
    }

    /*package-private*/
    final void setTargetNormal(MethodHandle newTarget) {
        checkTargetChange(newTarget);
        target = newTarget;
    }

    /*package-private*/
    final MethodHandle getTargetVolatile() {
        return target;
    }

    /*package-private*/
    final void setTargetVolatile(MethodHandle newTarget) {
        checkTargetChange(newTarget);
        target = newTarget;
    }
}
