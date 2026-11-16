/*
 * Arbace (overlay/jdk, F10 of doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F10a"): jrt's
 * sun.nio.ch.NativeThread. jdk26u's (unix, baf63fb) answers a platform thread's pthread to signal
 * out of a blocking system call, and a virtual thread's marker for the Poller. In jrt every thread
 * is a goroutine and a thread blocked in a descriptor waits in Go's poller, which the descriptor's
 * preClose wakes: current() is a token naming a thread (never 0, never jdk26u's virtual marker),
 * so that the channels' close calls preClose, and signal does nothing.
 * jdk26u's code: Copyright (c) Oracle and/or its affiliates, GPL 2 with the Classpath Exception
 * (LICENSE.md); the rest Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.nio.ch;

// Signalling operations on native threads

public class NativeThread {
    private static final long VIRTUAL_THREAD_ID = -1L;

    /**
     * Returns a token naming the current thread, never 0 (the platform can signal it: its
     * blocking operations are released by preClose) and never the virtual thread's id.
     */
    public static long current() {
        long id = Thread.currentThread().threadId();
        return id > 0 ? id : 1L;
    }

    /**
     * Signals the given native thread (nothing in jrt: preClose wakes it).
     *
     * @throws IllegalArgumentException if tid is not a token to a native thread
     */
    public static void signal(long tid) {
        if (!isNativeThread(tid))
            throw new IllegalArgumentException();
    }

    /**
     * Returns true the tid is the id of a native thread.
     */
    static boolean isNativeThread(long tid) {
        return (tid != 0 && tid != VIRTUAL_THREAD_ID);
    }

    /**
     * Returns true if tid is -1L.
     * @see #current()
     */
    static boolean isVirtualThread(long tid) {
        return (tid == VIRTUAL_THREAD_ID);
    }

    /**
     * Return true if the operating system supports pending signals. If a signal is sent
     * to a thread but cannot be delivered immediately then it will be delivered when the
     * thread is in the appropriate state.
     */
    static boolean supportPendingSignals() {
        return false;
    }
}
