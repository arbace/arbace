/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Phase 2C"): jrt's java.io.FileDescriptor, written
 * for jrt in place of jdk26u's (which needs the VM's native handles, Blocker and cleaners),
 * with jdk26u's public members and the package-private ones the streams use (set, close). A
 * descriptor is a number of jrt's file table (jdk.internal.jrt.HostFiles): 0, 1 and 2 the
 * host's standard streams, the others files the host opened.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.io;

import jdk.internal.jrt.HostFiles;

/**
 * An open file of the host: the standard streams and the files opened by
 * {@code FileInputStream} and {@code FileOutputStream}.
 */
public final class FileDescriptor {

    private int fd;

    /** An invalid descriptor. */
    public FileDescriptor() {
        fd = -1;
    }

    private FileDescriptor(int fd) {
        this.fd = fd;
    }

    /** The standard input stream. */
    public static final FileDescriptor in = new FileDescriptor(0);

    /** The standard output stream. */
    public static final FileDescriptor out = new FileDescriptor(1);

    /** The standard error stream. */
    public static final FileDescriptor err = new FileDescriptor(2);

    /** Whether the descriptor is open. */
    public boolean valid() {
        return fd != -1;
    }

    /** Nothing to do: jrt's host writes through. */
    public void sync() throws SyncFailedException {
    }

    /** Sets the descriptor's number (jrt's file table), -1 when closed. */
    synchronized void set(int fd) {
        this.fd = fd;
        HostFiles.register(this, fd);
    }

    /** Closes the file; the host's standard streams stay open, their descriptors invalid. */
    synchronized void close() throws IOException {
        int n = fd;
        if (n != -1) {
            set(-1);
            if (n > 2) {
                HostFiles.close(n);
            }
        }
    }
}
