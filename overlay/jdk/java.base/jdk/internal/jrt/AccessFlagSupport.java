/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS273 (write-ahead)"): the Java half of
 * Class.accessFlags, Executable.accessFlags and Field.accessFlags in the Go build (since JCS280 (a) Parameter.accessFlags too). jrt's Class, Method, Constructor
 * and Field are hand-written Go (go/arbace/jrt/reflect.ae); j2g writes their accessFlags methods, which call these
 * (arbace.j2g.out/support-forms), once java.lang.reflect.AccessFlag is translated. The bodies are transcribed from
 * https://github.com/openjdk/jdk26u at baf63fb: src/java.base/share/classes/java/lang/Class.java (accessFlags),
 * java/lang/reflect/Executable.java and Field.java (accessFlags) and
 * jdk/internal/reflect/ReflectionFactory.java (parseAccessFlags, classFileFormatVersion): Copyright (c) 1994, 2025,
 * Oracle and/or its affiliates, under the GNU General Public License version 2 with the Classpath Exception
 * (LICENSE.md). Their receiver is a parameter here. What the JVM answers is the closed world's: a class's class file
 * version is javac 26's (major 70, minor 0), so classFileFormatVersion is ClassFileFormatVersion.latest(); a class's
 * class file access flags (Class.getClassFileAccessFlags, JVM_GetClassFileAccessFlags) are its modifiers with
 * ACC_SUPER, which javac sets on every class that is not an interface (JVMS 4.1), as HotSpot keeps them. The rest is
 * Arbace's own, under the Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.ClassFileFormatVersion;
import java.util.Set;

/** accessFlags over the closed world's modifiers (jrt's Go calls it). */
public final class AccessFlagSupport {
    private AccessFlagSupport() {
    }

    /** Class.accessFlags of class c. */
    public static Set<AccessFlag> classAccessFlags(Class<?> c) {
        // Location.CLASS allows SUPER and AccessFlag.MODULE which
        // INNER_CLASS forbids. INNER_CLASS allows PRIVATE, PROTECTED,
        // and STATIC, which are not allowed on Location.CLASS.
        // Use getClassFileAccessFlags to expose SUPER status.
        var location = (c.isMemberClass() || c.isLocalClass() ||
                        c.isAnonymousClass() || c.isArray()) ?
            AccessFlag.Location.INNER_CLASS :
            AccessFlag.Location.CLASS;
        return parseAccessFlags((location == AccessFlag.Location.CLASS) ?
                        classFileAccessFlags(c) : c.getModifiers(), location, c);
    }

    /** Executable.accessFlags of a method or constructor of class c whose modifiers are modifiers. */
    public static Set<AccessFlag> executableAccessFlags(int modifiers, Class<?> c) {
        return parseAccessFlags(modifiers, AccessFlag.Location.METHOD, c);
    }

    /**
     * Parameter.accessFlags of a parameter (its modifiers: 0 for the synthesized parameters, JCS280 (a)) of an
     * executable of class c.
     */
    public static Set<AccessFlag> parameterAccessFlags(int modifiers, Class<?> c) {
        return parseAccessFlags(modifiers, AccessFlag.Location.METHOD_PARAMETER, c);
    }

    /** Field.accessFlags of a field of class c whose modifiers are modifiers. */
    public static Set<AccessFlag> fieldAccessFlags(int modifiers, Class<?> c) {
        return parseAccessFlags(modifiers, AccessFlag.Location.FIELD, c);
    }

    /**
     * Class.getClassFileAccessFlags: a top-level class's access_flags item; javac writes ACC_SUPER on every class
     * that is not an interface, and a primitive type's are its modifiers (PUBLIC, ABSTRACT, FINAL).
     */
    private static int classFileAccessFlags(Class<?> c) {
        int m = c.getModifiers();
        if (c.isPrimitive() || c.isInterface()) {
            return m;
        }
        return m | 0x0020; // ACC_SUPER
    }

    /** ReflectionFactory.parseAccessFlags, the class's version the closed world's (latest). */
    private static Set<AccessFlag> parseAccessFlags(int mask, AccessFlag.Location location, Class<?> classFile) {
        var cffv = ClassFileFormatVersion.latest();
        return AccessFlag.maskToAccessFlags(mask, location, cffv);
    }
}
