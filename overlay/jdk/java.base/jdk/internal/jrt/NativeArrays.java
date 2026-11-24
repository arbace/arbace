/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, java-design-1122): native memory seen as a Java array.
 * jrt's native memory (jdk.internal.misc.Unsafe's allocateMemory, go/arbace/jrt/nativemem.ae) is a Go byte slice per
 * block, named by an address; a buffer over it (JavaNioAccess.newDirectByteBuffer: a native memory segment's
 * asByteBuffer, java.lang.foreign) is jrt's direct buffer, a heap buffer (F7), whose array here shares the block's
 * bytes: a write through either is seen by the other. Its native is jrt's Go (nativemem.ae).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

/** Native memory as a byte array sharing its bytes. */
public final class NativeArrays {

    private NativeArrays() {}

    /**
     * The length bytes of native memory at address (within one Unsafe.allocateMemory block; length 0 any address) as
     * a byte array sharing them.
     */
    public static byte[] bytes(long address, int length) {
        if (length < 0)
            throw new IllegalArgumentException("negative length: " + length);
        return bytes0(address, length);
    }

    private static native byte[] bytes0(long address, int length);
}
