/*
 * Copyright (c) the Arbace authors. Eclipse Public License 1.0 (LICENSE.md).
 */
package sun.security.rsa;

/**
 * The SunRsaSign services of the Go build (doc/classes/JAVA-DEFERRED-TRIAGE.md, F1), constructed
 * directly for sun.security.provider.JrtServices, as com.sun.crypto.provider.JceServices does
 * SunJCE's: every service class jdk26u's SunRsaSignEntries names, in its order. Generated from
 * jdk26u's SunRsaSignEntries.java (baf63fb) by the F1 scratch script gensvc.py/genjava.py.
 */
public final class RsaServices {
    private RsaServices() {
    }

    /** A new instance of the SunRsaSign service class className, or null if it is not one. */
    public static Object newInstance(String className) throws Exception {
        switch (className) {
            case "sun.security.rsa.RSAKeyFactory$Legacy":
                return new RSAKeyFactory.Legacy();
            case "sun.security.rsa.RSAKeyPairGenerator$Legacy":
                return new RSAKeyPairGenerator.Legacy();
            case "sun.security.rsa.RSASignature$MD2withRSA":
                return new RSASignature.MD2withRSA();
            case "sun.security.rsa.RSASignature$MD5withRSA":
                return new RSASignature.MD5withRSA();
            case "sun.security.rsa.RSASignature$SHA1withRSA":
                return new RSASignature.SHA1withRSA();
            case "sun.security.rsa.RSASignature$SHA224withRSA":
                return new RSASignature.SHA224withRSA();
            case "sun.security.rsa.RSASignature$SHA256withRSA":
                return new RSASignature.SHA256withRSA();
            case "sun.security.rsa.RSASignature$SHA384withRSA":
                return new RSASignature.SHA384withRSA();
            case "sun.security.rsa.RSASignature$SHA512withRSA":
                return new RSASignature.SHA512withRSA();
            case "sun.security.rsa.RSASignature$SHA512_224withRSA":
                return new RSASignature.SHA512_224withRSA();
            case "sun.security.rsa.RSASignature$SHA512_256withRSA":
                return new RSASignature.SHA512_256withRSA();
            case "sun.security.rsa.RSASignature$SHA3_224withRSA":
                return new RSASignature.SHA3_224withRSA();
            case "sun.security.rsa.RSASignature$SHA3_256withRSA":
                return new RSASignature.SHA3_256withRSA();
            case "sun.security.rsa.RSASignature$SHA3_384withRSA":
                return new RSASignature.SHA3_384withRSA();
            case "sun.security.rsa.RSASignature$SHA3_512withRSA":
                return new RSASignature.SHA3_512withRSA();
            case "sun.security.rsa.RSAKeyFactory$PSS":
                return new RSAKeyFactory.PSS();
            case "sun.security.rsa.RSAKeyPairGenerator$PSS":
                return new RSAKeyPairGenerator.PSS();
            case "sun.security.rsa.RSAPSSSignature":
                return new RSAPSSSignature();
            case "sun.security.rsa.PSSParameters":
                return new PSSParameters();
            default:
                return null;
        }
    }
}
