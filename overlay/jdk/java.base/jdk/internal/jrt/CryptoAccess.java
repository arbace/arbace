/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F1, "As built: F1 (write-ahead)"): the
 * access objects the JCA's classes give each other through jdk.internal.access.SharedSecrets in
 * the JDK, which jrt's SharedSecrets does not hold (as jdk.internal.jrt.ZipAccess for zip and
 * jar): SecretKeySpec's static initializer sets JavaxCryptoSpecAccess (clearSecretKeySpec, read
 * by SunJCE's PBES2, PKCS #12 and PBMAC1 MACs, HKDF and KeyUtil) and EncodedKeySpec's sets
 * JavaSecuritySpecAccess (clearEncodedKeySpec, read by ConstructKeys, PKCS8Key, ECUtil and
 * KeyUtil), through the variants overlay/jdk/variants/SecretKeySpec.ae and EncodedKeySpec.ae; the
 * readers' variants (PBES2Core.ae, HmacPKCS12PBECore.ae, PBMAC1Core.ae, HKDFKeyDerivation.ae,
 * KeyUtil.ae, ConstructKeys.ae, PKCS8Key.ae, ECUtil.ae) read them here. A reader that runs before
 * the setter's class is initialized initializes it first, as the JDK's SharedSecrets does
 * (ensureClassInitialized), by touching the class.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import javax.crypto.spec.SecretKeySpec;

import jdk.internal.access.JavaSecuritySpecAccess;
import jdk.internal.access.JavaxCryptoSpecAccess;

/** The JCA's access objects (SharedSecrets' in the JDK). */
public final class CryptoAccess {

    private static JavaxCryptoSpecAccess javaxCryptoSpecAccess;
    private static JavaSecuritySpecAccess javaSecuritySpecAccess;

    private CryptoAccess() {
    }

    /** Called once, by SecretKeySpec's static initializer. */
    public static void setJavaxCryptoSpecAccess(JavaxCryptoSpecAccess access) {
        javaxCryptoSpecAccess = access;
    }

    /** SecretKeySpec's access object (SecretKeySpec initialized first). */
    public static JavaxCryptoSpecAccess javaxCryptoSpecAccess() {
        JavaxCryptoSpecAccess access = javaxCryptoSpecAccess;
        if (access == null) {
            // initializes SecretKeySpec, whose static initializer sets the access
            new SecretKeySpec(new byte[1], "RAW");
            access = javaxCryptoSpecAccess;
        }
        return access;
    }

    /** Called once, by EncodedKeySpec's static initializer. */
    public static void setJavaSecuritySpecAccess(JavaSecuritySpecAccess access) {
        javaSecuritySpecAccess = access;
    }

    /** EncodedKeySpec's access object (EncodedKeySpec initialized first). */
    public static JavaSecuritySpecAccess javaSecuritySpecAccess() {
        JavaSecuritySpecAccess access = javaSecuritySpecAccess;
        if (access == null) {
            // initializes EncodedKeySpec (through a subclass), whose static initializer sets it
            new java.security.spec.X509EncodedKeySpec(new byte[0]);
            access = javaSecuritySpecAccess;
        }
        return access;
    }
}
