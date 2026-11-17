/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Phase 2C"): jrt's java.io.FileInputStream,
 * written for jrt in place of jdk26u's (which needs file channels, events and the VM's
 * natives), over jrt's file table (jdk.internal.jrt.HostFiles); bytes read from the host, unbuffered.
 * Since F7 (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F7 (write-ahead)") its channel, jrt's
 * FileChannelImpl, by jdk26u's getChannel, and the channel closed with the stream as jdk26u's close
 * closes it (https://github.com/openjdk/jdk26u at baf63fb, src/java.base/share/classes/java/io/FileInputStream.java;
 * Copyright (c) Oracle and/or its affiliates, GPL 2 with the Classpath Exception: LICENSE.md).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.io;

import java.nio.channels.FileChannel;
import java.util.Objects;
import jdk.internal.jrt.HostFiles;
import sun.nio.ch.FileChannelImpl;

/** An input stream reading a file or the standard input of the host. */
public class FileInputStream extends InputStream {

    private final FileDescriptor fd;

    /** The path of the file, null for a stream made from a descriptor. */
    private final String path;

    private final Object closeLock = new Object();

    private volatile boolean closed;

    /** The channel, made by getChannel. */
    private volatile FileChannel channel;

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
        FileChannel fc = channel;
        if (fc != null) {
            // possible race with getChannel(), benign since
            // FileChannel.close is final and idempotent
            fc.close();
        }
        fd.close();
    }

    /** The descriptor of this stream. */
    public final FileDescriptor getFD() throws IOException {
        return fd;
    }

    /** The unique FileChannel of this stream (jdk26u's). */
    public FileChannel getChannel() {
        FileChannel fc = this.channel;
        if (fc == null) {
            synchronized (this) {
                fc = this.channel;
                if (fc == null) {
                    fc = FileChannelImpl.open(fd, path, true, false, false, false, this);
                    this.channel = fc;
                    if (closed) {
                        try {
                            // possible race with close(), benign since
                            // FileChannel.close is final and idempotent
                            fc.close();
                        } catch (IOException ioe) {
                            throw new InternalError(ioe); // should not happen
                        }
                    }
                }
            }
        }
        return fc;
    }
}
