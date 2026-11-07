/*
 * Arbace (overlay/jdk, F10 of doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F10a"): jdk26u's
 * sun.nio.ch.EventFD (linux, baf63fb) with its natives over jdk.internal.jrt.HostChannels
 * (EventFD.c's eventfd and its 8-byte write).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.nio.ch;

import java.io.IOException;
import jdk.internal.jrt.HostChannels;

/*
 * Provides access to the Linux eventfd object.
 */
final class EventFD {
    private final int efd;

    /**
     * Creates a blocking eventfd object with initial value of zero.
     */
    EventFD() throws IOException {
        efd = eventfd0();
    }

    int efd() {
        return efd;
    }

    void set() throws IOException {
        set0(efd);
    }

    void reset() throws IOException {
        IOUtil.drain(efd);
    }

    void close() throws IOException {
        FileDispatcherImpl.closeIntFD(efd);
    }

    private static int eventfd0() throws IOException {
        int[] err = new int[1];
        int efd = HostChannels.eventfd0(err);
        if (efd == HostChannels.THROWN)
            throw HostChannels.ioException(err[0], "eventfd failed");
        return efd;
    }

    /**
     * Writes the value 1 to the eventfd object as a long in the
     * native byte order of the platform.
     *
     * @param the integral eventfd file descriptor
     * @return the number of bytes written; should equal 8
     */
    private static int set0(int efd) throws IOException {
        int[] err = new int[1];
        int n = HostChannels.eventfdSet0(efd, err);
        if (n == HostChannels.THROWN)
            throw HostChannels.readWriteException(err[0], false);
        return n;
    }
}
