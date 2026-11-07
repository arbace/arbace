/*
 * Arbace (overlay/jdk, F10 of doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F10a"): jrt's
 * sun.nio.ch.UnixDispatcher, jdk26u's (baf63fb) with its natives over jdk.internal.jrt.HostChannels:
 * close0 closes the descriptor; preClose0 is UnixDispatcher.c's dup2 of a closed socket over the
 * descriptor, done by the host as a mark that also wakes a thread blocked in it (no signal is
 * needed: NativeThread.signal does nothing in jrt, and SUPPORTS_PENDING_SIGNALS is false).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import jdk.internal.jrt.HostChannels;

abstract class UnixDispatcher extends NativeDispatcher {
    private static final boolean SUPPORTS_PENDING_SIGNALS = NativeThread.supportPendingSignals();

    @Override
    void close(FileDescriptor fd) throws IOException {
        close0(fd);
    }

    private void signalThreads(long reader, long writer) {
        if (NativeThread.isNativeThread(reader))
            NativeThread.signal(reader);
        if (NativeThread.isNativeThread(writer))
            NativeThread.signal(writer);
    }

    @Override
    void implPreClose(FileDescriptor fd, long reader, long writer) throws IOException {
        if (SUPPORTS_PENDING_SIGNALS) {
            signalThreads(reader, writer);
        }
        preClose0(fd);
        if (!SUPPORTS_PENDING_SIGNALS) {
            signalThreads(reader, writer);
        }
    }

    private static void close0(FileDescriptor fd) throws IOException {
        int[] err = new int[1];
        int fdVal = IOUtil.fdVal(fd);
        if (fdVal == -1)
            return;
        if (HostChannels.close0(fdVal, err) == HostChannels.THROWN)
            throw HostChannels.ioException(err[0], "close failed");
    }

    private static void preClose0(FileDescriptor fd) throws IOException {
        HostChannels.preClose0(IOUtil.fdVal(fd));
    }
}
