/*
 * Copyright (c) the Arbace authors. Eclipse Public License 1.0 (LICENSE.md).
 */
package sun.security.provider;

import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandomParameters;


/**
 * The implementations of the Go build's security services (doc/go/JRT-NOTES.md, "Security"):
 * Provider.Service.newInstance (a variant) asks here first. The JDK finds a service's class by
 * its name and calls its public constructor by reflection: the one taking the engine type's
 * constructor parameter class (SecureRandomParameters for SecureRandom) when it has one, else
 * the one without parameters (if the parameter is null). The Go build's closed world has no
 * reflection on the classes of non-exported packages (J2G-SPEC §10.6), so the services of the
 * SUN and SunJCE providers are constructed here (SunJCE's by com.sun.crypto.provider.JceServices:
 * the classes are package-private), directly, with the same choice of
 * constructor, the same NoSuchMethodException when there is none, and whatever the constructor
 * throws wrapped in an InvocationTargetException, as Constructor.newInstance wraps it.
 */
public final class JrtServices {
    private JrtServices() {
    }

    /**
     * A new instance of the service class className, with constructor parameter class
     * paramClass (null for none) and parameter param; null when the class is not one of the Go
     * build's services (the caller then uses reflection).
     */
    public static Object newInstance(String className, Class<?> paramClass, Object param)
            throws InvocationTargetException, NoSuchMethodException {
        if (paramClass == SecureRandomParameters.class) {
            SecureRandomParameters p = (SecureRandomParameters) param;
            try {
                switch (className) {
                    case "sun.security.provider.NativePRNG":
                        return new NativePRNG(p);
                    case "sun.security.provider.NativePRNG$Blocking":
                        return new NativePRNG.Blocking(p);
                    case "sun.security.provider.NativePRNG$NonBlocking":
                        return new NativePRNG.NonBlocking(p);
                    case "sun.security.provider.DRBG":
                        return new DRBG(p);
                    case "sun.security.provider.SecureRandom":
                        break; // no constructor with parameters: the one without (below)
                    default:
                        return null;
                }
            } catch (Throwable t) {
                throw new InvocationTargetException(t);
            }
            if (param != null) {
                throw new NoSuchMethodException(className + ".<init>(" + paramClass.getName() + ")");
            }
        } else if (paramClass != null) {
            return null;
        }
        try {
            switch (className) {
                case "sun.security.provider.SecureRandom":
                    return new sun.security.provider.SecureRandom();
                case "sun.security.provider.MD2":
                    return new MD2();
                case "sun.security.provider.MD5":
                    return new MD5();
                case "sun.security.provider.SHA":
                    return new SHA();
                case "sun.security.provider.SHA2$SHA224":
                    return new SHA2.SHA224();
                case "sun.security.provider.SHA2$SHA256":
                    return new SHA2.SHA256();
                case "sun.security.provider.SHA5$SHA384":
                    return new SHA5.SHA384();
                case "sun.security.provider.SHA5$SHA512":
                    return new SHA5.SHA512();
                case "sun.security.provider.SHA5$SHA512_224":
                    return new SHA5.SHA512_224();
                case "sun.security.provider.SHA5$SHA512_256":
                    return new SHA5.SHA512_256();
                case "sun.security.provider.SHA3$SHA224":
                    return new SHA3.SHA224();
                case "sun.security.provider.SHA3$SHA256":
                    return new SHA3.SHA256();
                case "sun.security.provider.SHA3$SHA384":
                    return new SHA3.SHA384();
                case "sun.security.provider.SHA3$SHA512":
                    return new SHA3.SHA512();
                case "sun.security.provider.SHA3$SHAKE128Hash":
                    return new SHA3.SHAKE128Hash();
                case "sun.security.provider.SHA3$SHAKE256Hash":
                    return new SHA3.SHAKE256Hash();
                default:
                    return com.sun.crypto.provider.JceServices.newInstance(className);
            }
        } catch (Throwable t) {
            throw new InvocationTargetException(t);
        }
    }
}
