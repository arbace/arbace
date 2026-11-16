/*
 * jrt's own javax.crypto.JceSecurityManager for the Go build (doc/classes/JAVA-DEFERRED-TRIAGE.md, F1;
 * doc/go/JRT-NOTES.md, "Security"). The JDK's consults the crypto policy (jrt's JceSecurity: the
 * unlimited policy, no policy files) and the application's exemption permissions (the
 * cryptoperms file of a signed jar), and checks that the caller of Cipher's protected constructor
 * is a trusted provider by its code base (a StackWalker; JceSecurity.verifyProvider). The Go build
 * has the unlimited policy and no provider jars (every provider can be used: jrt's JceSecurity), so
 * the permission is always CryptoAllPermission and every caller is trusted (an OpenJDK build trusts
 * a provider whose jar is unsigned as well: its verification succeeds). WALKER keeps the JDK's
 * shape for Cipher's constructor (WALKER.getCallerClass()), without a StackWalker, which the Go
 * build has not. Copyright (c) the Arbace authors; Eclipse Public License 1.0 (LICENSE.md); the
 * names and signatures are jdk26u's (baf63fb, src/java.base/share/classes/javax/crypto/
 * JceSecurityManager.java, Copyright (c) 1999, 2025, Oracle and/or its affiliates, GPL 2 with the
 * Classpath Exception).
 */
package javax.crypto;

import java.security.Provider;

/** The JCE security manager: the unlimited policy, every caller trusted. */
final class JceSecurityManager {

    /** The caller lookup Cipher's protected constructor makes (the JDK's StackWalker). */
    static final class Walker {
        private Walker() {
        }

        /** No caller class: isCallerTrusted does not consult it. */
        Class<?> getCallerClass() {
            return null;
        }
    }

    // singleton instance
    static final JceSecurityManager INSTANCE = new JceSecurityManager();
    static final Walker WALKER = new Walker();

    private JceSecurityManager() {
        // empty
    }

    /**
     * The maximum allowable crypto strength for the given algorithm: CryptoAllPermission, the
     * unlimited policy's (the JDK's answer when CryptoAllPermission is granted by default).
     */
    CryptoPermission getCryptoPermission(String theAlg) {
        return CryptoAllPermission.INSTANCE;
    }

    /** Every caller of Cipher's protected constructor is trusted (no provider jar to verify). */
    boolean isCallerTrusted(Class<?> caller, Provider provider) {
        return true;
    }
}
