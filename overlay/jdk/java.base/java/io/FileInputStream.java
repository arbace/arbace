/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Phase 2C"): jrt's java.io.FileInputStream,
 * written for jrt in place of jdk26u's (which needs file channels, events and the VM's
 * natives), over jrt's file table (jdk.internal.jrt.HostFiles); bytes read from the host, unbuffered.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.io;

import java.util.Objects;
import jdk.internal.jrt.HostFiles;

/** An input stream reading a file or the standard input of the host. */
public class FileInputStream extends InputStream {

    private final FileDescriptor fd;

    /** The path of the file, null for a stream made from a descriptor. */
    private final String path;

    private final Object closeLock = new Object();

    private volatile boolean closed;

    /** Opens the file name for reading. */
    public FileInputStream(String name) throws FileNotFoundException {
        if (name == null) {
            throw new NullPointerException();
        }
        this.fd = new FileDescriptor();
        fd.set(HostFiles.open(name, HostFiles.READ));
        this.path = name;
    }

    /** Opens file for reading. */
    public FileInputStream(File file) throws FileNotFoundException {
        this(file != null ? file.getPath() : null);
    }

    /** A stream reading an open descriptor. */
    public FileInputStream(FileDescriptor fdObj) {
        if (fdObj == null) {
            throw new NullPointerException();
        }
        this.fd = fdObj;
        this.path = null;
    }

    @Override
    public int read() throws IOException {
        byte[] one = new byte[1];
        int n;
        do {
            n = HostFiles.read(HostFiles.handle(fd), one, 0, 1);
        } while (n == 0);
        return n < 0 ? -1 : one[0] & 0xff;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            return 0;
        }
        return HostFiles.read(HostFiles.handle(fd), b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        return HostFiles.skip(HostFiles.handle(fd), n);
    }

    @Override
    public int available() throws IOException {
        return HostFiles.available(HostFiles.handle(fd));
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
