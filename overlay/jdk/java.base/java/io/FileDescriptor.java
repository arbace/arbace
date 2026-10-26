/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Phase 2C"): jrt's java.io.FileDescriptor, written
 * for jrt in place of jdk26u's (which needs the VM's native handles, Blocker and cleaners),
 * with jdk26u's public members and the package-private ones the streams use (set, close). A
 * descriptor is a number of jrt's file table (jdk.internal.jrt.HostFiles): 0, 1 and 2 the
 * host's standard streams, the others files the host opened. The static initializer gives
 * HostFiles the setting of a descriptor's number (the JDK's FileDescriptor gives SharedSecrets its
 * JavaIOFileDescriptorAccess so), which ProcessImpl uses for its pipes (JRT-NOTES.md "Processes").
 * Since JC6b (doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JC6b") also jdk26u's attach and
 * closeAll, through which jdk26u's RandomAccessFile closes the streams sharing its descriptor;
 * they are transcribed from jdk26u (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/java/io/FileDescriptor.java; Copyright (c) Oracle and/or its
 * affiliates, GPL 2 with the Classpath Exception: LICENSE.md).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.io;

import java.util.ArrayList;
import java.util.List;

import jdk.internal.jrt.HostFiles;

/**
 * An open file of the host: the standard streams and the files opened by
 * {@code FileInputStream} and {@code FileOutputStream}.
 */
public final class FileDescriptor {

    private int fd;

    /** The streams sharing the descriptor (attach), and whether closeAll ran (jdk26u's). */
    private Closeable parent;
    private List<Closeable> otherParents;
    private boolean closed;

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

    static {
        HostFiles.setDescriptorAccess(new HostFiles.DescriptorAccess() {
            public void set(FileDescriptor fd, int h) {
                fd.set(h);
            }
        });
    }

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

    /** Attaches a stream that shares the descriptor (jdk26u's attach). */
    synchronized void attach(Closeable c) {
        if (parent == null) {
            // first caller gets to do this
            parent = c;
        } else if (otherParents == null) {
            otherParents = new ArrayList<>();
            otherParents.add(parent);
            otherParents.add(c);
        } else {
            otherParents.add(c);
        }
    }

    /**
     * Closes the streams attached, then releaser, once (jdk26u's closeAll): the first exception
     * thrown, the others suppressed.
     */
    @SuppressWarnings("try")
    synchronized void closeAll(Closeable releaser) throws IOException {
        if (!closed) {
            closed = true;
            IOException ioe = null;
            try (releaser) {
                if (otherParents != null) {
                    for (Closeable referent : otherParents) {
                        try {
                            referent.close();
                        } catch(IOException x) {
                            if (ioe == null) {
                                ioe = x;
                            } else {
                                ioe.addSuppressed(x);
                            }
                        }
                    }
                }
            } catch(IOException ex) {
                if (ioe != null)
                    ex.addSuppressed(ioe);
                ioe = ex;
            } finally {
                if (ioe != null)
                    throw ioe;
            }
        }
    }
}
