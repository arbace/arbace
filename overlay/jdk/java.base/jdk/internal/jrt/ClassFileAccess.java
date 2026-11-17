/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS273 (write-ahead)"): the access flag
 * constants of java.lang.classfile.ClassFile (src/java.base/share/classes/java/lang/classfile/ClassFile.java at
 * https://github.com/openjdk/jdk26u baf63fb), the values the JVM specification gives them (JVMS 4.1, 4.5, 4.6,
 * 4.7.6, 4.7.24, 4.7.25), which java.lang.reflect.AccessFlag imports statically. java.lang.classfile is out of the
 * Go build (JCS95 (a); JCS260 waits), so AccessFlag's derivation (test/jrt/derive.ae) imports these in its place.
 * The names and values are jdk26u's: Copyright (c) 2022, 2025, Oracle and/or its affiliates, under the GNU General
 * Public License version 2 with the Classpath Exception (LICENSE.md); the class Copyright (c) the Arbace authors;
 * Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

/** The access flags of java.lang.classfile.ClassFile, for java.lang.reflect.AccessFlag. */
public final class ClassFileAccess {
    private ClassFileAccess() {
    }

    public static final int ACC_PUBLIC = 0x0001;
    public static final int ACC_PROTECTED = 0x0004;
    public static final int ACC_PRIVATE = 0x0002;
    public static final int ACC_INTERFACE = 0x0200;
    public static final int ACC_ENUM = 0x4000;
    public static final int ACC_ANNOTATION = 0x2000;
    public static final int ACC_SUPER = 0x0020;
    public static final int ACC_ABSTRACT = 0x0400;
    public static final int ACC_VOLATILE = 0x0040;
    public static final int ACC_TRANSIENT = 0x0080;
    public static final int ACC_SYNTHETIC = 0x1000;
    public static final int ACC_STATIC = 0x0008;
    public static final int ACC_FINAL = 0x0010;
    public static final int ACC_SYNCHRONIZED = 0x0020;
    public static final int ACC_BRIDGE = 0x0040;
    public static final int ACC_VARARGS = 0x0080;
    public static final int ACC_NATIVE = 0x0100;
    public static final int ACC_STRICT = 0x0800;
    public static final int ACC_MODULE = 0x8000;
    public static final int ACC_OPEN = 0x20;
    public static final int ACC_MANDATED = 0x8000;
    public static final int ACC_TRANSITIVE = 0x20;
    public static final int ACC_STATIC_PHASE = 0x40;
}
