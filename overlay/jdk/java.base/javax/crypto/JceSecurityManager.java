/*
 * jrt's own javax.crypto.JceSecurityManager for the Go build (doc/classes/JAVA-DEFERRED-TRIAGE.md, F1,
 * "As built: JCS233"; doc/go/JRT-NOTES.md, "Security"). The JDK's consults the crypto policy
 * (JceSecurity's, java.home's policy files since JCS233) and the application's exemption
 * permissions (the cryptoperms file of a signed jar, found by the caller's code base, a
 * StackWalker), and checks that the caller of Cipher's protected constructor is a trusted provider
 * by its code base (JceSecurity.verifyProvider). The Go build has no jars, so no class has a code
 * base (JceSecurity.getCodeBase is null for every class in the JDK too when it has none): the
 * permission is the default policy's for the algorithm, getDefaultPermission as the JDK's (the
 * unlimited policy's CryptoAllPermission when JceSecurity has no policy: no java.home), and every
 * caller is trusted (an OpenJDK build trusts a provider whose jar is unsigned as well: its
 * verification succeeds). getCryptoPermission's and getDefaultPermission's code is jdk26u's but
 * the code-base walk. WALKER keeps the JDK's
 * shape for Cipher's constructor (WALKER.getCallerClass()), without a StackWalker, which the Go
 * build has not. Copyright (c) the Arbace authors; Eclipse Public License 1.0 (LICENSE.md); the
 * names and signatures are jdk26u's (baf63fb, src/java.base/share/classes/javax/crypto/
 * JceSecurityManager.java, Copyright (c) 1999, 2025, Oracle and/or its affiliates, GPL 2 with the
 * Classpath Exception).
 */
package javax.crypto;

import java.security.Permission;
import java.security.Provider;
import java.util.Enumeration;
import java.util.Locale;

/** The JCE security manager: the default policy's permissions, every caller trusted. */
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

    private static final CryptoPermissions defaultPolicy = JceSecurity.getDefaultPolicy();

    // singleton instance
    static final JceSecurityManager INSTANCE = new JceSecurityManager();
    static final Walker WALKER = new Walker();

    private JceSecurityManager() {
        // empty
    }

    /**
     * Returns the maximum allowable crypto strength for the given
     * application, for the given algorithm: the default policy's (no caller has a code base, so
     * no application's exemption permissions apply, as the JDK's walk finds for such a caller).
     */
    CryptoPermission getCryptoPermission(String theAlg) {

        // Need to convert to uppercase since the crypto perm
        // lookup is case sensitive.
        final String alg = theAlg.toUpperCase(Locale.ENGLISH);

        // If CryptoAllPermission is granted by default, we return that.
        // Otherwise, this will be the permission we return if anything goes
        // wrong.
        return getDefaultPermission(alg);
    }

    /**
     * Returns the default permission for the given algorithm (CryptoAllPermission without a
     * policy: no java.home).
     */
    private CryptoPermission getDefaultPermission(String alg) {
        if (defaultPolicy == null) {
            return CryptoAllPermission.INSTANCE;
        }
        Enumeration<Permission> enum_ =
            defaultPolicy.getPermissionCollection(alg).elements();
        return (CryptoPermission)enum_.nextElement();
    }

    /** Every caller of Cipher's protected constructor is trusted (no provider jar to verify). */
    boolean isCallerTrusted(Class<?> caller, Provider provider) {
        return true;
    }
}
