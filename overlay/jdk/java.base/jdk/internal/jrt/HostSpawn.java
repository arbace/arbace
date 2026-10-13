/*
 * Copyright (c) the Arbace authors. Eclipse Public License 1.0 (LICENSE.md).
 */
package jdk.internal.jrt;

/**
 * Tool guests (doc/go/BOX-NOTES.md, S7): a guest of the box runs another guest, its image a
 * blob of the store, by a request blob (an EDN map: the image, the command line, the
 * environment, the input trees, the output directory, the RAM and vCPUs asked); the answer is
 * a result blob (the exit code, the output tree, the console). In the box the monitor runs
 * them (its calls Spawn and SpawnPoll, under the launcher's ARBACE_BOX_SPAWN); in the native
 * executable a launcher process does, the one ARBACE_BOX_LAUNCHER names, with the store
 * ARBACE_BOX_STORE names. jrt's host answers (go/arbace/jrt/spawn.ae, the optional SpawnHost);
 * the namespace arbace.box is its user.
 */
public final class HostSpawn {

    private HostSpawn() {
    }

    /** Done. */
    public static final int OK = 0;
    /** Refused: no spawning here, no such request or handle, or a guest that could not run. */
    public static final int REFUSED = -1;
    /** poll: the guest still runs. */
    public static final int RUNNING = -11;

    /** Starts the guest the request blob hash names: a handle (0 up), or REFUSED. */
    public static long start(byte[] request) {
        checkHash(request);
        return start0(request);
    }

    /**
     * Handle h's end: OK once the guest has ended, its result's hash written to result and the
     * handle released; RUNNING while it runs; REFUSED.
     */
    public static int poll(long h, byte[] result) {
        checkHash(result);
        return poll0(h, result);
    }

    /** Waits for handle h's end: OK, its result's hash written to result, or REFUSED. */
    public static int await(long h, byte[] result) {
        checkHash(result);
        return await0(h, result);
    }

    private static void checkHash(byte[] hash) {
        if (hash.length != HostStore.HASH_BYTES) {
            throw new IllegalArgumentException("a hash is 32 bytes, not " + hash.length);
        }
    }

    private static native long start0(byte[] request);

    private static native int poll0(long h, byte[] result);

    private static native int await0(long h, byte[] result);
}
