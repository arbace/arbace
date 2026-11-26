/*
 * Arbace (overlay/jdk, java-tail3-1126; the user's decision JT2-6 (a) of 2026-11-26): VarHandles made at run time, for the
 * classes of Java forms the interpreter defines. Compiled code keeps J2G-SPEC section 8.5 (j2g reads the constant handles of
 * a class's static initializer and translates each access into an atomic operation on the field); a class made at the REPL
 * has no such translation, so the Java forms' analyzer (arbace.classes.analyze, *runtime-varhandles*, bound off by j2g)
 * writes, in the interpreter's code:
 *
 *   lookup.findVarHandle(C.class, "f", T.class)  ->  VarHandles.findVarHandle(lookup, C.class, "f", T.class)
 *   (and findStaticVarHandle, unreflectVarHandle, MethodHandles.arrayElementVarHandle)
 *   (int) VH.getAndAdd(x, 1)                      ->  VarHandles.accessI(VH, "getAndAdd", int.class,
 *                                                                        new Class<?>[] {C, int}, new Object[] {x, 1})
 *   VarHandle.fullFence() (and its kin)          ->  VarHandles.fullFence()
 *
 * A handle is a VarHandles.Handle over the field (java.lang.reflect.Field: the interpreter's slots or the compiled class's)
 * or an array's elements (java.lang.reflect.Array). Every access mode runs under one lock, so that the atomic modes
 * (compareAndSet and its kin, getAndSet, getAndAdd, the bitwise ones) are atomic among the handles' accesses; plain writes of
 * the field outside a handle are not ordered with them, as the interpreter's field writes are not volatile. Values compare
 * as the JDK's handles compare them: references by identity, float and double by their raw bits. Arguments are converted
 * to the variable's type as asType converts the call site's (widening, unboxing), a reference not of the variable's type a
 * ClassCastException (ArrayStoreException for an element of a reference array that is not of its component type at run
 * time), a final field's or a numeric mode on a reference an UnsupportedOperationException. Copyright (c) the Arbace
 * authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;

/** The run-time VarHandles of the interpreter's classes. */
public final class VarHandles {
    private VarHandles() { }

    private static final Object LOCK = new Object();

    private static final int INSTANCE = 0, STATIC = 1, ARRAY = 2;

    /** A handle: a field of a class (instance or static) or the elements of an array class. */
    public static final class Handle {
        final int kind;
        final Field field;
        final Class<?> varType;
        final Class<?> receiver;
        final boolean readOnly;

        Handle(int kind, Field field, Class<?> varType, Class<?> receiver, boolean readOnly) {
            this.kind = kind;
            this.field = field;
            this.varType = varType;
            this.receiver = receiver;
            this.readOnly = readOnly;
        }

        public Class<?> varType() {
            return varType;
        }

        public List<Class<?>> coordinateTypes() {
            switch (kind) {
                case INSTANCE: return List.of(receiver);
                case STATIC: return List.of();
                default: return List.of(receiver, int.class);
            }
        }

        @Override
        public String toString() {
            return "VarHandle[varType=" + varType.getName() + ", coord=" + coordinateTypes() + "]";
        }
    }

    // ---------------------------------------------------------------------------------------
    // the handles' makers

    private static Field field(Class<?> decl, String name, Class<?> type, boolean isStatic) throws NoSuchFieldException {
        Objects.requireNonNull(decl);
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
        Field f = decl.getDeclaredField(name);
        if (f.getType() != type || Modifier.isStatic(f.getModifiers()) != isStatic) {
            throw new NoSuchFieldException(name);
        }
        f.setAccessible(true);
        return f;
    }

    /** Lookup.findVarHandle(recv, name, type). */
    public static Object findVarHandle(MethodHandles.Lookup lookup, Class<?> recv, String name, Class<?> type)
            throws NoSuchFieldException, IllegalAccessException {
        Field f = field(recv, name, type, false);
        return new Handle(INSTANCE, f, type, recv, Modifier.isFinal(f.getModifiers()));
    }

    /** Lookup.findStaticVarHandle(decl, name, type). */
    public static Object findStaticVarHandle(MethodHandles.Lookup lookup, Class<?> decl, String name, Class<?> type)
            throws NoSuchFieldException, IllegalAccessException {
        Field f = field(decl, name, type, true);
        return new Handle(STATIC, f, type, decl, Modifier.isFinal(f.getModifiers()));
    }

    /** Lookup.unreflectVarHandle(f). */
    public static Object unreflectVarHandle(MethodHandles.Lookup lookup, Field f) throws IllegalAccessException {
        boolean isStatic = Modifier.isStatic(f.getModifiers());
        f.setAccessible(true);
        return new Handle(isStatic ? STATIC : INSTANCE, f, f.getType(), f.getDeclaringClass(), Modifier.isFinal(f.getModifiers()));
    }

    /** MethodHandles.arrayElementVarHandle(arrayClass). */
    public static Object arrayElementVarHandle(Class<?> arrayClass) {
        if (!arrayClass.isArray()) {
            throw new IllegalArgumentException("not an array class: " + arrayClass.getName());
        }
        return new Handle(ARRAY, null, arrayClass.getComponentType(), arrayClass, false);
    }

    // ---------------------------------------------------------------------------------------
    // fences: the lock orders the handles' accesses already

    public static void fullFence() { synchronized (LOCK) { } }
    public static void acquireFence() { synchronized (LOCK) { } }
    public static void releaseFence() { synchronized (LOCK) { } }
    public static void loadLoadFence() { synchronized (LOCK) { } }
    public static void storeStoreFence() { synchronized (LOCK) { } }

    // ---------------------------------------------------------------------------------------
    // a handle's methods that are not access modes

    /** vh.name(args) for VarHandle's methods that are not signature polymorphic. */
    public static Object call(Object vh, String name, Object[] args) {
        Handle h = handle(vh);
        switch (name) {
            case "varType": return h.varType();
            case "coordinateTypes": return h.coordinateTypes();
            case "toString": return h.toString();
            case "hashCode": return System.identityHashCode(h);
            case "equals": return h == args[0];
            case "hasInvokeExactBehavior": return false;
            case "withInvokeBehavior": return h;
            default: throw new UnsupportedOperationException("jrt: VarHandle." + name + " on a run-time VarHandle");
        }
    }

    // ---------------------------------------------------------------------------------------
    // access modes, by the call's return type (the analyzer writes them as Invokers' calls)

    public static void accessV(Object vh, String mode, Class<?> r, Class<?>[] p, Object[] a) {
        access(vh, mode, r, p, a);
    }
    public static boolean accessZ(Object vh, String mode, Class<?> r, Class<?>[] p, Object[] a) {
        return (Boolean) access(vh, mode, r, p, a);
    }
    public static byte accessB(Object vh, String mode, Class<?> r, Class<?>[] p, Object[] a) {
        return (Byte) access(vh, mode, r, p, a);
    }
    public static char accessC(Object vh, String mode, Class<?> r, Class<?>[] p, Object[] a) {
        return (Character) access(vh, mode, r, p, a);
    }
    public static short accessS(Object vh, String mode, Class<?> r, Class<?>[] p, Object[] a) {
        return (Short) access(vh, mode, r, p, a);
    }
    public static int accessI(Object vh, String mode, Class<?> r, Class<?>[] p, Object[] a) {
        return (Integer) access(vh, mode, r, p, a);
    }
    public static long accessJ(Object vh, String mode, Class<?> r, Class<?>[] p, Object[] a) {
        return (Long) access(vh, mode, r, p, a);
    }
    public static float accessF(Object vh, String mode, Class<?> r, Class<?>[] p, Object[] a) {
        return (Float) access(vh, mode, r, p, a);
    }
    public static double accessD(Object vh, String mode, Class<?> r, Class<?>[] p, Object[] a) {
        return (Double) access(vh, mode, r, p, a);
    }
    public static Object accessL(Object vh, String mode, Class<?> r, Class<?>[] p, Object[] a) {
        return access(vh, mode, r, p, a);
    }

    private static Handle handle(Object vh) {
        if (vh == null) {
            throw new NullPointerException();
        }
        if (!(vh instanceof Handle h)) {
            throw new UnsupportedOperationException("jrt: not a run-time VarHandle: " + vh.getClass().getName());
        }
        return h;
    }

    private static final int GET = 0, SET = 1, CAS = 2, CAE = 3, GAS = 4, ADD = 5, OR = 6, AND = 7, XOR = 8;

    private static int kind(String mode) {
        switch (mode) {
            case "get": case "getVolatile": case "getOpaque": case "getAcquire": return GET;
            case "set": case "setVolatile": case "setOpaque": case "setRelease": return SET;
            case "compareAndSet": case "weakCompareAndSet": case "weakCompareAndSetPlain":
            case "weakCompareAndSetAcquire": case "weakCompareAndSetRelease": return CAS;
            case "compareAndExchange": case "compareAndExchangeAcquire": case "compareAndExchangeRelease": return CAE;
            case "getAndSet": case "getAndSetAcquire": case "getAndSetRelease": return GAS;
            case "getAndAdd": case "getAndAddAcquire": case "getAndAddRelease": return ADD;
            case "getAndBitwiseOr": case "getAndBitwiseOrAcquire": case "getAndBitwiseOrRelease": return OR;
            case "getAndBitwiseAnd": case "getAndBitwiseAndAcquire": case "getAndBitwiseAndRelease": return AND;
            case "getAndBitwiseXor": case "getAndBitwiseXorAcquire": case "getAndBitwiseXorRelease": return XOR;
            default: throw new UnsupportedOperationException("jrt: VarHandle." + mode);
        }
    }

    private static int values(int k) {
        switch (k) {
            case GET: return 0;
            case CAS: case CAE: return 2;
            default: return 1;
        }
    }

    private static Object access(Object vh, String mode, Class<?> r, Class<?>[] p, Object[] a) {
        Handle h = handle(vh);
        int k = kind(mode);
        int coords = h.kind == INSTANCE ? 1 : h.kind == STATIC ? 0 : 2;
        if (a.length != coords + values(k)) {
            throw new WrongMethodTypeException("expected " + (coords + values(k)) + " arguments of VarHandle." + mode
                                               + ", got " + a.length);
        }
        Object target = null;
        int index = 0;
        if (h.kind == INSTANCE) {
            target = a[0];
            if (target == null) {
                throw new NullPointerException();
            }
            if (!h.receiver.isInstance(target)) {
                throw new ClassCastException("Cannot cast " + target.getClass().getName() + " to " + h.receiver.getName());
            }
        } else if (h.kind == ARRAY) {
            target = a[0];
            if (target == null) {
                throw new NullPointerException();
            }
            if (!h.receiver.isInstance(target)) {
                throw new ClassCastException("Cannot cast " + target.getClass().getName() + " to " + h.receiver.getName());
            }
            index = ((Number) a[1]).intValue();
            int n = Array.getLength(target);
            if (index < 0 || index >= n) {
                throw new ArrayIndexOutOfBoundsException("Index " + index + " out of bounds for length " + n);
            }
        }
        if (k != GET && h.readOnly) {
            throw new UnsupportedOperationException();
        }
        Object v1 = values(k) >= 1 ? convert(h, a[coords], p[coords]) : null;
        Object v2 = values(k) == 2 ? convert(h, a[coords + 1], p[coords + 1]) : null;
        synchronized (LOCK) {
            Object cur = read(h, target, index);
            switch (k) {
                case GET:
                    return result(cur, r);
                case SET:
                    write(h, target, index, v1);
                    return null;
                case CAS:
                    if (same(h.varType, cur, v1)) {
                        write(h, target, index, v2);
                        return Boolean.TRUE;
                    }
                    return Boolean.FALSE;
                case CAE:
                    if (same(h.varType, cur, v1)) {
                        write(h, target, index, v2);
                    }
                    return result(cur, r);
                case GAS:
                    write(h, target, index, v1);
                    return result(cur, r);
                default:
                    write(h, target, index, arith(h.varType, k, cur, v1));
                    return result(cur, r);
            }
        }
    }

    private static Object read(Handle h, Object target, int index) {
        try {
            return h.kind == ARRAY ? Array.get(target, index) : h.field.get(target);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void write(Handle h, Object target, int index, Object v) {
        try {
            if (h.kind == ARRAY) {
                // a reference array's own component type (an Object[] handle on a String[]): the JVM's aastore
                Class<?> c = target.getClass().getComponentType();
                if (v != null && !c.isPrimitive() && !c.isInstance(v)) {
                    throw new ArrayStoreException(v.getClass().getName());
                }
                Array.set(target, index, v);
            } else {
                h.field.set(target, v);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The argument v (of the call site's type s) as a value of the handle's variable type. */
    private static Object convert(Handle h, Object v, Class<?> s) {
        Class<?> t = h.varType;
        if (!t.isPrimitive()) {
            if (v != null && !t.isInstance(v)) {
                throw new ClassCastException("Cannot cast " + v.getClass().getName() + " to " + t.getName());
            }
            return v;
        }
        if (v == null) {
            throw new NullPointerException();
        }
        if (t == boolean.class) {
            if (v instanceof Boolean) return v;
        } else if (t == char.class) {
            if (v instanceof Character) return v;
        } else if (v instanceof Number || v instanceof Character) {
            long l = v instanceof Character c ? c : (v instanceof Double || v instanceof Float) ? 0 : ((Number) v).longValue();
            boolean fp = v instanceof Double || v instanceof Float;
            if (t == int.class && !fp) return (int) l;
            if (t == long.class && !fp) return l;
            if (t == short.class && !fp) return (short) l;
            if (t == byte.class && !fp) return (byte) l;
            if (t == float.class) return fp ? ((Number) v).floatValue() : (float) l;
            if (t == double.class) return fp ? ((Number) v).doubleValue() : (double) l;
        }
        throw new ClassCastException("Cannot convert " + v.getClass().getName() + " to " + t.getName());
    }

    /** The handle's value cur as the call site's return type r. */
    private static Object result(Object cur, Class<?> r) {
        if (r == void.class || r == Object.class || cur == null || !r.isPrimitive()) {
            return cur;
        }
        if (r == boolean.class || r == char.class) {
            return cur;
        }
        Number n = cur instanceof Character c ? Integer.valueOf(c) : (Number) cur;
        if (r == int.class) return n.intValue();
        if (r == long.class) return n.longValue();
        if (r == short.class) return n.shortValue();
        if (r == byte.class) return n.byteValue();
        if (r == float.class) return n.floatValue();
        return n.doubleValue();
    }

    /** compareAndSet's test: references by identity, float and double by their raw bits, the rest by value. */
    private static boolean same(Class<?> t, Object a, Object b) {
        if (!t.isPrimitive()) {
            return a == b;
        }
        if (t == float.class) {
            return Float.floatToRawIntBits((Float) a) == Float.floatToRawIntBits((Float) b);
        }
        if (t == double.class) {
            return Double.doubleToRawLongBits((Double) a) == Double.doubleToRawLongBits((Double) b);
        }
        return a.equals(b);
    }

    /** getAndAdd's and the bitwise modes' new value. */
    private static Object arith(Class<?> t, int k, Object cur, Object v) {
        if (t == boolean.class && k != ADD) {
            boolean x = (Boolean) cur, y = (Boolean) v;
            return k == OR ? (x | y) : k == AND ? (x & y) : (x ^ y);
        }
        if (t == float.class && k == ADD) return (Float) cur + (Float) v;
        if (t == double.class && k == ADD) return (Double) cur + (Double) v;
        if (t == char.class) {
            char x = (Character) cur, y = (Character) v;
            return (char) (k == ADD ? x + y : k == OR ? (x | y) : k == AND ? (x & y) : (x ^ y));
        }
        if (t == int.class || t == short.class || t == byte.class) {
            int x = ((Number) cur).intValue(), y = ((Number) v).intValue();
            int z = k == ADD ? x + y : k == OR ? (x | y) : k == AND ? (x & y) : (x ^ y);
            return t == int.class ? (Object) z : t == short.class ? (Object) (short) z : (Object) (byte) z;
        }
        if (t == long.class) {
            long x = (Long) cur, y = (Long) v;
            return k == ADD ? x + y : k == OR ? (x | y) : k == AND ? (x & y) : (x ^ y);
        }
        throw new UnsupportedOperationException();
    }
}
