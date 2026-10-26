/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Phase 2C"): jrt's file table, under the
 * FileDescriptor, FileInputStream and FileOutputStream written for jrt (overlay/jdk/java.base/
 * java/io). A handle is a number: 0, 1 and 2 the host's standard streams, the others the files
 * the host opened (Host.Open) and the program's ends of its children's pipes (go/arbace/jrt/
 * process.ae); the natives are jrt's Go (go/arbace/jrt/files.ae).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.IdentityHashMap;

/** The host's open files by number, and the descriptors that name them. */
public final class HostFiles {

    /** Open for reading. */
    public static final int READ = 0;
    /** Open for writing, created or truncated. */
    public static final int WRITE = 1;
    /** Open for writing, created or appended to. */
    public static final int APPEND = 2;
    /** Open for reading and writing, created if missing (RandomAccessFile's mode "rw"). */
    public static final int READ_WRITE = 3;
    /** As READ_WRITE, every write synchronous with its data and metadata ("rws", O_SYNC). */
    public static final int READ_WRITE_SYNC = 4;
    /** As READ_WRITE, every write synchronous with its data ("rwd", O_DSYNC). */
    public static final int READ_WRITE_DSYNC = 5;

    private static final IdentityHashMap<FileDescriptor, Integer> handles = new IdentityHashMap<>();

    private HostFiles() {
    }

    /**
     * The setting of a descriptor's number, which jrt's FileDescriptor gives (as the JDK's gives
     * SharedSecrets its JavaIOFileDescriptorAccess): ProcessImpl's pipes.
     */
    public interface DescriptorAccess {
        void set(FileDescriptor fd, int h);
    }

    private static DescriptorAccess access;

    /** Called once, by FileDescriptor's static initializer. */
    public static void setDescriptorAccess(DescriptorAccess a) {
        access = a;
    }

    /** Makes fd name handle h (-1: none), as the JDK's JavaIOFileDescriptorAccess.set does. */
    public static void setHandle(FileDescriptor fd, int h) {
        access.set(fd, h);
    }

    /** Records that fd names handle h (-1: none). */
    public static void register(FileDescriptor fd, int h) {
        synchronized (handles) {
            if (h == -1)
                handles.remove(fd);
            else
                handles.put(fd, h);
        }
    }

    /** The handle fd names, or -1 (closed or invalid). */
    public static int handle(FileDescriptor fd) {
        if (fd == FileDescriptor.out)
            return fd.valid() ? 1 : -1;
        if (fd == FileDescriptor.err)
            return fd.valid() ? 2 : -1;
        if (fd == FileDescriptor.in)
            return fd.valid() ? 0 : -1;
        synchronized (handles) {
            Integer h = handles.get(fd);
            return h == null ? -1 : h;
        }
    }

    /** Opens path in mode (READ, WRITE, APPEND, READ_WRITE, READ_WRITE_SYNC or READ_WRITE_DSYNC): its handle. */
    public static int open(String path, int mode) throws FileNotFoundException {
        String[] error = new String[1];
        int h = open0(path, mode, error);
        if (h < 0)
            throw new FileNotFoundException(path + " (" + error[0] + ")");
        return h;
    }

    private static int checked(int h) throws IOException {
        if (h < 0)
            throw new IOException("Stream Closed");
        return h;
    }

    /** Closes handle h. */
    public static void close(int h) throws IOException {
        close0(checked(h));
    }

    /** Writes len bytes of b from off to handle h. */
    public static void write(int h, byte[] b, int off, int len) throws IOException {
        write0(checked(h), b, off, len);
    }

    /** Reads up to len (> 0) bytes of handle h into b at off: the count, or -1 at the end. */
    public static int read(int h, byte[] b, int off, int len) throws IOException {
        return read0(checked(h), b, off, len);
    }

    /** The bytes handle h can give without blocking (an estimate). */
    public static int available(int h) throws IOException {
        return available0(checked(h));
    }

    /** Skips up to n (> 0) bytes of handle h: the count skipped. */
    public static long skip(int h, long n) throws IOException {
        return skip0(checked(h), n);
    }

    /** The position of handle h's file (RandomAccessFile.getFilePointer). */
    public static long position(int h) throws IOException {
        return position0(checked(h));
    }

    /** Sets the position of handle h's file to pos (>= 0). */
    public static void seek(int h, long pos) throws IOException {
        seek0(checked(h), pos);
    }

    /** The length of handle h's file. */
    public static long length(int h) throws IOException {
        return length0(checked(h));
    }

    /**
     * Sets the length of handle h's file, truncated or extended, the position moved to the new end
     * when it was beyond it (RandomAccessFile.setLength, as jdk26u's setLength0).
     */
    public static void setLength(int h, long len) throws IOException {
        setLength0(checked(h), len);
    }

    private static native int open0(String path, int mode, String[] error);
    private static native long position0(int h) throws IOException;
    private static native void seek0(int h, long pos) throws IOException;
    private static native long length0(int h) throws IOException;
    private static native void setLength0(int h, long len) throws IOException;
    private static native void close0(int h) throws IOException;
    private static native void write0(int h, byte[] b, int off, int len) throws IOException;
    private static native int read0(int h, byte[] b, int off, int len) throws IOException;
    private static native int available0(int h) throws IOException;
    private static native long skip0(int h, long n) throws IOException;
}
