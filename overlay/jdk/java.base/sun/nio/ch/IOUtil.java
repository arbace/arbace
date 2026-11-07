/*
 * Arbace (overlay/jdk, F10 of doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F10a"): jrt's
 * sun.nio.ch.IOUtil. jdk26u's (baf63fb) does I/O from native memory: a heap buffer is copied
 * through a temporary direct buffer, a gathering write or scattering read builds an iovec array
 * (IOVecWrapper) for one writev or readv. jrt has no native memory: a buffer with an array is read
 * into or written from that array, one without (a read-only buffer) through a temporary array, and
 * a gathering write or scattering read is one write or read of a temporary array as long as the
 * buffers' remaining bytes (at most IOV_MAX buffers and WRITEV_MAX bytes, as jdk26u's), the bytes
 * then given to the buffers in order: one system call, as writev and readv. The signatures, the
 * results and the buffers' positions are jdk26u's; directIO's alignment is refused (no O_DIRECT in
 * jrt); the natives are jdk26u's over jdk.internal.jrt.HostChannels (IOUtil.c's).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import jdk.internal.jrt.HostChannels;
import jdk.internal.jrt.HostFiles;

/**
 * File-descriptor based I/O utilities that are shared by NIO classes.
 */
public class IOUtil {

    /**
     * Max number of iovec structures that readv/writev supports
     */
    static final int IOV_MAX;

    /*
     * Max total number of bytes that writev supports
     */
    static final long WRITEV_MAX;

    private IOUtil() { }                // No instantiation

    static int write(FileDescriptor fd, ByteBuffer src, long position,
                     NativeDispatcher nd)
        throws IOException
    {
        return write(fd, src, position, false, false, -1, nd);
    }

    static int write(FileDescriptor fd, ByteBuffer src, long position,
                     boolean async, NativeDispatcher nd)
        throws IOException
    {
        return write(fd, src, position, false, async, -1, nd);
    }

    static int write(FileDescriptor fd, ByteBuffer src, long position,
                     boolean directIO, int alignment, NativeDispatcher nd)
        throws IOException
    {
        return write(fd, src, position, directIO, false, alignment, nd);
    }

    static int write(FileDescriptor fd, ByteBuffer src, long position,
                     boolean directIO, boolean async, int alignment,
                     NativeDispatcher nd)
        throws IOException
    {
        if (directIO)
            throw new IOException("Direct I/O is not supported");
        if (position != -1)
            throw new IOException("Operation Unsupported");
        int pos = src.position();
        int lim = src.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        if (rem == 0)
            return 0;
        int written;
        if (src.hasArray()) {
            written = nd.write(fd, src.array(), src.arrayOffset() + pos, rem);
        } else {
            byte[] b = new byte[rem];
            src.get(pos, b, 0, rem);
            written = nd.write(fd, b, 0, rem);
        }
        if (written > 0)
            src.position(pos + written);
        return written;
    }

    static long write(FileDescriptor fd, ByteBuffer[] bufs, boolean async,
                      NativeDispatcher nd)
        throws IOException
    {
        return write(fd, bufs, 0, bufs.length, false, async, -1, nd);
    }

    static long write(FileDescriptor fd, ByteBuffer[] bufs, int offset, int length,
                      NativeDispatcher nd)
        throws IOException
    {
        return write(fd, bufs, offset, length, false, false, -1, nd);
    }

    static long write(FileDescriptor fd, ByteBuffer[] bufs, int offset, int length,
                      boolean direct, int alignment, NativeDispatcher nd)
        throws IOException
    {
        return write(fd, bufs, offset, length, direct, false, alignment, nd);
    }

    static long write(FileDescriptor fd, ByteBuffer[] bufs, int offset, int length,
                      boolean directIO, boolean async,
                      int alignment, NativeDispatcher nd)
        throws IOException
    {
        if (directIO)
            throw new IOException("Direct I/O is not supported");
        // the buffers writev would take, as jdk26u's iovec array
        int count = offset + length;
        int iov_len = 0;
        long writevLen = 0L;
        int i = offset;
        while (i < count && iov_len < IOV_MAX && writevLen < WRITEV_MAX) {
            ByteBuffer buf = bufs[i];
            int pos = buf.position();
            int lim = buf.limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);
            if (rem > 0) {
                long headroom = WRITEV_MAX - writevLen;
                if (headroom < rem)
                    rem = (int)headroom;
                iov_len++;
                writevLen += rem;
            }
            i++;
        }
        if (iov_len == 0)
            return 0L;
        if (writevLen > Integer.MAX_VALUE - 8)
            writevLen = Integer.MAX_VALUE - 8;
        byte[] b = new byte[(int) writevLen];
        int filled = 0;
        for (int j = offset; j < i && filled < b.length; j++) {
            ByteBuffer buf = bufs[j];
            int pos = buf.position();
            int rem = Math.min(buf.remaining(), b.length - filled);
            if (rem > 0) {
                buf.get(pos, b, filled, rem);
                filled += rem;
            }
        }
        long bytesWritten = nd.write(fd, b, 0, filled);

        // Notify the buffers how many bytes were taken
        long left = bytesWritten;
        for (int j = offset; j < i && left > 0; j++) {
            ByteBuffer buf = bufs[j];
            int pos = buf.position();
            int rem = buf.remaining();
            int n = (left > rem) ? rem : (int)left;
            buf.position(pos + n);
            left -= n;
        }
        return bytesWritten;
    }

    static int read(FileDescriptor fd, ByteBuffer dst, long position,
                    NativeDispatcher nd)
        throws IOException
    {
        return read(fd, dst, position, false, false, -1, nd);
    }

    static int read(FileDescriptor fd, ByteBuffer dst, long position,
                    boolean async, NativeDispatcher nd)
        throws IOException
    {
        return read(fd, dst, position, false, async, -1, nd);
    }

    static int read(FileDescriptor fd, ByteBuffer dst, long position,
                    boolean directIO, int alignment, NativeDispatcher nd)
        throws IOException
    {
        return read(fd, dst, position, directIO, false, alignment, nd);
    }

    static int read(FileDescriptor fd, ByteBuffer dst, long position,
                    boolean directIO, boolean async,
                    int alignment, NativeDispatcher nd)
        throws IOException
    {
        if (dst.isReadOnly())
            throw new IllegalArgumentException("Read-only buffer");
        if (directIO)
            throw new IOException("Direct I/O is not supported");
        if (position != -1)
            throw new IOException("Operation Unsupported");
        int pos = dst.position();
        int lim = dst.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        if (rem == 0)
            return 0;
        int n;
        if (dst.hasArray()) {
            n = nd.read(fd, dst.array(), dst.arrayOffset() + pos, rem);
        } else {
            byte[] b = new byte[rem];
            n = nd.read(fd, b, 0, rem);
            if (n > 0)
                dst.put(pos, b, 0, n);
        }
        if (n > 0)
            dst.position(pos + n);
        return n;
    }

    static long read(FileDescriptor fd, ByteBuffer[] bufs, NativeDispatcher nd)
        throws IOException
    {
        return read(fd, bufs, 0, bufs.length, false, false, -1, nd);
    }

    static long read(FileDescriptor fd, ByteBuffer[] bufs, boolean async,
                     NativeDispatcher nd)
        throws IOException
    {
        return read(fd, bufs, 0, bufs.length, false, async, -1, nd);
    }

    static long read(FileDescriptor fd, ByteBuffer[] bufs, int offset, int length,
                     NativeDispatcher nd)
        throws IOException
    {
        return read(fd, bufs, offset, length, false, false, -1, nd);
    }

    static long read(FileDescriptor fd, ByteBuffer[] bufs, int offset, int length,
                     boolean directIO, int alignment, NativeDispatcher nd)

        throws IOException
    {
        return read(fd, bufs, offset, length, directIO, false, alignment, nd);
    }

    static long read(FileDescriptor fd, ByteBuffer[] bufs, int offset, int length,
                     boolean directIO, boolean async,
                     int alignment, NativeDispatcher nd)
        throws IOException
    {
        if (directIO)
            throw new IOException("Direct I/O is not supported");
        // the buffers readv would fill, as jdk26u's iovec array
        int count = offset + length;
        int iov_len = 0;
        long total = 0L;
        int i = offset;
        while (i < count && iov_len < IOV_MAX) {
            ByteBuffer buf = bufs[i];
            if (buf.isReadOnly())
                throw new IllegalArgumentException("Read-only buffer");
            int pos = buf.position();
            int lim = buf.limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);
            if (rem > 0) {
                iov_len++;
                total += rem;
            }
            i++;
        }
        if (iov_len == 0)
            return 0L;
        if (total > Integer.MAX_VALUE - 8)
            total = Integer.MAX_VALUE - 8;
        byte[] b = new byte[(int) total];
        long bytesRead = nd.read(fd, b, 0, b.length);

        // Notify the buffers how many bytes were read
        long left = bytesRead;
        int taken = 0;
        for (int j = offset; j < i && left > 0; j++) {
            ByteBuffer buf = bufs[j];
            int pos = buf.position();
            int rem = buf.remaining();
            int n = (left > rem) ? rem : (int)left;
            buf.put(pos, b, taken, n);
            buf.position(pos + n);
            taken += n;
            left -= n;
        }
        return bytesRead;
    }

    /** Throws t, checked or not, from a method that does not declare it (as JNI's Throw). */
    @SuppressWarnings("unchecked")
    static <T extends Throwable> RuntimeException sneaky(Throwable t) throws T {
        throw (T) t;
    }

    public static FileDescriptor newFD(int i) {
        FileDescriptor fd = new FileDescriptor();
        setfdVal(fd, i);
        return fd;
    }

    /**
     * Returns two file descriptors for a pipe encoded in a long.
     * The read end of the pipe is returned in the high 32 bits,
     * while the write end is returned in the low 32 bits.
     */
    static long makePipe(boolean blocking) throws IOException {
        int[] fds = new int[2];
        int[] err = new int[1];
        if (HostChannels.makePipe0(blocking, fds, err) == HostChannels.THROWN)
            throw HostChannels.ioException(err[0], "Pipe failed");
        return ((long) fds[0] << 32) | (fds[1] & 0xFFFFFFFFL);
    }

    static int write1(int fd, byte b) throws IOException {
        int[] err = new int[1];
        int n = HostChannels.write1_0(fd, b, err);
        if (n == HostChannels.THROWN)
            throw HostChannels.readWriteException(err[0], false);
        return n;
    }

    /**
     * Read and discard all bytes.
     */
    static boolean drain(int fd) throws IOException {
        int[] err = new int[1];
        int n = HostChannels.drain0(fd, err);
        if (n == HostChannels.THROWN)
            throw HostChannels.ioException(err[0], "Drain");
        return n == 1;
    }

    /**
     * Read and discard at most one byte
     * @return the number of bytes read or IOS_INTERRUPTED
     */
    static int drain1(int fd) throws IOException {
        int[] err = new int[1];
        int n = HostChannels.drain1_0(fd, err);
        if (n == HostChannels.THROWN)
            throw HostChannels.ioException(err[0], "Drain");
        return n;
    }

    public static void configureBlocking(FileDescriptor fd,
                                         boolean blocking)
        throws IOException
    {
        int[] err = new int[1];
        if (HostChannels.configureBlocking0(fdVal(fd), blocking, err) == HostChannels.THROWN)
            throw HostChannels.ioException(err[0], "Configure blocking failed");
    }

    public static int fdVal(FileDescriptor fd) {
        return HostFiles.handle(fd);
    }

    static void setfdVal(FileDescriptor fd, int value) {
        HostFiles.setHandle(fd, value);
    }

    static int fdLimit() {
        return HostChannels.fdLimit0();
    }

    /**
     * Used to trigger loading of native libraries
     */
    public static void load() { }

    static {
        // IOUtil.c's: IOV_MAX of limits.h (1024 on Linux), and INT_MAX for writev
        IOV_MAX = 1024;
        WRITEV_MAX = Integer.MAX_VALUE;
    }

}
