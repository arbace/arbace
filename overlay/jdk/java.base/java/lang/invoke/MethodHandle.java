/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F15 (java.lang.invoke)"): jrt's
 * java.lang.invoke.MethodHandle. jdk26u's (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/java/lang/invoke/MethodHandle.java) is a LambdaForm interpreted or spun into
 * bytecode by the VM; the Go build has neither, so a handle here is an object whose invokeImpl takes its
 * arguments as an Object[] (each boxed as its parameter type boxes it: an int as an Integer) and returns its
 * result boxed (null for void): a direct handle over java.lang.reflect (DirectMethodHandle), an adapter
 * (MethodHandleImpl). The public API is jdk26u's, rule for rule where its text says how: asType's conversions
 * (MethodHandleImpl.convert, MethodType.isConvertibleTo, jdk26u's), the varargs collectors' asType
 * (AsVarargsCollector.asTypeUncached), asSpreader's and asCollector's checks (asSpreaderChecks,
 * asCollectorChecks), bindTo's leading reference parameter, toString's "MethodHandle"+type.
 *
 * invokeExact and invoke stay native and signature polymorphic (JVMS 2.9.3: a final native varargs method of
 * Object[]), so that the analyzer of Java forms recognizes their calls and writes them as calls of
 * jdk.internal.jrt.Invokers (its header); their natives (jrt's natives.ae) are only reached by reflection, and
 * throw as the JDK's Method.invoke of them does.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.lang.invoke;

import java.lang.constant.Constable;
import java.lang.constant.MethodHandleDesc;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.lang.invoke.MethodHandleStatics.*;

/** A typed, directly executable reference to a method, constructor, field or adapted operation. */
public abstract class MethodHandle implements Constable {

    private final MethodType type;

    /*non-public*/
    MethodHandle(MethodType type) {
        this.type = Objects.requireNonNull(type);
    }

    /** Reports the type of this method handle. */
    public MethodType type() {
        return type;
    }

    /** Invokes the method handle, allowing any caller type descriptor, but requiring an exact type match. */
    public final native Object invokeExact(Object... args) throws Throwable;

    /** Invokes the method handle, allowing any caller type descriptor, and optionally performing conversions. */
    public final native Object invoke(Object... args) throws Throwable;

    /**
     * jrt: the invocation itself. args are this handle's arguments, args.length == type().parameterCount(),
     * each of its parameter's type (a primitive boxed); the result is of the return type, boxed, null for void.
     */
    /*non-public*/
    abstract Object invokeImpl(Object[] args) throws Throwable;

    /** jrt: invokeExact from a call site of type site (jdk.internal.jrt.Invokers). */
    /*non-public*/
    final Object invokeExactFrom(MethodType site, Object[] args) throws Throwable {
        if (!type.equals(site))
            throw new WrongMethodTypeException("handle's method type " + type + " but found " + site);
        return invokeImpl(args);
    }

    /** jrt: invoke from a call site of type site (jdk.internal.jrt.Invokers). */
    /*non-public*/
    final Object invokeFrom(MethodType site, Object[] args) throws Throwable {
        return asType(site).invokeImpl(args);
    }

    /** Performs a variable arity invocation, as if by invoke with the arguments given as an array. */
    public Object invokeWithArguments(Object... arguments) throws Throwable {
        // jdk26u: asType(genericMethodType(N)) then a spread invocation of the arguments
        int n = (arguments == null) ? 0 : arguments.length;
        MethodType invocationType = MethodType.genericMethodType(n);
        Object[] args = (n == 0) ? new Object[0] : arguments.clone();
        return asType(invocationType).invokeImpl(args);
    }

    /** Performs a variable arity invocation, the arguments given as a list. */
    public Object invokeWithArguments(List<?> arguments) throws Throwable {
        return invokeWithArguments(arguments.toArray());
    }

    /** Produces an adapter method handle which adapts the type of the current method handle to a new type. */
    public final MethodHandle asType(MethodType newType) {
        // Fast path alternative to a complete asType state machine:
        if (newType == type) {
            return this;
        }
        if (newType.equals(type)) {
            return this;
        }
        return asTypeUncached(newType);
    }

    /*non-public*/
    MethodHandle asTypeUncached(MethodType newType) {
        if (!type.isConvertibleTo(newType))
            throw new WrongMethodTypeException("cannot convert " + this + " to " + newType);
        return MethodHandleImpl.makePairwiseConvert(this, newType, false);
    }

    /** Makes an array-spreading method handle, which accepts an array argument at the end. */
    public MethodHandle asSpreader(Class<?> arrayType, int arrayLength) {
        return asSpreader(type().parameterCount() - arrayLength, arrayType, arrayLength);
    }

    /** Makes an array-spreading method handle, which accepts an array argument at a given position. */
    public MethodHandle asSpreader(int spreadArgPos, Class<?> arrayType, int arrayLength) {
        MethodType postSpreadType = asSpreaderChecks(arrayType, spreadArgPos, arrayLength);
        MethodHandle afterSpread = this.asType(postSpreadType);
        return MethodHandleImpl.makeSpreadArguments(afterSpread, arrayType, spreadArgPos, arrayLength);
    }

    /** See if {@code asSpreader} can be validly called with the given arguments (jdk26u's). */
    private MethodType asSpreaderChecks(Class<?> arrayType, int pos, int arrayLength) {
        spreadArrayChecks(arrayType, arrayLength);
        int nargs = type().parameterCount();
        if (nargs < arrayLength || arrayLength < 0)
            throw newIllegalArgumentException("bad spread array length");
        if (pos < 0 || pos + arrayLength > nargs) {
            throw newIllegalArgumentException("bad spread position");
        }
        Class<?> arrayElement = arrayType.getComponentType();
        MethodType mtype = type();
        boolean match = true, fail = false;
        for (int i = pos; i < pos + arrayLength; i++) {
            Class<?> ptype = mtype.parameterType(i);
            if (ptype != arrayElement) {
                match = false;
                if (!MethodType.canConvert(arrayElement, ptype)) {
                    fail = true;
                    break;
                }
            }
        }
        if (match)  return mtype;
        MethodType needType = mtype.asSpreaderType(arrayType, pos, arrayLength);
        if (!fail)  return needType;
        // elicit an error:
        this.asType(needType);
        throw newInternalError("should not return");
    }

    private void spreadArrayChecks(Class<?> arrayType, int arrayLength) {
        Class<?> arrayElement = arrayType.getComponentType();
        if (arrayElement == null)
            throw newIllegalArgumentException("not an array type", arrayType);
        if ((arrayLength & 0x7F) != arrayLength) {
            if ((arrayLength & 0xFF) != arrayLength)
                throw newIllegalArgumentException("array length is not legal", arrayLength);
            assert(arrayLength >= 128);
            if (arrayElement == long.class ||
                arrayElement == double.class)
                throw newIllegalArgumentException("array length is not legal for long[] or double[]", arrayLength);
        }
    }

    /** Adapts this method handle to be variable arity if the boolean flag is true, else fixed arity. */
    public MethodHandle withVarargs(boolean makeVarargs) {
        if (makeVarargs == isVarargsCollector()) {
            return this;
        } else if (makeVarargs) {
            return asVarargsCollector(type().lastParameterType());
        } else {
            return asFixedArity();
        }
    }

    /** Makes an array-collecting method handle, which accepts a given number of trailing positional arguments. */
    public MethodHandle asCollector(Class<?> arrayType, int arrayLength) {
        return asCollector(type().parameterCount() - 1, arrayType, arrayLength);
    }

    /** Makes an array-collecting method handle, which accepts a given number of positional arguments at a position. */
    public MethodHandle asCollector(int collectArgPos, Class<?> arrayType, int arrayLength) {
        asCollectorChecks(arrayType, collectArgPos, arrayLength);
        return MethodHandleImpl.makeCollectArguments(this, arrayType, collectArgPos, arrayLength);
    }

    /** See if {@code asCollector} can be validly called with the given arguments (jdk26u's). */
    /*non-public*/
    boolean asCollectorChecks(Class<?> arrayType, int pos, int arrayLength) {
        spreadArrayChecks(arrayType, arrayLength);
        int nargs = type().parameterCount();
        if (pos < 0 || pos >= nargs) {
            throw newIllegalArgumentException("bad collect position");
        }
        if (nargs != 0) {
            Class<?> param = type().parameterType(pos);
            if (param == arrayType)  return true;
            if (param.isAssignableFrom(arrayType))  return false;
        }
        throw newIllegalArgumentException("array type not assignable to argument", this, arrayType);
    }

    /** Makes an array-collecting method handle, which accepts any number of trailing positional arguments. */
    public MethodHandle asVarargsCollector(Class<?> arrayType) {
        Objects.requireNonNull(arrayType);
        boolean lastMatch = asCollectorChecks(arrayType, type().parameterCount() - 1, 0);
        if (isVarargsCollector() && lastMatch)
            return this;
        return MethodHandleImpl.makeVarargsCollector(this, arrayType);
    }

    /** Determines if this method handle supports variable arity calls. */
    public boolean isVarargsCollector() {
        return false;
    }

    /** Makes a fixed arity method handle which is otherwise equivalent to the current method handle. */
    public MethodHandle asFixedArity() {
        assert(!isVarargsCollector());
        return this;
    }

    /** Binds a value x to the first argument of a method handle, without invoking it. */
    public MethodHandle bindTo(Object x) {
        x = bindArgumentChecks(0, x);
        return MethodHandleImpl.makeBound(this, x);
    }

    private Object bindArgumentChecks(int pos, Object value) {
        // jdk26u's bindArgumentL's precondition: a leading reference parameter, the value cast to it
        Class<?> ptype = type.parameterCount() > pos ? type.parameterType(pos) : null;
        if (ptype == null || ptype.isPrimitive())
            throw newIllegalArgumentException("no leading reference parameter", value);
        return ptype.cast(value);
    }

    /** Returns a nominal descriptor for this instance, if one can be constructed (none here, as for a non-direct handle). */
    @Override
    public Optional<MethodHandleDesc> describeConstable() {
        return Optional.empty();
    }

    /** Returns a string representation of the method handle: "MethodHandle" and its type. */
    @Override
    public String toString() {
        return standardString();
    }

    /*non-public*/
    String standardString() {
        return "MethodHandle" + type;
    }
}
