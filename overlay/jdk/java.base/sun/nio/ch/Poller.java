/*
 * Arbace (overlay/jdk, F10 of doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F10a"): jrt's
 * sun.nio.ch.Poller, the members jdk26u's channels call (poll, stopPoll). jdk26u's Poller parks a
 * virtual thread and registers its descriptor with a poller thread (epoll); in jrt a virtual
 * thread is a goroutine like any other, so it waits in Go's poller as a platform thread does
 * (HostChannels.poll0), and stopPoll wakes it as preClose does.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.nio.ch;

import java.io.IOException;
import java.util.function.BooleanSupplier;
import jdk.internal.jrt.HostChannels;

/**
 * Polls file descriptors for the current thread.
 */
public abstract class Poller {

    private Poller() { }

    /**
     * Parks the current thread until a file descriptor is ready for the given op.
     * @param fdVal the file descriptor
     * @param event POLLIN or POLLOUT
     * @param nanos the waiting time or 0 to wait indefinitely
     * @param isOpen supplies a boolean to indicate if the enclosing object is open
     */
    public static void poll(int fdVal, int event, long nanos, BooleanSupplier isOpen)
        throws IOException
    {
        assert nanos >= 0L;
        if (!isOpen.getAsBoolean())
            return;
        long millis = -1;
        if (nanos > 0) {
            millis = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(nanos);
            if (nanos > java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(millis))
                millis++;
        }
        int[] err = new int[1];
        if (HostChannels.poll0(fdVal, event, millis, err) == HostChannels.THROWN)
            throw HostChannels.socketException(err[0], null);
    }

    /**
     * Parks the current thread until a Selector's file descriptor is ready.
     * @param fdVal the Selector's file descriptor
     * @param nanos the waiting time or 0 to wait indefinitely
     */
    public static void pollSelector(int fdVal, long nanos) throws IOException {
        poll(fdVal, Net.POLLIN, nanos, () -> true);
    }

    /**
     * Unpark the given thread so that it stops polling.
     */
    public static void stopPoll(int fdVal, int event) {
        HostChannels.preClose0(fdVal);
    }

    /**
     * Unpark the given thread so that it stops polling.
     */
    public static void stopPoll(int fdVal) {
        HostChannels.preClose0(fdVal);
    }
}
