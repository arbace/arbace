/*
 * Arbace (overlay/jdk, F10 of doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F10a"): jrt's
 * sun.nio.ch.SocketDispatcher, jdk26u's (baf63fb) over jrt's byte[] dispatcher: read0 and write0
 * are SocketDispatcher.c's over jdk.internal.jrt.HostChannels (a reset connection, ECONNRESET or
 * EPIPE on a read, is sun.net.ConnectionResetException; any other failure convertReturnVal's
 * IOException). Pipes (SourceChannelImpl, SinkChannelImpl) use it as jdk26u's do.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import jdk.internal.jrt.HostChannels;
import sun.net.ConnectionResetException;

/**
 * Allows different platforms to call different native methods
 * for read and write operations.
 */
class SocketDispatcher extends UnixDispatcher {
    SocketDispatcher() { }

    @Override
    int read(FileDescriptor fd, byte[] b, int off, int len) throws IOException {
        int[] err = new int[1];
        int n = HostChannels.read0(IOUtil.fdVal(fd), b, off, len, true, err);
        if (n == HostChannels.THROWN) {
            if (err[0] == HostChannels.ECONNRESET || err[0] == HostChannels.EPIPE)
                throw new ConnectionResetException("Connection reset");
            throw HostChannels.readWriteException(err[0], true);
        }
        return n;
    }

    @Override
    int write(FileDescriptor fd, byte[] b, int off, int len) throws IOException {
        int[] err = new int[1];
        int n = HostChannels.write0(IOUtil.fdVal(fd), b, off, len, err);
        if (n == HostChannels.THROWN)
            throw HostChannels.readWriteException(err[0], false);
        return n;
    }
}
