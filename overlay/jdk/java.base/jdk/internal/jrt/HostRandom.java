/*
 * Copyright (c) the Arbace authors. Eclipse Public License 1.0 (LICENSE.md).
 */
package jdk.internal.jrt;

import java.io.InputStream;
import java.util.Objects;

/**
 * The host's random source (doc/go/JRT-NOTES.md, "Security"): the bytes of jrt's Host.RandomBytes
 * (B1a's OSHost: Go's crypto/rand, getrandom(2) on Linux), as an endless stream. The Go build's
 * NativePRNG (a variant) reads its "seed" and "next" files from it, and its SeedGenerator (a
 * variant) its seeds, where the JDK reads /dev/random and /dev/urandom: the box (B1b) has no
 * such files, and getrandom(2) is what the JDK's own Linux code would prefer.
 */
public final class HostRandom extends InputStream {

    /** The stream every reader shares (it has no state). */
    public static final HostRandom INSTANCE = new HostRandom();

    private HostRandom() {
    }

    /** Fills b[off, off+len) with random bytes from the host. */
    public static void fill(byte[] b, int off, int len) {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len > 0) {
            fill0(b, off, len);
        }
    }

    /** A new array of n random bytes from the host. */
    public static byte[] bytes(int n) {
        byte[] b = new byte[n];
        fill0(b, 0, n);
        return b;
    }

    private static native void fill0(byte[] b, int off, int len);

    @Override
    public int read() {
        byte[] b = new byte[1];
        fill0(b, 0, 1);
        return b[0] & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len) {
        fill(b, off, len);
        return len;
    }

    @Override
    public int available() {
        return Integer.MAX_VALUE;
    }
}
