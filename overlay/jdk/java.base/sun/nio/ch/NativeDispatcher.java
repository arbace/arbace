/*
 * Arbace (overlay/jdk, F10 of doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F10a"): jrt's
 * sun.nio.ch.NativeDispatcher. jdk26u's (baf63fb) reads and writes at a native address (a direct
 * buffer's, or an iovec array's); jrt has no native memory, so its dispatcher reads into and
 * writes from a byte[] (IOUtil's buffers' arrays). The address methods stay, refusing, so that
 * jdk26u's callers compile; preClose is jdk26u's, its descriptor access IOUtil.fdVal.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;

/**
 * Allows different platforms to call different native methods
 * for read and write operations.
 */
abstract class NativeDispatcher {

    /** Reads up to len bytes of fd into b at off: a count, IOStatus.EOF or UNAVAILABLE. */
    abstract int read(FileDescriptor fd, byte[] b, int off, int len) throws IOException;

    /** Writes up to len bytes of b from off to fd: a count or IOStatus.UNAVAILABLE. */
    abstract int write(FileDescriptor fd, byte[] b, int off, int len) throws IOException;

    int read(FileDescriptor fd, long address, int len) throws IOException {
        throw new UnsupportedOperationException("native memory");
    }

    /**
     * Returns {@code true} if pread/pwrite needs to be synchronized with
     * position sensitive methods.
     */
    boolean needsPositionLock() {
        return false;
    }

    int pread(FileDescriptor fd, long address, int len, long position)
        throws IOException
    {
        throw new IOException("Operation Unsupported");
    }

    long readv(FileDescriptor fd, long address, int len) throws IOException {
        throw new UnsupportedOperationException("native memory");
    }

    int write(FileDescriptor fd, long address, int len) throws IOException {
        throw new UnsupportedOperationException("native memory");
    }

    int pwrite(FileDescriptor fd, long address, int len, long position)
        throws IOException
    {
        throw new IOException("Operation Unsupported");
    }

    long writev(FileDescriptor fd, long address, int len) throws IOException {
        throw new UnsupportedOperationException("native memory");
    }

    abstract void close(FileDescriptor fd) throws IOException;

    /**
     * Prepare the given file descriptor for closing. If a virtual thread is blocked
     * on the file descriptor then it is unparked so that it stops polling. On Unix systems,
     * if a platform thread is blocked on the file descriptor then the file descriptor is
     * dup'ed to a special fd and the thread signalled so that the syscall fails with EINTR.
     */
    final void preClose(FileDescriptor fd, long reader, long writer) throws IOException {
        if (NativeThread.isVirtualThread(reader) || NativeThread.isVirtualThread(writer)) {
            int fdVal = IOUtil.fdVal(fd);
            Poller.stopPoll(fdVal);
        }
        if (NativeThread.isNativeThread(reader) || NativeThread.isNativeThread(writer)) {
            implPreClose(fd, reader, writer);
        }
    }

    /**
     * This method does nothing by default. On Unix systems the file descriptor is dup'ed
     * to a special fd and native threads signalled.
     */
    void implPreClose(FileDescriptor fd, long reader, long writer) throws IOException {
        // Do nothing by default; this is only needed on Unix
    }

    /**
     * Duplicates a file descriptor.
     * @param fd1 the file descriptor to duplicate
     * @param fd2 the new file descriptor, must not be null
     * @throws UnsupportedOperationException is not supported by this implementation
     */
    void dup(FileDescriptor fd1, FileDescriptor fd2) throws IOException {
        throw new UnsupportedOperationException();
    }
}
