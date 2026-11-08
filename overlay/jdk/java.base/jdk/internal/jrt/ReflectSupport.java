/*
 * Arbace (F25, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F25 (write-ahead)"; doc/go/JRT-NOTES.md,
 * "Reflection"): the Java half of generic reflection in the Go build. jrt's Class, Method, Constructor and
 * Field are hand-written Go (go/arbace/jrt/reflect.ae); their generic members (getTypeParameters,
 * getGenericSuperclass, getGenericInterfaces, getGenericReturnType, getGenericParameterTypes,
 * getGenericExceptionTypes, getGenericType, toGenericString) call these static methods
 * (go/arbace/jrt/classdata.ae) with the Signature attribute the closed world recorded for the class or
 * member (arbace.classes.annot's S and G lines). The repositories, the parser, the trees, the factory and
 * the reflective objects are jdk26u's sun.reflect.generics, translated unchanged; jrt keeps each class's
 * repository (Class.genericInfo's cache). jrt's own build (go test) has a stand-in of this class
 * (go/arbace/jrt/standin_reflectsupport.ae) that answers as a class without a Signature.
 *
 * The bodies below are transcribed from https://github.com/openjdk/jdk26u at baf63fb:
 * src/java.base/share/classes/java/lang/Class.java (getGenericInfo, getFactory, getTypeParameters,
 * getGenericSuperclass, getGenericInterfaces, toGenericString, typeVarBounds, addSealingInfo,
 * hasSealedAncestor), java/lang/reflect/Method.java, Constructor.java, Executable.java (getGenericInfo,
 * getFactory, getTypeParameters, getGenericReturnType, getGenericParameterTypes,
 * getGenericExceptionTypes, sharedToGenericString, printModifiersIfNonzero, typeVarBounds,
 * specificToGenericStringHeader) and Field.java (getGenericInfo, getFactory, getGenericType,
 * toGenericString): Copyright (c) 1994, 2025, Oracle and/or its affiliates, under the GNU General Public
 * License version 2 with the Classpath Exception (LICENSE.md). Their receiver is a parameter here; what
 * the JVM's object holds (the signature, the repository) is passed in. The rest is Arbace's own,
 * under the Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import sun.reflect.generics.factory.CoreReflectionFactory;
import sun.reflect.generics.factory.GenericsFactory;
import sun.reflect.generics.repository.ClassRepository;
import sun.reflect.generics.repository.ConstructorRepository;
import sun.reflect.generics.repository.FieldRepository;
import sun.reflect.generics.repository.GenericDeclRepository;
import sun.reflect.generics.repository.MethodRepository;
import sun.reflect.generics.scope.ClassScope;
import sun.reflect.generics.scope.ConstructorScope;
import sun.reflect.generics.scope.MethodScope;

/** Generic reflection over the recorded Signature attributes (jrt's Go calls it). */
public final class ReflectSupport {

    /** Modifier.ACCESS_MODIFIERS (package-private in java.lang.reflect). */
    private static final int ACCESS_MODIFIERS = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;

    private ReflectSupport() {
    }

    // ---- the repositories (Class.getGenericInfo, Method.getGenericInfo, ...): jrt caches a class's

    /** Class.getGenericInfo's repository of class c with its Signature sig. */
    public static Object classRepository(Class<?> c, String sig) {
        return ClassRepository.make(sig, CoreReflectionFactory.make(c, ClassScope.make(c)));
    }

    /** Method.getGenericInfo's repository of method m with its Signature sig. */
    public static Object methodRepository(Method m, String sig) {
        GenericsFactory f = CoreReflectionFactory.make(m, MethodScope.make(m));
        return MethodRepository.make(sig, f);
    }

    /** Constructor.getGenericInfo's repository of constructor k with its Signature sig. */
    public static Object constructorRepository(Constructor<?> k, String sig) {
        GenericsFactory f = CoreReflectionFactory.make(k, ConstructorScope.make(k));
        return ConstructorRepository.make(sig, f);
    }

    /** Field.getGenericInfo's repository of field f with its Signature sig. */
    public static Object fieldRepository(Field f, String sig) {
        Class<?> c = f.getDeclaringClass();
        return FieldRepository.make(sig, CoreReflectionFactory.make(c, ClassScope.make(c)));
    }

    // ---- what the generic members return, from a repository

    /** getTypeParameters of a class's, method's or constructor's repository. */
    public static Object typeParameters(Object repo) {
        return ((GenericDeclRepository<?>) repo).getTypeParameters();
    }

    /** Class.getGenericSuperclass: null for an interface. */
    public static Object genericSuperclass(Class<?> c, Object repo) {
        if (c.isInterface()) {
            return null;
        }
        return ((ClassRepository) repo).getSuperclass();
    }

    /** Class.getGenericInterfaces. */
    public static Object genericInterfaces(Object repo) {
        return ((ClassRepository) repo).getSuperInterfaces();
    }

    /** Method.getGenericReturnType. */
    public static Object genericReturnType(Object repo) {
        return ((MethodRepository) repo).getReturnType();
    }

    /** Executable.getGenericParameterTypes of a method's or constructor's repository. */
    public static Object genericParameterTypes(Object repo) {
        return ((ConstructorRepository) repo).getParameterTypes();
    }

    /** The exception types of a method's or constructor's repository (Executable.getGenericExceptionTypes
     *  takes them when there are some, else getExceptionTypes). */
    public static Object genericExceptionTypes(Object repo) {
        return ((ConstructorRepository) repo).getExceptionTypes();
    }

    /** Field.getGenericType. */
    public static Object genericType(Object repo) {
        return ((FieldRepository) repo).getGenericType();
    }

    // ---- toGenericString

    static String typeVarBounds(TypeVariable<?> typeVar) {
        Type[] bounds = typeVar.getBounds();
        if (bounds.length == 1 && bounds[0].equals(Object.class)) {
            return typeVar.getName();
        } else {
            return typeVar.getName() + " extends " +
                Arrays.stream(bounds)
                .map(Type::getTypeName)
                .collect(Collectors.joining(" & "));
        }
    }

    /** Class.toGenericString. */
    public static String classToGenericString(Class<?> c) {
        if (c.isPrimitive()) {
            return c.toString();
        } else {
            StringBuilder sb = new StringBuilder();
            Class<?> component = c;
            int arrayDepth = 0;

            if (c.isArray()) {
                do {
                    arrayDepth++;
                    component = component.getComponentType();
                } while (component.isArray());
                sb.append(component.getName());
            } else {
                // Class modifiers are a superset of interface modifiers
                int modifiers = c.getModifiers() & Modifier.classModifiers();
                if (modifiers != 0) {
                    sb.append(Modifier.toString(modifiers));
                    sb.append(' ');
                }

                // A class cannot be strictfp and sealed/non-sealed so
                // it is sufficient to check for sealed-ness after all
                // modifiers are printed.
                addSealingInfo(c, modifiers, sb);

                if (c.isAnnotation()) {
                    sb.append('@');
                }
                if (c.isInterface()) { // Note: all annotation interfaces are interfaces
                    sb.append("interface");
                } else {
                    if (c.isEnum())
                        sb.append("enum");
                    else if (c.isRecord())
                        sb.append("record");
                    else
                        sb.append("class");
                }
                sb.append(' ');
                sb.append(c.getName());
            }

            TypeVariable<?>[] typeparms = component.getTypeParameters();
            if (typeparms.length > 0) {
                sb.append(Arrays.stream(typeparms)
                          .map(ReflectSupport::typeVarBounds)
                          .collect(Collectors.joining(",", "<", ">")));
            }

            if (arrayDepth > 0) sb.append("[]".repeat(arrayDepth));

            return sb.toString();
        }
    }

    private static void addSealingInfo(Class<?> c, int modifiers, StringBuilder sb) {
        // A class can be final XOR sealed XOR non-sealed.
        if (Modifier.isFinal(modifiers)) {
            return; // no-op
        } else {
            if (c.isSealed()) {
                sb.append("sealed ");
                return;
            } else {
                // Check for sealed ancestor, which implies this class
                // is non-sealed.
                if (hasSealedAncestor(c)) {
                    sb.append("non-sealed ");
                }
            }
        }
    }

    private static boolean hasSealedAncestor(Class<?> clazz) {
        // From JLS 8.1.1.2:
        // "It is a compile-time error if a class has a sealed direct
        // superclass or a sealed direct superinterface, and is not
        // declared final, sealed, or non-sealed either explicitly or
        // implicitly.
        // Thus, an effect of the sealed keyword is to force all
        // permitted subclasses to explicitly declare whether they are
        // final, sealed, or non-sealed. ..."

        // Therefore, depending on checks made by the compiler, the
        // direct superclass and direct superinterfaces of a class
        // need to be checked to see if they are sealed.
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && superclass.isSealed()) {
            return true;
        }
        for (var superinterface : clazz.getInterfaces()) {
            if (superinterface.isSealed()) {
                return true;
            }
        }
        return false;
    }

    private static void printModifiersIfNonzero(int modifiers, StringBuilder sb, int mask, boolean isDefault) {
        int mod = modifiers & mask;

        if (mod != 0 && !isDefault) {
            sb.append(Modifier.toString(mod)).append(' ');
        } else {
            int access_mod = mod & ACCESS_MODIFIERS;
            if (access_mod != 0)
                sb.append(Modifier.toString(access_mod)).append(' ');
            if (isDefault)
                sb.append("default ");
            mod = (mod & ~ACCESS_MODIFIERS);
            if (mod != 0)
                sb.append(Modifier.toString(mod)).append(' ');
        }
    }

    private static String sharedToGenericString(int modifiers, int modifierMask, boolean isDefault,
                                                TypeVariable<?>[] typeparms, String header, Type[] params,
                                                boolean isVarArgs, Type[] exceptionTypes) {
        try {
            StringBuilder sb = new StringBuilder();

            printModifiersIfNonzero(modifiers, sb, modifierMask, isDefault);

            if (typeparms.length > 0) {
                sb.append(Arrays.stream(typeparms)
                          .map(ReflectSupport::typeVarBounds)
                          .collect(Collectors.joining(",", "<", "> ")));
            }

            sb.append(header);

            sb.append('(');
            StringJoiner sj = new StringJoiner(",");
            for (int j = 0; j < params.length; j++) {
                String param = params[j].getTypeName();
                if (isVarArgs && (j == params.length - 1)) // replace T[] with T...
                    param = param.replaceFirst("\\[\\]$", "...");
                sj.add(param);
            }
            sb.append(sj.toString());
            sb.append(')');

            if (exceptionTypes.length > 0) {
                sb.append(Arrays.stream(exceptionTypes)
                          .map(Type::getTypeName)
                          .collect(Collectors.joining(",", " throws ", "")));
            }
            return sb.toString();
        } catch (Exception e) {
            return "<" + e + ">";
        }
    }

    /** Method.toGenericString. */
    public static String methodToGenericString(Method m) {
        String header;
        try {
            // Method.specificToGenericStringHeader
            Type genRetType = m.getGenericReturnType();
            header = genRetType.getTypeName() + " " + m.getDeclaringClass().getTypeName() + "." + m.getName();
        } catch (Exception e) {
            return "<" + e + ">";
        }
        return sharedToGenericString(m.getModifiers(), Modifier.methodModifiers(), m.isDefault(),
                                     m.getTypeParameters(), header, m.getGenericParameterTypes(), m.isVarArgs(),
                                     m.getGenericExceptionTypes());
    }

    /** Constructor.toGenericString. */
    public static String constructorToGenericString(Constructor<?> k) {
        // Constructor.specificToGenericStringHeader: specificToStringHeader
        String header = k.getDeclaringClass().getTypeName();
        return sharedToGenericString(k.getModifiers(), Modifier.constructorModifiers(), false,
                                     k.getTypeParameters(), header, k.getGenericParameterTypes(), k.isVarArgs(),
                                     k.getGenericExceptionTypes());
    }

    /** Field.toGenericString. */
    public static String fieldToGenericString(Field f) {
        int mod = f.getModifiers();
        Type fieldType = f.getGenericType();
        return (((mod == 0) ? "" : (Modifier.toString(mod) + " "))
            + fieldType.getTypeName() + " "
            + f.getDeclaringClass().getTypeName() + "."
            + f.getName());
    }

    // ---- a type variable's annotations (TypeVariableImpl's variant, overlay/jdk/variants/TypeVariableImpl.ae)

}
