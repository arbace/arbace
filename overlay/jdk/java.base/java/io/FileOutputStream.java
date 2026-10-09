/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Phase 2C"): jrt's java.io.FileOutputStream,
 * written for jrt in place of jdk26u's (which needs file channels, events and the VM's
 * natives), over jrt's file table (jdk.internal.jrt.HostFiles); bytes written to the host, unbuffered.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.io;

import java.util.Objects;
import jdk.internal.jrt.HostFiles;

/** An output stream writing to a file or to a standard stream of the host. */
public class FileOutputStream extends OutputStream {

    private final FileDescriptor fd;

    /** The path of the file, null for a stream made from a descriptor. */
    private final String path;

    private final Object closeLock = new Object();

    private volatile boolean closed;

    /** Opens the file name for writing, created or truncated. */
    public FileOutputStream(String name) throws FileNotFoundException {
        this(name, false);
    }

    /** Opens the file name for writing, created, and appended to when append is true. */
    public FileOutputStream(String name, boolean append) throws FileNotFoundException {
        if (name == null) {
            throw new NullPointerException();
        }
        this.fd = new FileDescriptor();
        fd.set(HostFiles.open(name, append ? HostFiles.APPEND : HostFiles.WRITE));
        this.path = name;
    }

    /** Opens file for writing, created or truncated. */
    public FileOutputStream(File file) throws FileNotFoundException {
        this(file, false);
    }

    /** Opens file for writing, created, and appended to when append is true. */
    public FileOutputStream(File file, boolean append) throws FileNotFoundException {
        this(file != null ? file.getPath() : null, append);
    }

    /** A stream writing to an open descriptor. */
    public FileOutputStream(FileDescriptor fdObj) {
        if (fdObj == null) {
            throw new NullPointerException();
        }
        this.fd = fdObj;
        this.path = null;
    }

    @Override
    public void write(int b) throws IOException {
        byte[] one = {(byte) b};
        HostFiles.write(HostFiles.handle(fd), one, 0, 1);
    }

    @Override
    public void write(byte[] b) throws IOException {
        HostFiles.write(HostFiles.handle(fd), b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len > 0) {
            HostFiles.write(HostFiles.handle(fd), b, off, len);
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (closeLock) {
            if (closed) {
                return;
            }
            closed = true;
        }
        fd.close();
    }

    /** The descriptor of this stream. */
    public final FileDescriptor getFD() throws IOException {
        return fd;
    }
}
