/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS271 (b) (write-ahead)"): jrt's
 * jdk.internal.reflect.ConstantPool, in place of jdk26u's (src/java.base/share/classes/jdk/internal/reflect/
 * ConstantPool.java at https://github.com/openjdk/jdk26u baf63fb), whose natives read a class's constant pool in the
 * VM: here over the entries the closed world records for a class's type annotations (arbace.classes.annot's N
 * records, jdk.internal.jrt.TypeAnnotationData), by index as in the class file (a Long's or Double's second index
 * unused). The accessors sun.reflect.annotation's parsers call, as jdk26u declares them (getSize, getIntAt,
 * getLongAt, getFloatAt, getDoubleAt, getUTF8At); the other entry kinds (classes, members, strings, method types)
 * are not recorded. Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.reflect;

/** A class's constant pool, as far as its annotations' bytes index it. */
public class ConstantPool {
    // Each entry: a String (CONSTANT_Utf8), an Integer (CONSTANT_Integer, or a CONSTANT_Float's raw bits), a
    // Long (CONSTANT_Long, or a CONSTANT_Double's raw bits), or null
    private final Object[] entries;

    public ConstantPool(Object[] entries) {
        this.entries = entries;
    }

    public int getSize() {
        return entries.length;
    }

    private Object entry(int index, Class<?> kind) {
        if (index <= 0 || index >= entries.length || !kind.isInstance(entries[index])) {
            throw new IllegalArgumentException("Wrong type at constant pool index");
        }
        return entries[index];
    }

    public int getIntAt(int index) {
        return (Integer) entry(index, Integer.class);
    }

    public long getLongAt(int index) {
        return (Long) entry(index, Long.class);
    }

    public float getFloatAt(int index) {
        return Float.intBitsToFloat((Integer) entry(index, Integer.class));
    }

    public double getDoubleAt(int index) {
        return Double.longBitsToDouble((Long) entry(index, Long.class));
    }

    public String getUTF8At(int index) {
        return (String) entry(index, String.class);
    }
}
