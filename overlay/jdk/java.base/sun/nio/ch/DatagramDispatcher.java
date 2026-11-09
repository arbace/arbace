/*
 * Arbace (overlay/jdk, F10 of doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F10b"): jrt's
 * sun.nio.ch.DatagramDispatcher, a connected datagram channel's reads and writes (jdk26u's unix
 * DatagramDispatcher, baf63fb). jdk26u's natives read and write at a native address
 * (DatagramDispatcher.c: recv and send with no flags, recvmsg and sendmsg of an iovec array,
 * ECONNREFUSED a PortUnreachableException, else convertReturnVal); jrt's dispatcher reads into
 * and writes from a byte[] (NativeDispatcher's overlay; IOUtil gathers and scatters through one
 * array) over HostChannels' read0 and write0, read(2) and write(2) of the socket, which are recv
 * and send with no flags, with the same results; ECONNREFUSED is the PortUnreachableException.
 * dup (dup0, dup2) is the macOS branch of DatagramChannelImpl.repairSocket, which Linux does not
 * reach (its disconnect leaves an explicitly bound port bound and resets an ephemeral one to 0):
 * NativeDispatcher's refusal, as a descriptor of Go's poller cannot be replaced under it.
 * jdk26u's code: Copyright (c) Oracle and/or its affiliates, GPL 2 with the Classpath Exception
 * (LICENSE.md); the rest Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.PortUnreachableException;
import jdk.internal.jrt.HostChannels;

/**
 * Allows different platforms to call different native methods
 * for read and write operations.
 */

class DatagramDispatcher extends UnixDispatcher {

    static {
        IOUtil.load();
    }

    @Override
    int read(FileDescriptor fd, byte[] b, int off, int len) throws IOException {
        int[] err = new int[1];
        int n = HostChannels.read0(IOUtil.fdVal(fd), b, off, len, true, err);
        if (n == HostChannels.THROWN) {
            if (err[0] == HostChannels.ECONNREFUSED)
                throw new PortUnreachableException();
            throw HostChannels.readWriteException(err[0], true);
        }
        return n;
    }

    @Override
    int write(FileDescriptor fd, byte[] b, int off, int len) throws IOException {
        int[] err = new int[1];
        int n = HostChannels.write0(IOUtil.fdVal(fd), b, off, len, err);
        if (n == HostChannels.THROWN) {
            if (err[0] == HostChannels.ECONNREFUSED)
                throw new PortUnreachableException();
            throw HostChannels.readWriteException(err[0], false);
        }
        return n;
    }
}
