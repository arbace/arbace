/*
 * Copyright (c) the Arbace authors. Eclipse Public License 1.0 (LICENSE.md).
 */
package sun.security.provider;

import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandomParameters;
import javax.crypto.KDFParameters;


/**
 * The implementations of the Go build's security services (doc/go/JRT-NOTES.md, "Security"):
 * Provider.Service.newInstance (a variant) asks here first. The JDK finds a service's class by
 * its name and calls its public constructor by reflection: the one taking the engine type's
 * constructor parameter class (SecureRandomParameters for SecureRandom) when it has one, else
 * the one without parameters (if the parameter is null). The Go build's closed world has no
 * reflection on the classes of non-exported packages (J2G-SPEC §10.6), so the services of the
 * SUN, SunJCE and SunRsaSign providers are constructed here (SunJCE's by
 * com.sun.crypto.provider.JceServices, SunRsaSign's by sun.security.rsa.RsaServices: the classes are
 * package-private; SunEC's own Provider.Service subclass constructs its services, as in the JDK),
 * directly, with the same choice of constructor (KDFParameters for the KDF engine, F1), the same
 * NoSuchMethodException when there is none, and whatever the constructor throws wrapped in an
 * InvocationTargetException, as Constructor.newInstance wraps it.
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
        } else if (paramClass == KDFParameters.class) {
            // the KDF engine's (SunJCE's HKDF classes, whose one constructor takes it)
            try {
                return com.sun.crypto.provider.JceServices.newKdfInstance(className, (KDFParameters) param);
            } catch (Throwable t) {
                throw new InvocationTargetException(t);
            }
        } else if (paramClass != null) {
            return null;
        }
        try {
            switch (className) {
                case "sun.security.provider.SecureRandom":
                    return new sun.security.provider.SecureRandom();
                case "sun.security.provider.DSA$SHA1withDSA":
                    return new DSA.SHA1withDSA();
                case "sun.security.provider.DSA$RawDSA":
                    return new DSA.RawDSA();
                case "sun.security.provider.DSA$SHA224withDSA":
                    return new DSA.SHA224withDSA();
                case "sun.security.provider.DSA$SHA256withDSA":
                    return new DSA.SHA256withDSA();
                case "sun.security.provider.DSA$SHA3_224withDSA":
                    return new DSA.SHA3_224withDSA();
                case "sun.security.provider.DSA$SHA3_256withDSA":
                    return new DSA.SHA3_256withDSA();
                case "sun.security.provider.DSA$SHA384withDSA":
                    return new DSA.SHA384withDSA();
                case "sun.security.provider.DSA$SHA512withDSA":
                    return new DSA.SHA512withDSA();
                case "sun.security.provider.DSA$SHA3_384withDSA":
                    return new DSA.SHA3_384withDSA();
                case "sun.security.provider.DSA$SHA3_512withDSA":
                    return new DSA.SHA3_512withDSA();
                case "sun.security.provider.DSA$SHA1withDSAinP1363Format":
                    return new DSA.SHA1withDSAinP1363Format();
                case "sun.security.provider.DSA$RawDSAinP1363Format":
                    return new DSA.RawDSAinP1363Format();
                case "sun.security.provider.DSA$SHA224withDSAinP1363Format":
                    return new DSA.SHA224withDSAinP1363Format();
                case "sun.security.provider.DSA$SHA256withDSAinP1363Format":
                    return new DSA.SHA256withDSAinP1363Format();
                case "sun.security.provider.DSA$SHA384withDSAinP1363Format":
                    return new DSA.SHA384withDSAinP1363Format();
                case "sun.security.provider.DSA$SHA512withDSAinP1363Format":
                    return new DSA.SHA512withDSAinP1363Format();
                case "sun.security.provider.DSA$SHA3_224withDSAinP1363Format":
                    return new DSA.SHA3_224withDSAinP1363Format();
                case "sun.security.provider.DSA$SHA3_256withDSAinP1363Format":
                    return new DSA.SHA3_256withDSAinP1363Format();
                case "sun.security.provider.DSA$SHA3_384withDSAinP1363Format":
                    return new DSA.SHA3_384withDSAinP1363Format();
                case "sun.security.provider.DSA$SHA3_512withDSAinP1363Format":
                    return new DSA.SHA3_512withDSAinP1363Format();
                case "sun.security.provider.HSS":
                    return new HSS();
                case "sun.security.provider.ML_DSA_Impls$SIG":
                    return new ML_DSA_Impls.SIG();
                case "sun.security.provider.ML_DSA_Impls$SIG2":
                    return new ML_DSA_Impls.SIG2();
                case "sun.security.provider.ML_DSA_Impls$SIG3":
                    return new ML_DSA_Impls.SIG3();
                case "sun.security.provider.ML_DSA_Impls$SIG5":
                    return new ML_DSA_Impls.SIG5();
                case "sun.security.provider.DSAKeyPairGenerator$Current":
                    return new DSAKeyPairGenerator.Current();
                case "sun.security.provider.DSAKeyPairGenerator$Legacy":
                    return new DSAKeyPairGenerator.Legacy();
                case "sun.security.provider.ML_DSA_Impls$KPG":
                    return new ML_DSA_Impls.KPG();
                case "sun.security.provider.ML_DSA_Impls$KPG2":
                    return new ML_DSA_Impls.KPG2();
                case "sun.security.provider.ML_DSA_Impls$KPG3":
                    return new ML_DSA_Impls.KPG3();
                case "sun.security.provider.ML_DSA_Impls$KPG5":
                    return new ML_DSA_Impls.KPG5();
                case "sun.security.provider.DSAParameterGenerator":
                    return new DSAParameterGenerator();
                case "sun.security.provider.DSAParameters":
                    return new DSAParameters();
                case "sun.security.provider.DSAKeyFactory":
                    return new DSAKeyFactory();
                case "sun.security.provider.HSS$KeyFactoryImpl":
                    return new HSS.KeyFactoryImpl();
                case "sun.security.provider.ML_DSA_Impls$KF":
                    return new ML_DSA_Impls.KF();
                case "sun.security.provider.ML_DSA_Impls$KF2":
                    return new ML_DSA_Impls.KF2();
                case "sun.security.provider.ML_DSA_Impls$KF3":
                    return new ML_DSA_Impls.KF3();
                case "sun.security.provider.ML_DSA_Impls$KF5":
                    return new ML_DSA_Impls.KF5();
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
                    Object o = com.sun.crypto.provider.JceServices.newInstance(className);
                    return o != null ? o : sun.security.rsa.RsaServices.newInstance(className);
            }
        } catch (Throwable t) {
            throw new InvocationTargetException(t);
        }
    }
}
