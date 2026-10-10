/*
 * jrt's own sun.security.provider.SeedGenerator for the Go build (doc/go/JRT-NOTES.md,
 * "Security"). The JDK's chooses a seed generator by securerandom.source (file:/dev/random on
 * Linux: NativeSeedGenerator, which reads the file, a URLSeedGenerator) or falls back to
 * ThreadedSeedGenerator (thread scheduling jitter); the Go build's seeds come from the host's
 * random source (jdk.internal.jrt.HostRandom: getrandom(2) through Go's crypto/rand in B1a),
 * what the JDK's Linux generator reads without the file (the box, B1b, has no /dev). The
 * system entropy (SHA1PRNG's additional seed material, not observable) is the JDK's but for
 * the network adapters (java.net.NetworkInterface is not in the Go build), hashes a few system
 * properties rather than all (jrt has no System.getProperties() for translated code) and lists
 * the temporary directory with File.list (jrt's java.nio.file has no directory streams).
 * getSystemEntropy and longToByteArray are transcribed from openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/sun/security/provider/SeedGenerator.java, Copyright (c) 1996, 2024,
 * Oracle and/or its affiliates, under the GNU General Public License version 2 with the
 * Classpath Exception (LICENSE.md); the rest is Arbace's own (Eclipse Public License 1.0).
 */
package sun.security.provider;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import jdk.internal.jrt.HostRandom;

/**
 * Seeds for the SHA1PRNG and the DRBG's entropy, from the host's random source.
 */
abstract class SeedGenerator {

    private SeedGenerator() {
    }

    /**
     * Fill result with bytes from the host's random source.
     */
    public static void generateSeed(byte[] result) {
        HostRandom.fill(result, 0, result.length);
    }

    /**
     * Retrieve some system information, hashed.
     */
    static byte[] getSystemEntropy() {
        final MessageDigest md;

        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException nsae) {
            throw new InternalError("internal error: SHA-1 not available.",
                    nsae);
        }

        // The current time in millis
        byte b =(byte)System.currentTimeMillis();
        md.update(b);

        try {
            // The JDK hashes every system property here; jrt has no
            // System.getProperties() for translated code: a few of them
            for (String s: new String[] {"java.version", "os.name", "os.arch",
                    "user.name", "user.home", "user.dir", "java.io.tmpdir"}) {
                String v = System.getProperty(s);
                md.update(s.getBytes());
                if (v != null) {
                    md.update(v.getBytes());
                }
            }

            // The temporary dir
            String[] names = new File(System.getProperty("java.io.tmpdir", "/tmp")).list();
            if (names != null) {
                int count = 0;
                // We use a Random object to choose what file names
                // should be used. Otherwise, on a machine with too
                // many files, the same first 1024 files always get
                // used. Any, We make sure the first 512 files are
                // always used.
                Random r = new Random();
                for (String name : names) {
                    if (count < 512 || r.nextBoolean()) {
                        md.update(name.getBytes());
                    }
                    if (count++ > 1024) {
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            md.update((byte)ex.hashCode());
        }

        // get Runtime memory stats
        Runtime rt = Runtime.getRuntime();
        byte[] memBytes = longToByteArray(rt.totalMemory());
        md.update(memBytes, 0, memBytes.length);
        memBytes = longToByteArray(rt.freeMemory());
        md.update(memBytes, 0, memBytes.length);

        return md.digest();
    }

    /**
     * Helper function to convert a long into a byte array (least significant
     * byte first).
     */
    private static byte[] longToByteArray(long l) {
        byte[] retVal = new byte[8];

        for (int i=0; i<8; i++) {
            retVal[i] = (byte) l;
            l >>= 8;
        }

        return retVal;
    }
}
