/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F3, "As built: F3 (write-ahead)"): the
 * Go build's SunJSSE services, constructed directly, as jrt's JrtServices and PkiServices construct
 * SUN's and SunJCE's (Provider.Service.newInstanceUtil, a variant, asks JrtServices, PkiServices,
 * then here): the classes SunJSSE.registerAlgorithms names, MD5andSHA1withRSA's signature, the
 * SunX509 and PKIX key and trust manager factories, and the TLS, DTLS and Default SSL contexts.
 * None takes a constructor parameter (the engine types SSLContext, KeyManagerFactory and
 * TrustManagerFactory have none); whatever the constructor throws is wrapped in an
 * InvocationTargetException, as the JDK's reflection wraps it.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.security.ssl;

import java.lang.reflect.InvocationTargetException;

/** The Go build's SunJSSE services. */
public final class JsseServices {
    private JsseServices() {
    }

    /**
     * A new instance of the service class className, with constructor parameter class
     * paramClass (null for none) and parameter param; null when the class is not one of these.
     */
    public static Object newInstance(String className, Class<?> paramClass, Object param)
            throws InvocationTargetException {
        if (paramClass != null || !className.startsWith("sun.security.ssl.")) {
            return null;
        }
        try {
            switch (className) {
                case "sun.security.ssl.RSASignature":
                    return new RSASignature();
                case "sun.security.ssl.KeyManagerFactoryImpl$SunX509":
                    return new KeyManagerFactoryImpl.SunX509();
                case "sun.security.ssl.KeyManagerFactoryImpl$X509":
                    return new KeyManagerFactoryImpl.X509();
                case "sun.security.ssl.TrustManagerFactoryImpl$SimpleFactory":
                    return new TrustManagerFactoryImpl.SimpleFactory();
                case "sun.security.ssl.TrustManagerFactoryImpl$PKIXFactory":
                    return new TrustManagerFactoryImpl.PKIXFactory();
                case "sun.security.ssl.SSLContextImpl$TLS10Context":
                    return new SSLContextImpl.TLS10Context();
                case "sun.security.ssl.SSLContextImpl$TLS11Context":
                    return new SSLContextImpl.TLS11Context();
                case "sun.security.ssl.SSLContextImpl$TLS12Context":
                    return new SSLContextImpl.TLS12Context();
                case "sun.security.ssl.SSLContextImpl$TLS13Context":
                    return new SSLContextImpl.TLS13Context();
                case "sun.security.ssl.SSLContextImpl$TLSContext":
                    return new SSLContextImpl.TLSContext();
                case "sun.security.ssl.SSLContextImpl$DTLS10Context":
                    return new SSLContextImpl.DTLS10Context();
                case "sun.security.ssl.SSLContextImpl$DTLS12Context":
                    return new SSLContextImpl.DTLS12Context();
                case "sun.security.ssl.SSLContextImpl$DTLSContext":
                    return new SSLContextImpl.DTLSContext();
                case "sun.security.ssl.SSLContextImpl$DefaultSSLContext":
                    return new SSLContextImpl.DefaultSSLContext();
                default:
                    return null;
            }
        } catch (Throwable t) {
            throw new InvocationTargetException(t);
        }
    }
}
