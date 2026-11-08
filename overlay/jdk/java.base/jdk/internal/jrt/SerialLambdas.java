/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, JCS169 (a), "As built: JCS169 (write-ahead)"):
 * serializable lambdas and method references in the Go build. On the JVM javac compiles a serializable
 * lambda into an invokedynamic of LambdaMetafactory.altMetafactory with FLAG_SERIALIZABLE, whose class's
 * writeReplace answers a java.lang.invoke.SerializedLambda (InnerClassLambdaMetafactory), and a
 * $deserializeLambda$ in the capturing class, a switch over the SerializedLambda's implementation method
 * and functional interface that makes the lambda again from its captured arguments (LambdaToMethod), which
 * SerializedLambda.readResolve calls by reflection. The Go build has no invokedynamic and no class made at
 * run time for a translated lambda: each serializable lambda's site registers here what the two know of it
 * (the SerializedLambda's fields and a Factory making the lambda's object from captured values: j2g for a
 * translated class, at its package's initialization; the interpreter for an interpreted one, when it builds
 * the site), each serializable lambda's object holds an Instance (its site and captured values), its
 * class's writeReplace is replace(instance), and SerializedLambda's readResolve (jrt's variant,
 * overlay/jdk/variants/SerializedLambda.ae) is deserialize: the site found by javac's $deserializeLambda$
 * tests (the capturing class, the implementation method's name and signature, the functional interface's
 * class, method and signature, the implementation's kind and class), else jdk26u's InvalidObjectException
 * around javac's IllegalArgumentException "Invalid lambda deserialization".
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

/** Serializable lambdas' sites, their objects' serial state and their deserialization. */
public final class SerialLambdas {

    private SerialLambdas() {
    }

    /** A site's way to make its lambda's object from the captured values (javac's indy in $deserializeLambda$). */
    @FunctionalInterface
    public interface Factory {
        Object make(Object[] capturedArgs);
    }

    /** A serializable lambda's site: what its SerializedLambda holds besides the captured values. */
    public static final class Site {
        final Class<?> capturingClass;
        final String functionalInterfaceClass;
        final String functionalInterfaceMethodName;
        final String functionalInterfaceMethodSignature;
        final int implMethodKind;
        final String implClass;
        final String implMethodName;
        final String implMethodSignature;
        final String instantiatedMethodType;
        final Factory factory;

        Site(Class<?> capturingClass, String functionalInterfaceClass, String functionalInterfaceMethodName,
             String functionalInterfaceMethodSignature, int implMethodKind, String implClass, String implMethodName,
             String implMethodSignature, String instantiatedMethodType, Factory factory) {
            this.capturingClass = capturingClass;
            this.functionalInterfaceClass = functionalInterfaceClass;
            this.functionalInterfaceMethodName = functionalInterfaceMethodName;
            this.functionalInterfaceMethodSignature = functionalInterfaceMethodSignature;
            this.implMethodKind = implMethodKind;
            this.implClass = implClass;
            this.implMethodName = implMethodName;
            this.implMethodSignature = implMethodSignature;
            this.instantiatedMethodType = instantiatedMethodType;
            this.factory = factory;
        }
    }

    /** A serializable lambda's object's serial state: its site and its captured values. */
    public static final class Instance {
        final Site site;
        final Object[] capturedArgs;

        Instance(Site site, Object[] capturedArgs) {
            this.site = site;
            this.capturedArgs = capturedArgs;
        }
    }

    private static final ConcurrentHashMap<String, Site> SITES = new ConcurrentHashMap<>();

    private static String key(String capturingClass, String implMethodName, String implMethodSignature) {
        return capturingClass + " " + implMethodName + " " + implMethodSignature;
    }

    /**
     * Registers a serializable lambda's site (class names with slashes, as SerializedLambda holds them; the
     * kind a MethodHandleInfo REF_ kind) and returns it. A site registered again (the REPL's class made
     * anew) replaces the earlier one.
     */
    public static Site register(Class<?> capturingClass, String functionalInterfaceClass,
                                String functionalInterfaceMethodName, String functionalInterfaceMethodSignature,
                                int implMethodKind, String implClass, String implMethodName,
                                String implMethodSignature, String instantiatedMethodType, Factory factory) {
        Site s = new Site(capturingClass, functionalInterfaceClass, functionalInterfaceMethodName,
                          functionalInterfaceMethodSignature, implMethodKind, implClass, implMethodName,
                          implMethodSignature, instantiatedMethodType, factory);
        SITES.put(key(capturingClass.getName().replace('.', '/'), implMethodName, implMethodSignature), s);
        return s;
    }

    /** The serial state of a lambda's object made at site s with these captured values. */
    public static Object instance(Site s, Object[] capturedArgs) {
        return new Instance(s, capturedArgs);
    }

    /** writeReplace of a serializable lambda's object whose serial state is instance: its SerializedLambda. */
    public static Object replace(Object instance) throws ObjectStreamException {
        if (!(instance instanceof Instance i)) {
            // an object of a serializable functional interface's adapter made without a site (a Clojure
            // fn adapted to the interface): no lambda the JVM would have serialized
            throw new java.io.NotSerializableException("a functional interface's adapter of a function");
        }
        Site s = i.site;
        return new SerializedLambda(s.capturingClass, s.functionalInterfaceClass, s.functionalInterfaceMethodName,
                                    s.functionalInterfaceMethodSignature, s.implMethodKind, s.implClass,
                                    s.implMethodName, s.implMethodSignature, s.instantiatedMethodType,
                                    i.capturedArgs.clone());
    }

    /** SerializedLambda.readResolve: the lambda made again by its site's factory, as $deserializeLambda$. */
    public static Object deserialize(SerializedLambda sl) throws ObjectStreamException {
        Site s = SITES.get(key(sl.getCapturingClass(), sl.getImplMethodName(), sl.getImplMethodSignature()));
        if (s == null
                || s.implMethodKind != sl.getImplMethodKind()
                || !s.functionalInterfaceClass.equals(sl.getFunctionalInterfaceClass())
                || !s.functionalInterfaceMethodName.equals(sl.getFunctionalInterfaceMethodName())
                || !s.functionalInterfaceMethodSignature.equals(sl.getFunctionalInterfaceMethodSignature())
                || !s.implClass.equals(sl.getImplClass())) {
            // javac's $deserializeLambda$ throws IllegalArgumentException, which readResolve gets through
            // Method.invoke (InvocationTargetException, a ReflectiveOperationException)
            throw new InvalidObjectException("ReflectiveOperationException during deserialization",
                    new InvocationTargetException(new IllegalArgumentException("Invalid lambda deserialization")));
        }
        Object[] caps = new Object[sl.getCapturedArgCount()];
        for (int k = 0; k < caps.length; k++) {
            caps[k] = sl.getCapturedArg(k);
        }
        return s.factory.make(caps);
    }
}
