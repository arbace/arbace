/*
 * Arbace (overlay/jdk, F10 of doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F10a"): the member of
 * jdk26u's sun.nio.ch.FileDispatcherImpl (linux, over UnixFileDispatcherImpl, baf63fb) the selectors
 * call, closeIntFD (UnixFileDispatcherImpl.c's close of a descriptor number), over
 * jdk.internal.jrt.HostChannels. jrt's file channels are its own (sun.nio.ch.FileChannelImpl, F7),
 * so the rest of jdk26u's file dispatcher is not in the Go build.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.nio.ch;

import java.io.IOException;
import jdk.internal.jrt.HostChannels;

class FileDispatcherImpl {
    private FileDispatcherImpl() { }

    static void closeIntFD(int fd) throws IOException {
        int[] err = new int[1];
        if (HostChannels.close0(fd, err) == HostChannels.THROWN)
            throw HostChannels.ioException(err[0], "close failed");
    }
}
