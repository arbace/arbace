/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS271 (b) (write-ahead)"): the Java half of
 * type annotations in the Go build. jrt's Class, Method, Constructor and Field are hand-written Go
 * (go/arbace/jrt/reflect.ae); j2g writes their getAnnotated* methods, which call these (arbace.j2g.out/support-forms),
 * once sun.reflect.annotation.TypeAnnotationParser is translated. The bodies are transcribed from
 * https://github.com/openjdk/jdk26u at baf63fb: src/java.base/share/classes/java/lang/Class.java
 * (getAnnotatedSuperclass, getAnnotatedInterfaces), java/lang/reflect/Executable.java (getAnnotatedReturnType0,
 * getAnnotatedReceiverType, parameterize, getAnnotatedParameterTypes, getAllGenericParameterTypes,
 * getAnnotatedExceptionTypes), Method.java and Constructor.java (getAnnotatedReturnType, Constructor's
 * getAnnotatedReceiverType) and Field.java (getAnnotatedType): Copyright (c) 1994, 2025, Oracle and/or its affiliates,
 * under the GNU General Public License version 2 with the Classpath Exception (LICENSE.md). Their receiver is a
 * parameter here; the bytes and the pool are jdk.internal.jrt.TypeAnnotationData's (the closed world's records). The
 * closed world records no MethodParameters (javac's -parameters: JCS273's Parameter), so getAllGenericParameterTypes
 * takes jdk26u's branch without real parameter data. The rest is Arbace's own, under the Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import sun.reflect.annotation.TypeAnnotation;
import sun.reflect.annotation.TypeAnnotationParser;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

/** Type annotations over the recorded RuntimeVisibleTypeAnnotations (jrt's Go calls it). */
public final class TypeAnnotationSupport {
    private TypeAnnotationSupport() {
    }

    /** Class.getAnnotatedSuperclass of c. */
    public static AnnotatedType annotatedSuperclass(Class<?> c) {
        if (c == Object.class ||
                c.isInterface() ||
                c.isArray() ||
                c.isPrimitive() ||
                c == Void.TYPE) {
            return null;
        }

        return TypeAnnotationParser.buildAnnotatedSuperclass(TypeAnnotationData.rawClassTypeAnnotations(c),
                TypeAnnotationData.constantPool(c), c);
    }

    /** Class.getAnnotatedInterfaces of c. */
    public static AnnotatedType[] annotatedInterfaces(Class<?> c) {
         return TypeAnnotationParser.buildAnnotatedInterfaces(TypeAnnotationData.rawClassTypeAnnotations(c),
                 TypeAnnotationData.constantPool(c), c);
    }

    /** Method.getAnnotatedReturnType and Constructor.getAnnotatedReturnType (Executable.getAnnotatedReturnType0). */
    public static AnnotatedType annotatedReturnType(Executable e) {
        Type returnType = (e instanceof Method m) ? m.getGenericReturnType() : e.getDeclaringClass();
        return TypeAnnotationParser.buildAnnotatedType(TypeAnnotationData.rawExecutableTypeAnnotations(e),
                TypeAnnotationData.constantPool(e.getDeclaringClass()),
                e,
                e.getDeclaringClass(),
                returnType,
                TypeAnnotation.TypeAnnotationTarget.METHOD_RETURN);
    }

    /** Executable.getAnnotatedReceiverType, and Constructor's. */
    public static AnnotatedType annotatedReceiverType(Executable e) {
        if (e instanceof Constructor<?>) {
            Class<?> thisDeclClass = e.getDeclaringClass();
            Class<?> enclosingClass = thisDeclClass.getEnclosingClass();

            if (enclosingClass == null) {
                // A Constructor for a top-level class
                return null;
            }

            Class<?> outerDeclaringClass = thisDeclClass.getDeclaringClass();
            if (outerDeclaringClass == null) {
                // A constructor for a local or anonymous class
                return null;
            }

            // Either static nested or inner class
            if (Modifier.isStatic(thisDeclClass.getModifiers())) {
                // static nested
                return null;
            }

            // A Constructor for an inner class
            return TypeAnnotationParser.buildAnnotatedType(TypeAnnotationData.rawExecutableTypeAnnotations(e),
                    TypeAnnotationData.constantPool(thisDeclClass),
                    e,
                    thisDeclClass,
                    parameterize(enclosingClass),
                    TypeAnnotation.TypeAnnotationTarget.METHOD_RECEIVER);
        }
        if (Modifier.isStatic(e.getModifiers()))
            return null;
        return TypeAnnotationParser.buildAnnotatedType(TypeAnnotationData.rawExecutableTypeAnnotations(e),
                TypeAnnotationData.constantPool(e.getDeclaringClass()),
                e,
                e.getDeclaringClass(),
                parameterize(e.getDeclaringClass()),
                TypeAnnotation.TypeAnnotationTarget.METHOD_RECEIVER);
    }

    static Type parameterize(Class<?> c) {
        Class<?> ownerClass = c.getDeclaringClass();
        TypeVariable<?>[] typeVars = c.getTypeParameters();

        // base case, static nested classes, according to JLS 8.1.3, has no
        // enclosing instance, therefore its owner is not generified.
        if (ownerClass == null || Modifier.isStatic(c.getModifiers())) {
            if (typeVars.length == 0)
                return c;
            else
                return ParameterizedTypeImpl.make(c, typeVars, null);
        }

        // Resolve owner
        Type ownerType = parameterize(ownerClass);
        if (ownerType instanceof Class<?> && typeVars.length == 0) // We have yet to encounter type parameters
            return c;
        else
            return ParameterizedTypeImpl.make(c, typeVars, ownerType);
    }

    /** Executable.getAnnotatedParameterTypes. */
    public static AnnotatedType[] annotatedParameterTypes(Executable e) {
        return TypeAnnotationParser.buildAnnotatedTypes(TypeAnnotationData.rawExecutableTypeAnnotations(e),
                TypeAnnotationData.constantPool(e.getDeclaringClass()),
                e,
                e.getDeclaringClass(),
                allGenericParameterTypes(e),
                TypeAnnotation.TypeAnnotationTarget.METHOD_FORMAL_PARAMETER);
    }

    /**
     * Executable.getAllGenericParameterTypes without real parameter data: the generic parameter types when they are
     * as many as the class file's parameters, else the erased ones (a declaration without a Signature answers its
     * erased types as its generic ones, so hasGenericInformation's two branches agree).
     */
    static Type[] allGenericParameterTypes(Executable e) {
        final Type[] genericParamTypes = e.getGenericParameterTypes();
        final Class<?>[] nonGenericParamTypes = e.getParameterTypes();
        return genericParamTypes.length == nonGenericParamTypes.length ?
            genericParamTypes : nonGenericParamTypes;
    }

    /** Executable.getAnnotatedExceptionTypes. */
    public static AnnotatedType[] annotatedExceptionTypes(Executable e) {
        return TypeAnnotationParser.buildAnnotatedTypes(TypeAnnotationData.rawExecutableTypeAnnotations(e),
                TypeAnnotationData.constantPool(e.getDeclaringClass()),
                e,
                e.getDeclaringClass(),
                e.getGenericExceptionTypes(),
                TypeAnnotation.TypeAnnotationTarget.THROWS);
    }

    /** Field.getAnnotatedType. */
    public static AnnotatedType annotatedType(Field f) {
        return TypeAnnotationParser.buildAnnotatedType(TypeAnnotationData.rawFieldTypeAnnotations(f),
                                                       TypeAnnotationData.constantPool(f.getDeclaringClass()),
                                                       f,
                                                       f.getDeclaringClass(),
                                                       f.getGenericType(),
                                                       TypeAnnotation.TypeAnnotationTarget.FIELD);
    }
}
