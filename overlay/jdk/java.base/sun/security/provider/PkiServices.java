/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F2 and F4, "As built: F2
 * (write-ahead)"): the Go build's PKI and login configuration services, constructed directly, as
 * jrt's JrtServices constructs SUN's others (Provider.Service.newInstanceUtil, a variant, asks
 * JrtServices, then here): SUN's X.509 certificate factory, key stores (PKCS12, JKS, CaseExactJKS,
 * DKS), cert stores (Collection, IndexedCollection), cert path builder and validator (PKIX) and
 * login configuration (JavaLoginConfig, F4), and SunJCE's JCEKS key store (F1 left it to F2). The
 * constructor is chosen as the JDK's reflection chooses it: the one taking the engine type's
 * constructor parameter class when the type has one (CertStoreParameters for CertStore,
 * Configuration.Parameters for Configuration), else the one without parameters; a NoSuchMethodException
 * when there is none, and whatever the constructor throws wrapped in an InvocationTargetException.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.security.provider;

import java.lang.reflect.InvocationTargetException;
import java.security.cert.CertStoreParameters;
import javax.security.auth.login.Configuration;

/** The Go build's PKI and login configuration services. */
public final class PkiServices {
    private PkiServices() {
    }

    /**
     * A new instance of the service class className, with constructor parameter class
     * paramClass (null for none) and parameter param; null when the class is not one of these.
     */
    public static Object newInstance(String className, Class<?> paramClass, Object param)
            throws InvocationTargetException, NoSuchMethodException {
        try {
            if (paramClass == CertStoreParameters.class) {
                CertStoreParameters p = (CertStoreParameters) param;
                switch (className) {
                    case "sun.security.provider.certpath.CollectionCertStore":
                        return new sun.security.provider.certpath.CollectionCertStore(p);
                    case "sun.security.provider.certpath.IndexedCollectionCertStore":
                        return new sun.security.provider.certpath.IndexedCollectionCertStore(p);
                    default:
                        return null;
                }
            }
            if (paramClass == Configuration.Parameters.class) {
                switch (className) {
                    case "sun.security.provider.ConfigFile$Spi":
                        return new ConfigFile.Spi((Configuration.Parameters) param);
                    default:
                        return null;
                }
            }
            if (paramClass != null) {
                return null;
            }
            switch (className) {
                case "sun.security.provider.X509Factory":
                    return new X509Factory();
                case "sun.security.pkcs12.PKCS12KeyStore$DualFormatPKCS12":
                    return new sun.security.pkcs12.PKCS12KeyStore.DualFormatPKCS12();
                case "sun.security.provider.JavaKeyStore$DualFormatJKS":
                    return new JavaKeyStore.DualFormatJKS();
                case "sun.security.provider.JavaKeyStore$CaseExactJKS":
                    return new JavaKeyStore.CaseExactJKS();
                case "sun.security.provider.DomainKeyStore$DKS":
                    return new DomainKeyStore.DKS();
                case "sun.security.provider.ConfigFile$Spi":
                    return new ConfigFile.Spi();
                case "sun.security.provider.certpath.SunCertPathBuilder":
                    return new sun.security.provider.certpath.SunCertPathBuilder();
                case "sun.security.provider.certpath.PKIXCertPathValidator":
                    return new sun.security.provider.certpath.PKIXCertPathValidator();
                case "com.sun.crypto.provider.JceKeyStore":
                    return new com.sun.crypto.provider.JceKeyStore();
                default:
                    return null;
            }
        } catch (Throwable t) {
            throw new InvocationTargetException(t);
        }
    }
}
