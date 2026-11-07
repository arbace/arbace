/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F2, "As built: F2 (write-ahead)"): the
 * access objects PKI's classes give each other through jdk.internal.access.SharedSecrets in the
 * JDK, which jrt's SharedSecrets does not hold (as F1's jdk.internal.jrt.CryptoAccess for the
 * JCA's key specs): X500Principal's static initializer sets JavaxSecurityAccess (X500Name's
 * asX500Principal and asX500Name, read by sun.security.x509.X500Name) and Signature's sets
 * JavaSecuritySignatureAccess (initVerify and initSign with parameters, read by
 * sun.security.util.SignatureUtil), through the variants overlay/jdk/variants/X500Principal.ae and
 * SignatureAccess.ae; the readers' variants (X500Name.ae, SignatureUtil.ae) read them here. A
 * reader that runs before the setter's class is initialized initializes it first, as the JDK's
 * SharedSecrets does (ensureClassInitialized), by Class.forName.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import jdk.internal.access.JavaSecuritySignatureAccess;
import jdk.internal.access.JavaxSecurityAccess;

/** PKI's access objects (SharedSecrets' in the JDK). */
public final class PkiAccess {

    private static JavaxSecurityAccess javaxSecurityAccess;
    private static JavaSecuritySignatureAccess javaSecuritySignatureAccess;

    private PkiAccess() {
    }

    private static void initialize(String name) {
        try {
            Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new InternalError(e);
        }
    }

    /** Called once, by X500Principal's static initializer. */
    public static void setJavaxSecurityAccess(JavaxSecurityAccess access) {
        javaxSecurityAccess = access;
    }

    /** X500Principal's access object (X500Principal initialized first). */
    public static JavaxSecurityAccess javaxSecurityAccess() {
        JavaxSecurityAccess access = javaxSecurityAccess;
        if (access == null) {
            initialize("javax.security.auth.x500.X500Principal");
            access = javaxSecurityAccess;
        }
        return access;
    }

    /** Called once, by Signature's static initializer. */
    public static void setJavaSecuritySignatureAccess(JavaSecuritySignatureAccess access) {
        javaSecuritySignatureAccess = access;
    }

    /** Signature's access object (Signature initialized first). */
    public static JavaSecuritySignatureAccess javaSecuritySignatureAccess() {
        JavaSecuritySignatureAccess access = javaSecuritySignatureAccess;
        if (access == null) {
            initialize("java.security.Signature");
            access = javaSecuritySignatureAccess;
        }
        return access;
    }
}
