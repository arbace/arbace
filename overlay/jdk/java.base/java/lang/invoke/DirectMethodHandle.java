/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F15 (java.lang.invoke)"): jrt's
 * java.lang.invoke.DirectMethodHandle, a handle of a field, method or constructor over java.lang.reflect
 * (jdk26u's, https://github.com/openjdk/jdk26u at baf63fb, src/java.base/share/classes/java/lang/invoke/
 * DirectMethodHandle.java, links a MemberName the VM resolved). Its reference kind and member are the
 * symbolic reference MethodHandleInfo reveals (InfoFromMemberName's answers). The invocations:
 * getField/getStatic Field.get, putField/putStatic Field.set, invokeStatic Method.invoke(null, ...),
 * invokeVirtual and invokeInterface Method.invoke on the receiver (reflection's virtual dispatch, the JVM's),
 * newInvokeSpecial Constructor.newInstance; an InvocationTargetException's cause is thrown as itself, as the
 * VM's linked call throws it. invokeSpecial: a private method directly (not virtual, JVMS 5.4.6), Object's
 * hashCode, equals and toString as Object's own; any other method of a superclass, called non-virtually on
 * the special caller, is reflection's virtual call when the receiver's class does not override it, else
 * an UnsupportedOperationException (jrt's reflection has no non-virtual call; the triage's F15 records it).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.lang.invoke;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static java.lang.invoke.MethodHandleNatives.Constants.*;

/** A direct handle: a method, constructor or field, over reflection. */
final class DirectMethodHandle extends MethodHandle {

    final byte refKind;
    final Class<?> declaringClass;
    final String name;
    final MethodType memberType;   // the member's own type (MethodHandleInfo.getMethodType)
    final Member member;
    final boolean varargs;

    DirectMethodHandle(MethodType type, byte refKind, Class<?> declaringClass, String name,
                       MethodType memberType, Member member) {
        super(type);
        this.refKind = refKind;
        this.declaringClass = declaringClass;
        this.name = name;
        this.memberType = memberType;
        this.member = member;
        this.varargs = (member instanceof Method m && m.isVarArgs())
                    || (member instanceof Constructor<?> c && c.isVarArgs());
        if (member instanceof java.lang.reflect.AccessibleObject ao) {
            try {
                ao.setAccessible(true);
            } catch (RuntimeException e) {
                // jrt has no run-time access checks to lift
            }
        }
    }

    /** The same member seen with another type (a narrowed receiver, findSpecial's caller). */
    DirectMethodHandle withType(MethodType t) {
        return new DirectMethodHandle(t, refKind, declaringClass, name, memberType, member);
    }

    /** The same member with another reference kind (MethodHandleInfo's normalized one). */
    DirectMethodHandle withKind(byte kind) {
        return new DirectMethodHandle(type(), kind, declaringClass, name, memberType, member);
    }

    int getModifiers() {
        return member.getModifiers();
    }

    private static Object[] rest(Object[] a) {
        Object[] r = new Object[a.length - 1];
        System.arraycopy(a, 1, r, 0, r.length);
        return r;
    }

    @Override
    Object invokeImpl(Object[] a) throws Throwable {
        try {
            switch (refKind) {
                case REF_getField:
                    return ((Field) member).get(java.util.Objects.requireNonNull(a[0]));
                case REF_getStatic:
                    return ((Field) member).get(null);
                case REF_putField:
                    ((Field) member).set(java.util.Objects.requireNonNull(a[0]), a[1]);
                    return null;
                case REF_putStatic:
                    ((Field) member).set(null, a[0]);
                    return null;
                case REF_invokeStatic:
                    return ((Method) member).invoke(null, a);
                case REF_invokeVirtual:
                case REF_invokeInterface:
                    return ((Method) member).invoke(java.util.Objects.requireNonNull(a[0]), rest(a));
                case REF_invokeSpecial:
                    return invokeSpecial(a);
                case REF_newInvokeSpecial:
                    return ((Constructor<?>) member).newInstance(a);
                default:
                    throw new InternalError("bad reference kind " + refKind);
            }
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private Object invokeSpecial(Object[] a) throws Throwable {
        Method m = (Method) member;
        Object self = java.util.Objects.requireNonNull(a[0]);
        if (Modifier.isPrivate(m.getModifiers()))
            return m.invoke(self, rest(a));
        if (m.getDeclaringClass() == Object.class) {
            switch (m.getName()) {
                case "hashCode": return System.identityHashCode(self);
                case "equals": return self == a[1];
                case "toString": return self.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(self));
                default: break;
            }
        }
        Method actual = null;
        for (Class<?> c = self.getClass(); c != null && actual == null; c = c.getSuperclass()) {
            if (c == m.getDeclaringClass()) break;
            try {
                actual = c.getDeclaredMethod(m.getName(), m.getParameterTypes());
            } catch (NoSuchMethodException e) {
                // not declared here
            }
        }
        if (actual != null && !Modifier.isPrivate(actual.getModifiers()) && !Modifier.isStatic(actual.getModifiers()))
            throw new UnsupportedOperationException("jrt: a non-virtual call of " + m + " on " + self.getClass().getName()
                                                    + ", which overrides it (java.lang.invoke's invokeSpecial over reflection)");
        return m.invoke(self, rest(a));
    }
}
