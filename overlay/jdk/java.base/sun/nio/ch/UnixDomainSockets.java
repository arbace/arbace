/*
 * Arbace (overlay/jdk, F10 of doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F10a"): jrt's
 * sun.nio.ch.UnixDomainSockets. jdk26u's (baf63fb) binds and connects AF_UNIX sockets at a path of
 * the default file system (sun.nio.fs' provider, UnixDomainSocketAddress over java.nio.file's
 * FileSystems): the Go build's java.nio.file has no FileSystems (F9), so isSupported() is false,
 * SelectorProviderImpl refuses the UNIX family as jdk26u's does where it is not supported, and the
 * other members, which only a UNIX channel reaches, refuse.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.UnsupportedAddressTypeException;
import java.nio.file.Path;

class UnixDomainSockets {
    private UnixDomainSockets() { }

    static boolean isSupported() {
        return false;
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Unix domain sockets are not in the Go build");
    }

    static UnixDomainSocketAddress localAddress(FileDescriptor fd) throws IOException {
        throw unsupported();
    }

    static UnixDomainSocketAddress checkAddress(SocketAddress sa) {
        if (sa == null)
            throw new NullPointerException();
        throw new UnsupportedAddressTypeException();
    }

    static byte[] getPathBytes(Path path) {
        throw unsupported();
    }

    static FileDescriptor socket() throws IOException {
        throw unsupported();
    }

    static void bind(FileDescriptor fd, Path addr) throws IOException {
        throw unsupported();
    }

    static UnixDomainSocketAddress generateTempName() throws IOException {
        throw unsupported();
    }

    static int connect(FileDescriptor fd, SocketAddress sa) throws IOException {
        throw unsupported();
    }

    static int connect(FileDescriptor fd, Path path) throws IOException {
        throw unsupported();
    }

    static int accept(FileDescriptor fd, FileDescriptor newfd, String[] paths)
        throws IOException
    {
        throw unsupported();
    }

    static UnixDomainSocketAddress unnamed() {
        throw unsupported();
    }
}
