/*
 * jrt's own javax.crypto.JceSecurity for the Go build (doc/go/JRT-NOTES.md, "Security";
 * doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS233"). The JDK generates its JceSecurity
 * from JceSecurity.java.template, which reads the crypto jurisdiction policy files of
 * ${java.home}/conf/security/policy and verifies a provider by the signature of its jar. The Go
 * build has the java.base providers alone (SUN, SunRsaSign, SunEC, SunJCE, SunJSSE, SunSASL),
 * which the JDK trusts without verification (a class of the boot layer): no jar is verified here.
 * Since JCS233 (a) the policy is the JDK's: setupJurisdictionPolicies, getDefaultPolicy,
 * getExemptPolicy and isRestricted are the template's, @@JCE_DEFAULT_POLICY@@ replaced by
 * "unlimited" as the JDK build's UNLIMITED_CRYPTO default replaces it (make/modules/java.base/
 * Gensrc.gmk), over java.home's policy files (jrt's javahome.ae writes them from the embedded
 * data); when the Go build has no java.home (none could be written: the box), the policy is the
 * unlimited one, CryptoAllPermission, as before JCS233. The getInstance methods and
 * setupJurisdictionPolicies are transcribed from openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/javax/crypto/JceSecurity.java.template, Copyright (c) 1997, 2025,
 * Oracle and/or its affiliates, under the GNU General Public License version 2 with the
 * Classpath Exception (LICENSE.md); the rest is Arbace's own (Eclipse Public License 1.0).
 */
package javax.crypto;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.Security;
import java.util.Iterator;

import jdk.internal.util.StaticProperty;

import sun.security.jca.GetInstance;
import sun.security.jca.GetInstance.Instance;
import sun.security.util.Debug;

/**
 * The JCE's provider checks and jurisdiction policy: every provider of the Go build can be used
 * (no jar to verify); the policy is java.home's, the JDK's.
 */
final class JceSecurity {

    private static final Debug debug = Debug.getInstance("jca");

    // The defaultPolicy and exemptPolicy will be set up
    // in the static initializer.
    private static CryptoPermissions defaultPolicy = null;
    private static CryptoPermissions exemptPolicy = null;

    private static final boolean isRestricted;

    private JceSecurity() {
    }

    static {
        try {
            String javaHome = StaticProperty.javaHome();
            if (javaHome == null || javaHome.isEmpty()) {
                // no java.home in this Go build (javahome.ae could write none): the unlimited
                // policy, as jrt's JceSecurity before JCS233
                isRestricted = false;
            } else {
                setupJurisdictionPolicies();

                isRestricted = defaultPolicy.implies(
                    CryptoAllPermission.INSTANCE) ? false : true;
            }
        } catch (Exception e) {
            throw new SecurityException(
                    "Can not initialize cryptographic mechanism", e);
        }
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

    /**
     * The result of verifying provider p (null: verified), which Cipher.getInstance(transformation,
     * provider) asks (F1): every provider is, as canUseProvider says.
     */
    static Exception getVerificationResult(Provider p) {
        return null;
    }

    /** Every provider can be used: no provider jar to verify in a static executable. */
    static boolean canUseProvider(Provider p) {
        return true;
    }

    private static void setupJurisdictionPolicies() throws Exception {

        // Sanity check the crypto.policy Security property.  Single
        // directory entry, no pseudo-directories (".", "..", leading/trailing
        // path separators). normalize()/getParent() will help later.
        String cryptoPolicyProperty = Security.getProperty("crypto.policy");

        /*
         * In case no property is present, rather than fail catastrophically,
         * we at least try for a "sane" value, which is what we were
         * built with.  We first preprocess this file to plug in that
         * value, then compile the result gensrc.
         *
         * Log the warning first.
         */
        if (cryptoPolicyProperty == null) {
            cryptoPolicyProperty = "unlimited";
            if (debug != null) {
                debug.println(
                    "Security Property 'crypto.policy' not found: "
                    + "using '" + cryptoPolicyProperty + "' as fallback");
            }
        }

        Path cpPath = Paths.get(cryptoPolicyProperty);

        if ((cpPath.getNameCount() != 1) ||
                (cpPath.compareTo(cpPath.getFileName()) != 0)) {
            throw new SecurityException(
                "Invalid policy directory name format: " +
                cryptoPolicyProperty);
        }

        // Prepend java.home to get the full path.  normalize() in
        // case an extra "." or ".." snuck in somehow.
        String javaHomeProperty = StaticProperty.javaHome();
        Path javaHomePolicyPath = Paths.get(javaHomeProperty, "conf",
                "security", "policy").normalize();
        Path cryptoPolicyPath = Paths.get(javaHomeProperty, "conf", "security",
                "policy", cryptoPolicyProperty).normalize();

        if (cryptoPolicyPath.getParent().compareTo(javaHomePolicyPath) != 0) {
            throw new SecurityException(
                "Invalid cryptographic jurisdiction policy directory path: " +
                cryptoPolicyProperty);
        }

        if (!Files.isDirectory(cryptoPolicyPath)
                || !Files.isReadable(cryptoPolicyPath)) {
            throw new SecurityException(
                "Can't read cryptographic policy directory: " +
                cryptoPolicyProperty);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                cryptoPolicyPath, "{default,exempt}_*.policy")) {
            for (Path entry : stream) {
                try (InputStream is = new BufferedInputStream(
                        Files.newInputStream(entry))) {
                    String filename = entry.getFileName().toString();

                    CryptoPermissions tmpPerms = new CryptoPermissions();
                    tmpPerms.load(is);

                    if (filename.startsWith("default_")) {
                        // Did we find a default perms?
                        defaultPolicy = ((defaultPolicy == null) ? tmpPerms :
                                defaultPolicy.getMinimum(tmpPerms));
                    } else if (filename.startsWith("exempt_")) {
                        // Did we find a exempt perms?
                        exemptPolicy = ((exemptPolicy == null) ? tmpPerms :
                                exemptPolicy.getMinimum(tmpPerms));
                    } else {
                        // This should never happen.  newDirectoryStream
                        // should only throw return "{default,exempt}_*.policy"
                        throw new SecurityException(
                            "Unexpected jurisdiction policy files in : " +
                            cryptoPolicyProperty);
                    }
                } catch (Exception e) {
                    throw new SecurityException(
                        "Couldn't parse jurisdiction policy files in: " +
                        cryptoPolicyProperty);
                }
            }
        } catch (DirectoryIteratorException ex) {
            // I/O error encountered during the iteration,
            // the cause is an IOException
            throw new SecurityException(
                "Couldn't iterate through the jurisdiction policy files: " +
                cryptoPolicyProperty);
        }

        // Must have a default policy
        if ((defaultPolicy == null) || defaultPolicy.isEmpty()) {
            throw new SecurityException(
                "Missing mandatory jurisdiction policy files: " +
                cryptoPolicyProperty);
        }

        // If there was an empty exempt policy file, ignore it.
        if ((exemptPolicy != null) && exemptPolicy.isEmpty()) {
            exemptPolicy = null;
        }
    }

    /** The default policy: null without java.home (the unlimited policy, JceSecurityManager's). */
    static CryptoPermissions getDefaultPolicy() {
        return defaultPolicy;
    }

    static CryptoPermissions getExemptPolicy() {
        return exemptPolicy;
    }

    static boolean isRestricted() {
        return isRestricted;
    }
}
