/*
 * Arbace (overlay/jdk, doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JC6b"): jrt's
 * sun.security.util.ManifestEntryVerifier, written for jrt in place of jdk26u's, which digests a
 * jar's entries for java.util.jar's verifier: the Go build reads jars as unsigned (jrt's
 * JarVerifier verifies nothing), so this holds the manifest alone and digests nothing; its
 * constructor is the one java.util.jar.JarInputStream calls.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.security.util;

import java.util.jar.Manifest;

/** The digests of a jar's entries, which the Go build does not compute. */
public class ManifestEntryVerifier {

    private final Manifest man;
    private final String manifestFileName;

    public ManifestEntryVerifier(Manifest man, String manifestFileName) {
        this.man = man;
        this.manifestFileName = manifestFileName;
    }
}
