/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F15 (java.lang.invoke)"): jrt's lambda
 * metafactory, LambdaMetafactory's (derived: in place of jdk26u's InnerClassLambdaMetafactory, which spins a hidden
 * class's bytecode, https://github.com/openjdk/jdk26u at baf63fb, src/java.base/share/classes/java/lang/invoke/
 * InnerClassLambdaMetafactory.java). The checks are jdk26u's AbstractValidatingLambdaMetafactory's, unpatched; the
 * instances are java.lang.reflect.Proxy's (JCS170: proxy classes over the evaluator's Dyn), one per call of the call
 * site's target (or, with nothing captured, one instance for every call, as jdk26u's), implementing the functional
 * interface and the marker interfaces. A call of the interface's method (its interfaceMethodType's signature, or a
 * bridge's of altMethods) is the implementation's: the captured values, then the call's arguments, the
 * implementation adapted by asType to the captured types and dynamicMethodType (the lambda's conversions are a subset
 * of asType's, which the validation guarantees), the result converted to the method's return type. Object's equals,
 * hashCode and toString are the object's identity's, as a lambda's; another default method of the interface is
 * InvocationHandler.invokeDefault's, which jrt has not (JCS241): UnsupportedOperationException. Serializable lambdas
 * made through here have no serial form (j2g's and the interpreter's own serializable lambdas are JCS169's).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.lang.invoke;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedHashSet;

/** A lambda metafactory whose instances are proxies over the implementation handle. */
final class ProxyLambdaMetafactory extends AbstractValidatingLambdaMetafactory {

    ProxyLambdaMetafactory(MethodHandles.Lookup caller,
                           MethodType factoryType,
                           String interfaceMethodName,
                           MethodType interfaceMethodType,
                           MethodHandle implementation,
                           MethodType dynamicMethodType,
                           boolean isSerializable,
                           Class<?>[] altInterfaces,
                           MethodType[] altMethods)
            throws LambdaConversionException {
        super(caller, factoryType, interfaceMethodName, interfaceMethodType,
              implementation, dynamicMethodType, isSerializable, altInterfaces, altMethods);
    }

    @Override
    CallSite buildCallSite() throws LambdaConversionException {
        LinkedHashSet<Class<?>> set = new LinkedHashSet<>();
        set.add(interfaceClass);
        set.addAll(Arrays.asList(altInterfaces));
        Class<?>[] interfaces = set.toArray(new Class<?>[0]);
        // the implementation as the lambda's method calls it: the captured values' types, then dynamicMethodType's
        MethodType callType = MethodType.methodType(dynamicMethodType.returnType(), factoryType.parameterArray())
                .appendParameterTypes(dynamicMethodType.parameterArray());
        MethodHandle target;
        try {
            target = implementation.asFixedArity().asType(callType);
        } catch (WrongMethodTypeException e) {
            throw new LambdaConversionException(e.getMessage());
        }
        MethodType[] signatures = new MethodType[altMethods.length + 1];
        signatures[0] = interfaceMethodType;
        System.arraycopy(altMethods, 0, signatures, 1, altMethods.length);
        ClassLoader loader = interfaceClass.getClassLoader();
        String name = interfaceMethodName;
        int k = factoryType.parameterCount();
        if (k == 0) {
            Object instance = newInstance(loader, interfaces, name, signatures, target, new Object[0]);
            return new ConstantCallSite(MethodHandles.constant(interfaceClass, instance));
        }
        MethodHandle factory = MethodHandleImpl.adapter(factoryType,
                a -> newInstance(loader, interfaces, name, signatures, target, a.clone()));
        return new ConstantCallSite(factory);
    }

    private static Object newInstance(ClassLoader loader, Class<?>[] interfaces, String name, MethodType[] signatures,
                                      MethodHandle target, Object[] captured) {
        return Proxy.newProxyInstance(loader, interfaces, new Handler(name, signatures, target, captured));
    }

    /** The calls of a lambda proxy's methods. */
    private static final class Handler implements InvocationHandler {
        private final String name;
        private final MethodType[] signatures;
        private final MethodHandle target;
        private final Object[] captured;

        Handler(String name, MethodType[] signatures, MethodHandle target, Object[] captured) {
            this.name = name;
            this.signatures = signatures;
            this.target = target;
            this.captured = captured;
        }

        private boolean implemented(Method m) {
            if (!m.getName().equals(name)) return false;
            Class<?>[] ps = m.getParameterTypes();
            for (MethodType t : signatures) {
                if (t.returnType() == m.getReturnType() && Arrays.equals(t.parameterArray(), ps)) return true;
            }
            return false;
        }

        @Override
        public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
            if (m.getDeclaringClass() == Object.class) {
                switch (m.getName()) {
                    case "equals": return proxy == args[0];
                    case "hashCode": return System.identityHashCode(proxy);
                    case "toString": return proxy.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
                    default: break;
                }
            }
            if (!implemented(m))
                throw new UnsupportedOperationException("jrt: a lambda proxy's method " + m + " is not its functional"
                                                        + " interface's (InvocationHandler.invokeDefault: JCS241)");
            MethodType tt = target.type();
            Class<?>[] ps = m.getParameterTypes();
            int n = (args == null) ? 0 : args.length;
            int k = captured.length;
            Object[] a = new Object[k + n];
            System.arraycopy(captured, 0, a, 0, k);
            for (int i = 0; i < n; i++) {
                // the method's argument (of its erased type, boxed) cast or converted to dynamicMethodType's
                a[k + i] = MethodHandleImpl.convert(args[i], ps[i], tt.parameterType(k + i), false);
            }
            Object r = target.invokeImpl(a);
            return MethodHandleImpl.convert(r, tt.returnType(), m.getReturnType(), false);
        }
    }
}
