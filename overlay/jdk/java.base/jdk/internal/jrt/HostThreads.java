/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS162"; JCS162 (a)): what
 * java.nio.channels asks of jrt's Thread (go/arbace/jrt/thread.ae, hand-written) in place of jdk26u's
 * JavaLangAccess.blockedOn: the current thread's nioBlocker, which Thread.interrupt calls (the
 * Interruptible's interrupt(Thread) under the thread's interrupt lock, then postInterrupt).
 * AbstractInterruptibleChannel's variant (overlay/jdk/variants/AbstractInterruptibleChannel.ae) calls it.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

/** The thread's blocker, for interrupts of I/O. */
public final class HostThreads {

    private HostThreads() {
    }

    /**
     * Sets the current thread's blocker (a sun.nio.ch.Interruptible), or clears it (null): jdk26u's
     * Thread.blockedOn.
     */
    public static native void blockedOn(Object b);
}
