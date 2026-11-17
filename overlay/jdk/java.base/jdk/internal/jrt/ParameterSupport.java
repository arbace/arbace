/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS280 (a)"): what java.lang.reflect.Parameter
 * (the derived overlay, test/jrt/derive.ae) asks of its executable that jrt's hand-written Executable has not (only its
 * Method and Constructor have them, or none: getAllGenericParameterTypes is package-private in jdk26u's Executable).
 * genericParameterType transcribes jdk26u's Executable.getAllGenericParameterTypes for an executable without real
 * parameter data (no MethodParameters records in the Go build: every parameter synthesized), from
 * https://github.com/openjdk/jdk26u at baf63fb, src/java.base/share/classes/java/lang/reflect/Executable.java:
 * Copyright (c) 2012, 2025, Oracle and/or its affiliates, under the GNU General Public License version 2 with the
 * Classpath Exception (LICENSE.md). The rest Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/** Parameter's questions to its executable, a Method or a Constructor. */
public final class ParameterSupport {

    private ParameterSupport() {
    }

    private static Type[] genericParameterTypes(Executable e) {
        if (e instanceof Method m)
            return m.getGenericParameterTypes();
        return ((Constructor<?>) e).getGenericParameterTypes();
    }

    /**
     * The parameterized type of e's parameter i: getAllGenericParameterTypes()[i] without real parameter data, the
     * generic parameter types when there are as many as the parameter types (an inner class's constructor's signature
     * leaves the enclosing instance out), else the parameter types.
     */
    public static Type genericParameterType(Executable e, int i) {
        final Type[] genericParamTypes = genericParameterTypes(e);
        final Class<?>[] nonGenericParamTypes = e.getParameterTypes();
        return (genericParamTypes.length == nonGenericParamTypes.length ? genericParamTypes : nonGenericParamTypes)[i];
    }

    /** The declaration annotations of e's parameter i: getParameterAnnotations()[i]. */
    public static Annotation[] parameterAnnotations(Executable e, int i) {
        if (e instanceof Method m)
            return m.getParameterAnnotations()[i];
        return ((Constructor<?>) e).getParameterAnnotations()[i];
    }
}
