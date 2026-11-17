/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS271 (b) (write-ahead)"): what HotSpot gives
 * jdk26u's sun.reflect.annotation.TypeAnnotationParser through JavaLangAccess and the natives of Class, Executable
 * and Field (JVM_GetClassTypeAnnotations, JVM_GetMethodTypeAnnotations, JVM_GetFieldTypeAnnotations,
 * JVM_GetClassConstantPool): a class's or member's RuntimeVisibleTypeAnnotations attribute as the class file holds it,
 * and the constant pool entries it indexes, which the closed world records ahead of time (arbace.classes.annot's Y and
 * N records; go/arbace/jrt/classdata.ae decodes them in the natives). TypeAnnotationParser's derivation
 * (test/jrt/derive.ae) asks this class where jdk26u asks JavaLangAccess; Class's, Executable's and Field's own
 * getTypeAnnotationBytes0 are TypeAnnotationSupport's. Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import jdk.internal.reflect.ConstantPool;

/** The recorded type annotations' bytes and constant pools (jrt's Go natives). */
public final class TypeAnnotationData {
    private TypeAnnotationData() {
    }

    /** The info of c's Y record of key ("T", "M NAME DESC", "C DESC", "F NAME"), or null. */
    private static native byte[] bytes0(Class<?> c, String key);

    /** c's recorded constant pool entries, by index (String, Integer, Long). */
    private static native Object[] pool0(Class<?> c);

    /** JavaLangAccess.getRawClassTypeAnnotations: Class.getRawTypeAnnotations. */
    public static byte[] rawClassTypeAnnotations(Class<?> c) {
        return bytes0(c, "T");
    }

    /** JavaLangAccess.getRawExecutableTypeAnnotations: Executable.getTypeAnnotationBytes0. */
    public static byte[] rawExecutableTypeAnnotations(Executable e) {
        StringBuilder d = new StringBuilder("(");
        for (Class<?> p : e.getParameterTypes()) {
            d.append(descriptor(p));
        }
        d.append(')');
        if (e instanceof Constructor<?>) {
            return bytes0(e.getDeclaringClass(), "C " + d + "V");
        }
        return bytes0(e.getDeclaringClass(), "M " + e.getName() + " " + d + descriptor(((Method) e).getReturnType()));
    }

    /** Field.getTypeAnnotationBytes0. */
    public static byte[] rawFieldTypeAnnotations(Field f) {
        return bytes0(f.getDeclaringClass(), "F " + f.getName());
    }

    /** JavaLangAccess.getConstantPool: the recorded entries of c's pool. */
    public static ConstantPool constantPool(Class<?> c) {
        return new ConstantPool(pool0(c));
    }

    /** A type's descriptor with binary names (the records' DESC): I, [I, Ljava.lang.String;, [Ljava.lang.String;. */
    private static String descriptor(Class<?> c) {
        if (c.isArray()) {
            return c.getName();
        }
        if (c.isPrimitive()) {
            if (c == int.class) return "I";
            if (c == long.class) return "J";
            if (c == boolean.class) return "Z";
            if (c == byte.class) return "B";
            if (c == char.class) return "C";
            if (c == short.class) return "S";
            if (c == float.class) return "F";
            if (c == double.class) return "D";
            return "V";
        }
        return "L" + c.getName() + ";";
    }
}
