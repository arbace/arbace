/*
 * Arbace (overlay/jdk, doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JC6b"): jrt's
 * java.util.jar.JarVerifier, written for jrt in place of jdk26u's, which verifies a signed jar's
 * entries against its signature files through sun.security's PKCS #7, certificate and digest
 * classes (outside the Go build's world: security is deferred, JCS90 (b)). This one verifies
 * nothing: every jar is read as unsigned (nothingToVerify), as the JDK reads a jar whose
 * signature files it cannot use, so JarInputStream and Manifest, which hold a verifier, run
 * jdk26u's code unchanged. Its members are those jdk26u's java.util.jar classes use; the
 * constants are jdk26u's (https://github.com/openjdk/jdk26u at baf63fb).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.util.jar;

import sun.security.util.Debug;
import sun.security.util.ManifestEntryVerifier;

/** A jar's signature verifier that verifies nothing (the Go build reads jars as unsigned). */
class JarVerifier {

    public static final String MULTIPLE_MANIFEST_WARNING =
            "WARNING: Multiple MANIFEST.MF found. Treat JAR file as unsigned.";

    static final Debug debug = Debug.getInstance("jar");

    /** The manifest's name and bytes, as the JDK's verifier keeps them. */
    final String manifestName;
    final byte[] manifestRawBytes;

    JarVerifier(String name, byte[] rawBytes) {
        manifestName = name;
        manifestRawBytes = rawBytes;
    }

    /** Nothing to do: no entry is verified. */
    public void beginEntry(JarEntry je, ManifestEntryVerifier mev) {
    }

    /** Nothing to do: no entry is verified. */
    public void update(int b, ManifestEntryVerifier mev) {
    }

    /** Nothing to do: no entry is verified. */
    public void update(int n, byte[] b, int off, int len, ManifestEntryVerifier mev) {
    }

    /** No manifest entry is trusted: there are no signers. */
    boolean isTrustedManifestEntry(String name) {
        return false;
    }

    /** Always: the jar is read as unsigned. */
    boolean nothingToVerify() {
        return true;
    }

    /** Nothing to do. */
    void doneWithMeta() {
    }
}
