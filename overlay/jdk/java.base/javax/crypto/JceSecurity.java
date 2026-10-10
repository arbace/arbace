/*
 * jrt's own javax.crypto.JceSecurity for the Go build (doc/go/JRT-NOTES.md, "Security"). The
 * JDK generates its JceSecurity from JceSecurity.java.template, which reads the crypto policy
 * files of $JAVA_HOME/conf/security/policy and verifies a provider by the signature of its jar.
 * The Go build has the java.base providers alone (SUN and SunJCE), which the JDK trusts without
 * verification (a class of the boot layer), and the unlimited policy, the JDK's default. Its
 * getInstance methods are transcribed from openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/javax/crypto/JceSecurity.java.template, Copyright (c) 1997, 2025,
 * Oracle and/or its affiliates, under the GNU General Public License version 2 with the
 * Classpath Exception (LICENSE.md); the rest is Arbace's own (Eclipse Public License 1.0).
 */
package javax.crypto;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Provider.Service;
import java.util.Iterator;

import sun.security.jca.GetInstance;
import sun.security.jca.GetInstance.Instance;

/**
 * The JCE's provider checks: every provider of the Go build can be used (no jar to verify), and
 * the policy is unlimited (isRestricted is false).
 */
final class JceSecurity {

    private JceSecurity() {
    }

    static Instance getInstance(String type, Class<?> clazz, String algorithm,
            String provider) throws NoSuchAlgorithmException,
            NoSuchProviderException {
        Service s = GetInstance.getService(type, algorithm, provider);
        return GetInstance.getInstance(s, clazz);
    }

    static Instance getInstance(String type, Class<?> clazz, String algorithm,
            Provider provider) throws NoSuchAlgorithmException {
        Service s = GetInstance.getService(type, algorithm, provider);
        return GetInstance.getInstance(s, clazz);
    }

    static Instance getInstance(String type, Class<?> clazz, String algorithm)
            throws NoSuchAlgorithmException {
        Iterator<Service> t = GetInstance.getServices(type, algorithm);
        NoSuchAlgorithmException failure = null;
        while (t.hasNext()) {
            Service s = t.next();
            if (canUseProvider(s.getProvider()) == false) {
                // allow only signed providers
                continue;
            }
            try {
                Instance instance = GetInstance.getInstance(s, clazz);
                return instance;
            } catch (NoSuchAlgorithmException e) {
                failure = e;
            }
        }
        throw new NoSuchAlgorithmException("Algorithm " + algorithm
                + " not available", failure);
    }

    /** Every provider can be used: no provider jar to verify in a static executable. */
    static boolean canUseProvider(Provider p) {
        return true;
    }

    /** The unlimited policy, the JDK's default (crypto.policy=unlimited). */
    static boolean isRestricted() {
        return false;
    }
}
