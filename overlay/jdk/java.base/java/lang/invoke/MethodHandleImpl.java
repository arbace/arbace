/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F15 (java.lang.invoke)"): jrt's
 * java.lang.invoke.MethodHandleImpl, the adapters of jrt's handles (MethodHandle's header). Where jdk26u
 * (https://github.com/openjdk/jdk26u at baf63fb, src/java.base/share/classes/java/lang/invoke/
 * MethodHandleImpl.java, BoundMethodHandle.java, ValueConversions in sun/invoke/util) builds a LambdaForm,
 * an adapter here is a handle of the new type whose invokeImpl computes the target's arguments from its own
 * and invokes the target. The value conversions are jdk26u's rules, as MethodHandle.asType's and
 * MethodHandles.explicitCastArguments' documentation states them, on boxed values: a reference cast (to an
 * interface too under asType, keepInterfaces), a primitive widened (asType) or cast (explicit, a boolean as
 * its low bit), a primitive boxed then cast, a reference unboxed by its dynamic wrapper then widened (asType:
 * ValueConversions.unbox*'s ClassCastException "Cannot cast X to Y" otherwise; a null a NullPointerException,
 * explicit: a zero), void to a zero or null, any value dropped to void.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.lang.invoke;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;

import static java.lang.invoke.MethodHandleStatics.*;

/** The adapters of jrt's method handles. */
final class MethodHandleImpl {

    private MethodHandleImpl() { }

    /** An adapter's invocation: its arguments (of its type) to its result (of its return type). */
    interface Body {
        Object apply(Object[] a) throws Throwable;
    }

    /** A handle of type t computed by b. */
    static final class Adapter extends MethodHandle {
        private final Body body;
        Adapter(MethodType t, Body body) {
            super(t);
            this.body = body;
        }
        @Override
        Object invokeImpl(Object[] args) throws Throwable {
            return body.apply(args);
        }
    }

    static MethodHandle adapter(MethodType t, Body b) {
        return new Adapter(t, b);
    }

    // ---------------------------------------------------------------------------------------
    // value conversions

    /** The zero of type t, boxed (false, 0, null; null for void). */
    static Object zero(Class<?> t) {
        if (!t.isPrimitive() || t == void.class) return null;
        if (t == boolean.class) return Boolean.FALSE;
        if (t == char.class) return Character.valueOf((char) 0);
        return box(t, 0L, 0.0, false);
    }

    /** The primitive type whose wrapper is c, or null. */
    static Class<?> unwrapped(Class<?> c) {
        if (c == Integer.class) return int.class;
        if (c == Long.class) return long.class;
        if (c == Boolean.class) return boolean.class;
        if (c == Character.class) return char.class;
        if (c == Byte.class) return byte.class;
        if (c == Short.class) return short.class;
        if (c == Float.class) return float.class;
        if (c == Double.class) return double.class;
        if (c == Void.class) return void.class;
        return null;
    }

    /** The wrapper of primitive type p. */
    static Class<?> wrapper(Class<?> p) {
        if (p == int.class) return Integer.class;
        if (p == long.class) return Long.class;
        if (p == boolean.class) return Boolean.class;
        if (p == char.class) return Character.class;
        if (p == byte.class) return Byte.class;
        if (p == short.class) return Short.class;
        if (p == float.class) return Float.class;
        if (p == double.class) return Double.class;
        if (p == void.class) return Void.class;
        return p;
    }

    /** JLS 5.1.2: does primitive src widen (or stay) to primitive dst? */
    static boolean widens(Class<?> src, Class<?> dst) {
        if (src == dst) return true;
        if (src == boolean.class || dst == boolean.class) return false;
        if (dst == char.class) return false;
        if (src == char.class) return dst == int.class || dst == long.class || dst == float.class || dst == double.class;
        int s = rank(src), d = rank(dst);
        if (src == byte.class && dst == short.class) return true;
        if (dst == short.class || dst == byte.class) return false;
        return s < d;
    }

    private static int rank(Class<?> p) {
        if (p == byte.class) return 0;
        if (p == short.class) return 1;
        if (p == int.class) return 2;
        if (p == long.class) return 3;
        if (p == float.class) return 4;
        if (p == double.class) return 5;
        return -1;
    }

    /** A box of primitive type t of the value l (integral) or d (floating, when fp). */
    private static Object box(Class<?> t, long l, double d, boolean fp) {
        if (t == int.class) return Integer.valueOf(fp ? (int) d : (int) l);
        if (t == long.class) return Long.valueOf(fp ? (long) d : l);
        if (t == byte.class) return Byte.valueOf(fp ? (byte) (int) d : (byte) l);
        if (t == short.class) return Short.valueOf(fp ? (short) (int) d : (short) l);
        if (t == char.class) return Character.valueOf(fp ? (char) (int) d : (char) l);
        if (t == float.class) return Float.valueOf(fp ? (float) d : (float) l);
        if (t == double.class) return Double.valueOf(fp ? d : (double) l);
        if (t == boolean.class) return Boolean.valueOf(fp ? ((int) d & 1) != 0 : (l & 1) != 0);
        throw newInternalError("not a primitive: " + t);
    }

    /** Box v (of primitive type src, boxed) converted to primitive dst (a widening, or any cast when explicit). */
    private static Object primitive(Object v, Class<?> src, Class<?> dst) {
        if (src == dst) return v;
        if (v instanceof Boolean b) {
            long l = b ? 1 : 0;
            return box(dst, l, 0, false);
        }
        if (dst == boolean.class) {
            // explicitCastArguments: the low-order bit (a floating value through int, as jdk26u's
            // ValueConversions do)
            if (v instanceof Character c) return Boolean.valueOf((c & 1) != 0);
            Number n = (Number) v;
            if (v instanceof Float || v instanceof Double) return Boolean.valueOf(((int) n.doubleValue() & 1) != 0);
            return Boolean.valueOf((n.longValue() & 1) != 0);
        }
        if (v instanceof Character c) return box(dst, c.charValue(), 0, false);
        Number n = (Number) v;
        if (v instanceof Float || v instanceof Double) return box(dst, 0, n.doubleValue(), true);
        return box(dst, n.longValue(), 0, false);
    }

    private static ClassCastException castException(Class<?> t, Class<?> actual) {
        return new ClassCastException("Cannot cast " + actual.getName() + " to " + t.getName());
    }

    /**
     * Converts v, a value of type src (boxed when primitive), to type dst (boxed when primitive): asType's
     * conversion, or explicitCastArguments' when explicit.
     */
    static Object convert(Object v, Class<?> src, Class<?> dst, boolean explicit) {
        if (src == dst) return v;
        if (dst == void.class) return null;
        if (src == void.class) return zero(dst);
        if (!dst.isPrimitive()) {
            if (v == null) return null;
            if (dst == Object.class) return v;
            if (explicit && dst.isInterface()) return v;  // explicitCastArguments: interfaces as Object
            if (!dst.isInstance(v)) throw castException(dst, v.getClass());
            return v;
        }
        if (src.isPrimitive()) {
            if (!explicit && !widens(src, dst))
                throw new WrongMethodTypeException("cannot convert " + src + " to " + dst);
            return primitive(v, src, dst);
        }
        // a reference unboxed
        if (v == null) {
            if (explicit) return zero(dst);
            throw new NullPointerException();
        }
        Class<?> vp = unwrapped(v.getClass());
        if (vp == null || vp == void.class) {
            throw castException(wrapper(dst), v.getClass());
        }
        if (explicit) {
            // explicitCastArguments: unboxed at run time, then a primitive cast (a boolean as one bit)
            return primitive(v, vp, dst);
        }
        if (!widens(vp, dst)) throw castException(wrapper(dst), v.getClass());
        return primitive(v, vp, dst);
    }

    // ---------------------------------------------------------------------------------------
    // the adapters of MethodHandle

    /** asType's adapter (jdk26u's makePairwiseConvert): each argument converted to target's, the result back. */
    static MethodHandle makePairwiseConvert(MethodHandle target, MethodType newType, boolean explicit) {
        MethodType oldType = target.type();
        if (oldType.parameterCount() != newType.parameterCount())
            throw newIllegalArgumentException("arity mismatch", oldType, newType);
        int n = oldType.parameterCount();
        Class<?>[] src = newType.ptypes(), dst = oldType.ptypes();
        Class<?> rsrc = oldType.returnType(), rdst = newType.returnType();
        return adapter(newType, a -> {
            Object[] b = new Object[n];
            for (int i = 0; i < n; i++) b[i] = convert(a[i], src[i], dst[i], explicit);
            return convert(target.invokeImpl(b), rsrc, rdst, explicit);
        });
    }

    /** asSpreader's adapter: the array argument at pos spread into arrayLength of target's (of its component type). */
    static MethodHandle makeSpreadArguments(MethodHandle target, Class<?> arrayType, int pos, int arrayLength) {
        MethodType tt = target.type();
        MethodType newType = tt.replaceParameterTypes(pos, pos + arrayLength, arrayType);
        int n = tt.parameterCount();
        return adapter(newType, a -> {
            Object arr = a[pos];
            // jdk26u's checkSpreadArgument: null spreads as no elements; else the length must match
            int len = (arr == null) ? 0 : Array.getLength(arr);
            if (arr == null && arrayLength != 0) throw new NullPointerException("null array reference");
            if (len != arrayLength) throw newIllegalArgumentException("array is not of length " + arrayLength);
            Object[] b = new Object[n];
            System.arraycopy(a, 0, b, 0, pos);
            for (int i = 0; i < arrayLength; i++) b[pos + i] = Array.get(arr, i);
            System.arraycopy(a, pos + 1, b, pos + arrayLength, a.length - pos - 1);
            return target.invokeImpl(b);
        });
    }

    /** asCollector's adapter: arrayLength arguments at pos collected into a new array of arrayType. */
    static MethodHandle makeCollectArguments(MethodHandle target, Class<?> arrayType, int pos, int arrayLength) {
        MethodType tt = target.type();
        MethodType newType = tt.asCollectorType(arrayType, pos, arrayLength);
        Class<?> elem = arrayType.getComponentType();
        int n = tt.parameterCount();
        return adapter(newType, a -> {
            Object arr = Array.newInstance(elem, arrayLength);
            for (int i = 0; i < arrayLength; i++) Array.set(arr, i, a[pos + i]);
            Object[] b = new Object[n];
            System.arraycopy(a, 0, b, 0, pos);
            b[pos] = arr;
            System.arraycopy(a, pos + arrayLength, b, pos + 1, a.length - pos - arrayLength);
            return target.invokeImpl(b);
        });
    }

    /** bindTo's adapter: x as the target's first argument. */
    static MethodHandle makeBound(MethodHandle target, Object x) {
        return insert(target, 0, new Object[] { x });
    }

    /** values as the target's arguments at pos (each of its parameter's type already). */
    static MethodHandle insert(MethodHandle target, int pos, Object[] values) {
        MethodType tt = target.type();
        int k = values.length, n = tt.parameterCount();
        MethodType newType = tt.dropParameterTypes(pos, pos + k);
        return adapter(newType, a -> {
            Object[] b = new Object[n];
            System.arraycopy(a, 0, b, 0, pos);
            System.arraycopy(values, 0, b, pos, k);
            System.arraycopy(a, pos, b, pos + k, a.length - pos);
            return target.invokeImpl(b);
        });
    }

    /** A variable arity handle over target (jdk26u's AsVarargsCollector). */
    static MethodHandle makeVarargsCollector(MethodHandle target, Class<?> arrayType) {
        if (target.isVarargsCollector()) target = target.asFixedArity();
        return new AsVarargsCollector(target, arrayType);
    }

    /** jdk26u's AsVarargsCollector: its asType collects the trailing arguments when the arity asks. */
    static final class AsVarargsCollector extends MethodHandle {
        private final MethodHandle target;
        private final Class<?> arrayType;
        private MethodHandle asCollectorCache;

        AsVarargsCollector(MethodHandle target, Class<?> arrayType) {
            super(target.type());
            this.target = target;
            this.arrayType = arrayType;
        }

        @Override
        public boolean isVarargsCollector() {
            return true;
        }

        @Override
        public MethodHandle asFixedArity() {
            return target;
        }

        @Override
        Object invokeImpl(Object[] args) throws Throwable {
            return target.invokeImpl(args);
        }

        @Override
        MethodHandle asTypeUncached(MethodType newType) {
            MethodType type = this.type();
            int collectArg = type.parameterCount() - 1;
            int newArity = newType.parameterCount();
            if (newArity == collectArg + 1 &&
                type.parameterType(collectArg).isAssignableFrom(newType.parameterType(collectArg))) {
                // if arity and trailing parameter are compatible, do normal thing
                return asFixedArity().asType(newType);
            }
            // check cache
            MethodHandle acc = asCollectorCache;
            if (acc != null && acc.type().parameterCount() == newArity)
                return acc.asType(newType);
            // build and cache a collector
            int arrayLength = newArity - collectArg;
            MethodHandle collector;
            try {
                collector = asFixedArity().asCollector(arrayType, arrayLength);
            } catch (IllegalArgumentException ex) {
                throw new WrongMethodTypeException("cannot build collector", ex);
            }
            asCollectorCache = collector;
            return collector.asType(newType);
        }

        @Override
        public MethodHandle withVarargs(boolean makeVarargs) {
            return makeVarargs ? this : target;
        }
    }

    /** A handle that calls target with the same arguments, whatever its own type's view (viewAsType). */
    static MethodHandle view(MethodHandle target, MethodType newType) {
        return adapter(newType, target::invokeImpl);
    }

    static Object[] copy(Object[] a) {
        return Arrays.copyOf(a, a.length);
    }

    static void checkType(MethodType t) {
        Objects.requireNonNull(t);
    }
}
