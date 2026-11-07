/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F7 (write-ahead)"): jrt's
 * sun.nio.ch.FileChannelImpl, written for jrt in place of jdk26u's (whose dispatcher, native
 * thread set, temporary direct buffers, mmap and file lock table need the VM's natives), over
 * jrt's file table (jdk.internal.jrt.HostFiles) and HostFileChannels: the channel of a
 * FileInputStream, FileOutputStream or RandomAccessFile (getChannel, through jdk26u's open with
 * its arguments) and of FileChannel.open (the FileChannel variant). The checks and their order,
 * the exceptions and the results are jdk26u's (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/sun/nio/ch/FileChannelImpl.java; Copyright (c) Oracle and/or its
 * affiliates, GPL 2 with the Classpath Exception: LICENSE.md): the channel's position is the
 * descriptor's (shared with the stream), a stream opened for appending writes at the end and
 * reports the size as its position, a positional read or write leaves the position alone, truncate
 * moves a position beyond the new size, transfers move bytes through a buffer, a lock is checked
 * against the process's other locks of the file (OverlappingFileLockException) but not against
 * other processes', and map copies the region into a heap buffer (jrt's MappedByteBuffer), written
 * back by force and when this channel closes. Interruption is AbstractInterruptibleChannel's (its
 * variant: a thread interrupted before or during an operation closes the channel and gets
 * ClosedByInterruptException). The rest Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.nio.ch;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import jdk.internal.jrt.HostFileChannels;
import jdk.internal.jrt.HostFiles;

public class FileChannelImpl
    extends FileChannel
{
    // The bytes a transfer or a read into a buffer without an array moves at once
    private static final int TRANSFER_SIZE = 8192;

    // Used to make native read and write calls
    private final FileDescriptor fd;

    // File access mode (immutable)
    private final boolean writable;
    private final boolean readable;
    private final boolean append;

    // Required to prevent finalization of creating stream (immutable)
    private final Closeable parent;

    // The path of the referenced file
    // (null if the parent stream is created with a file descriptor)
    private final String path;

    // The key of this file's locks (HostFileChannels)
    private final Object lockKey;

    // The read/write mappings made through this channel, written back when it closes
    private final List<MappedByteBuffer> mappings = new ArrayList<>();

    private FileChannelImpl(FileDescriptor fd, String path, boolean readable,
                            boolean writable, boolean append, Closeable parent)
    {
        this.fd = fd;
        this.path = path;
        this.readable = readable;
        this.writable = writable;
        this.append = append;
        this.parent = parent;
        this.lockKey = HostFileChannels.lockKey(fd, path);
    }

    /** jdk26u's: the channel of a stream or a RandomAccessFile (sync and direct do not matter here). */
    public static FileChannel open(FileDescriptor fd, String path,
                                   boolean readable, boolean writable,
                                   boolean sync, boolean direct, Closeable parent)
    {
        return new FileChannelImpl(fd, path, readable, writable, false, parent);
    }

    /** jrt's: the channel of a FileOutputStream opened for appending. */
    public static FileChannel open(FileDescriptor fd, String path,
                                   boolean readable, boolean writable, boolean append,
                                   Closeable parent)
    {
        return new FileChannelImpl(fd, path, readable, writable, append, parent);
    }

    /**
     * jrt's: FileChannel.open(path, options), the options of the default file system's
     * (sun.nio.fs.UnixChannelFactory's checks and messages); the file attributes are not
     * supported but an empty array.
     */
    public static FileChannel open(Path path, Set<? extends OpenOption> options) throws IOException {
        Objects.requireNonNull(path, "path");
        boolean read = false, write = false, appendMode = false, truncate = false;
        boolean create = false, createNew = false, deleteOnClose = false;
        for (OpenOption o : options) {
            if (o == StandardOpenOption.READ) read = true;
            else if (o == StandardOpenOption.WRITE) write = true;
            else if (o == StandardOpenOption.APPEND) { appendMode = true; write = true; }
            else if (o == StandardOpenOption.TRUNCATE_EXISTING) truncate = true;
            else if (o == StandardOpenOption.CREATE) create = true;
            else if (o == StandardOpenOption.CREATE_NEW) createNew = true;
            else if (o == StandardOpenOption.DELETE_ON_CLOSE) deleteOnClose = true;
            else if (o == StandardOpenOption.SPARSE || o == StandardOpenOption.SYNC
                     || o == StandardOpenOption.DSYNC || o == java.nio.file.LinkOption.NOFOLLOW_LINKS) { }
            else if (o == null) throw new NullPointerException();
            else throw new UnsupportedOperationException(o + " not supported");
        }
        if (!read && !write)
            read = true;
        if (read && appendMode)
            throw new IllegalArgumentException("READ + APPEND not allowed");
        if (appendMode && truncate)
            throw new IllegalArgumentException("APPEND + TRUNCATE_EXISTING not allowed");
        String name = path.toString();
        File f = new File(name);
        boolean exists = f.exists();
        if (createNew && write && exists)
            throw new FileAlreadyExistsException(name);
        if (!exists && !(write && (create || createNew)))
            throw new NoSuchFileException(name);
        int mode;
        if (appendMode)
            mode = read ? HostFiles.READ_WRITE : HostFiles.APPEND;
        else if (write)
            mode = HostFiles.READ_WRITE;
        else
            mode = HostFiles.READ;
        int h;
        try {
            h = HostFiles.open(name, mode);
        } catch (FileNotFoundException x) {
            throw new java.nio.file.FileSystemException(name, null, x.getMessage());
        }
        FileDescriptor fdObj = new FileDescriptor();
        HostFiles.setHandle(fdObj, h);
        FileChannelImpl fc = new FileChannelImpl(fdObj, name, read, write, appendMode, null);
        if (write && truncate && !appendMode)
            HostFiles.setLength(h, 0);
        if (deleteOnClose)
            f.deleteOnExit();
        return fc;
    }

    private int handle() {
        return HostFiles.handle(fd);
    }

    private void ensureOpen() throws IOException {
        if (!isOpen())
            throw new ClosedChannelException();
    }

    // -- Standard channel operations --

    protected void implCloseChannel() throws IOException {
        if (!fd.valid())
            return; // nothing to do

        // Write back the read/write mappings while the descriptor is open
        List<MappedByteBuffer> ms;
        synchronized (mappings) {
            ms = new ArrayList<>(mappings);
            mappings.clear();
        }
        for (MappedByteBuffer m : ms) {
            try {
                m.force();
            } catch (RuntimeException x) {
                // the mapping's file is gone: nothing to write back
            }
        }

        // Release and invalidate any locks that we still hold
        for (FileLock fl : HostFileChannels.removeLocks(lockKey, this)) {
            synchronized (fl) {
                if (fl.isValid()) {
                    ((FileLockImpl) fl).invalidate();
                }
            }
        }

        if (parent != null) {
            // Close the fd via the parent stream's close method (which closes this channel
            // again: AbstractInterruptibleChannel's close returns at once)
            parent.close();
        } else {
            int h = handle();
            HostFiles.setHandle(fd, -1);
            if (h > 2)
                HostFiles.close(h);
        }
    }

    // reads into dst at the descriptor's position (pos < 0) or at pos: the count, or -1
    private int readInto(ByteBuffer dst, long pos) throws IOException {
        if (dst.isReadOnly())
            throw new IllegalArgumentException("Read-only buffer");
        int rem = dst.remaining();
        if (rem == 0)
            return 0;
        byte[] b;
        int off;
        boolean direct = dst.hasArray();
        if (direct) {
            b = dst.array();
            off = dst.arrayOffset() + dst.position();
        } else {
            b = new byte[Math.min(rem, TRANSFER_SIZE)];
            off = 0;
            rem = b.length;
        }
        int n = (pos < 0) ? HostFiles.read(handle(), b, off, rem)
                          : HostFileChannels.pread(fd, pos, b, off, rem);
        if (n > 0) {
            if (direct)
                dst.position(dst.position() + n);
            else
                dst.put(b, 0, n);
        }
        return n;
    }

    // writes src's remaining bytes at the descriptor's position (pos < 0) or at pos: the count
    private int writeFrom(ByteBuffer src, long pos) throws IOException {
        int rem = src.remaining();
        if (rem == 0)
            return 0;
        byte[] b;
        int off;
        if (src.hasArray()) {
            b = src.array();
            off = src.arrayOffset() + src.position();
        } else {
            b = new byte[rem];
            off = 0;
            src.duplicate().get(b);
        }
        if (pos < 0)
            HostFiles.write(handle(), b, off, rem);
        else
            HostFileChannels.pwrite(fd, pos, b, off, rem);
        src.position(src.position() + rem);
        return rem;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        Objects.requireNonNull(dst);
        ensureOpen();
        if (!readable)
            throw new NonReadableChannelException();
        synchronized (fd) {
            int n = 0;
            try {
                begin();
                if (!isOpen())
                    return 0;
                n = readInto(dst, -1);
                return n;
            } finally {
                end(n > 0);
            }
        }
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, dsts.length);
        ensureOpen();
        if (!readable)
            throw new NonReadableChannelException();
        synchronized (fd) {
            long total = 0;
            try {
                begin();
                if (!isOpen())
                    return 0;
                for (int i = offset; i < offset + length; i++) {
                    ByteBuffer dst = dsts[i];
                    int want = dst.remaining();
                    if (want == 0)
                        continue;
                    int n = readInto(dst, -1);
                    if (n < 0)
                        return (total == 0) ? -1 : total;
                    total += n;
                    if (n < want)
                        break;
                }
                return total;
            } finally {
                end(total > 0);
            }
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        Objects.requireNonNull(src);
        ensureOpen();
        if (!writable)
            throw new NonWritableChannelException();
        synchronized (fd) {
            int n = 0;
            try {
                begin();
                if (!isOpen())
                    return 0;
                n = writeFrom(src, -1);
                return n;
            } finally {
                end(n > 0);
            }
        }
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, srcs.length);
        ensureOpen();
        if (!writable)
            throw new NonWritableChannelException();
        synchronized (fd) {
            long total = 0;
            try {
                begin();
                if (!isOpen())
                    return 0;
                for (int i = offset; i < offset + length; i++)
                    total += writeFrom(srcs[i], -1);
                return total;
            } finally {
                end(total > 0);
            }
        }
    }

    // -- Other operations --

    @Override
    public long position() throws IOException {
        ensureOpen();
        synchronized (fd) {
            long p = -1;
            try {
                begin();
                if (!isOpen())
                    return 0;
                // in append-mode then position is advanced to end before writing
                p = append ? HostFiles.length(handle()) : HostFiles.position(handle());
                return p;
            } finally {
                end(p > -1);
            }
        }
    }

    @Override
    public FileChannel position(long newPosition) throws IOException {
        ensureOpen();
        if (newPosition < 0)
            throw new IllegalArgumentException();
        synchronized (fd) {
            boolean done = false;
            try {
                begin();
                if (!isOpen())
                    return null;
                HostFiles.seek(handle(), newPosition);
                done = true;
                return this;
            } finally {
                end(done);
            }
        }
    }

    @Override
    public long size() throws IOException {
        ensureOpen();
        synchronized (fd) {
            long s = -1;
            try {
                begin();
                if (!isOpen())
                    return -1;
                s = HostFiles.length(handle());
                return s;
            } finally {
                end(s > -1);
            }
        }
    }

    /**
     * Returns an estimate of the number of remaining bytes that can be read
     * from this channel without blocking.
     */
    int available() throws IOException {
        ensureOpen();
        synchronized (fd) {
            return HostFiles.available(handle());
        }
    }

    /**
     * Tells whether the channel represents something other than a regular
     * file, directory, or symbolic link: the host's standard streams.
     */
    boolean isOther() throws IOException {
        ensureOpen();
        return path == null && handle() <= 2;
    }

    @Override
    public FileChannel truncate(long newSize) throws IOException {
        ensureOpen();
        if (newSize < 0)
            throw new IllegalArgumentException("Negative size");
        if (!writable)
            throw new NonWritableChannelException();
        synchronized (fd) {
            boolean done = false;
            try {
                begin();
                if (!isOpen())
                    return null;
                int h = handle();
                long size = HostFiles.length(h);
                long p = HostFiles.position(h);
                // truncate file if given size is less than the current size
                if (newSize < size)
                    HostFiles.setLength(h, newSize);
                // if position is beyond new size then adjust it
                if (p > newSize)
                    p = newSize;
                HostFiles.seek(h, p);
                done = true;
                return this;
            } finally {
                end(done);
            }
        }
    }

    @Override
    public void force(boolean metaData) throws IOException {
        ensureOpen();
        // jrt's host writes through (FileDescriptor.sync): nothing is buffered here
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target)
        throws IOException
    {
        ensureOpen();
        if (!target.isOpen())
            throw new ClosedChannelException();
        if (!readable)
            throw new NonReadableChannelException();
        if (target instanceof FileChannelImpl && !((FileChannelImpl) target).writable)
            throw new NonWritableChannelException();
        if ((position < 0) || (count < 0))
            throw new IllegalArgumentException();
        long sz = size();
        if (position > sz)
            return 0;
        long remaining = Math.min(count, sz - position);
        long tw = 0;
        byte[] b = new byte[(int) Math.min(remaining, TRANSFER_SIZE)];
        try {
            while (tw < remaining) {
                int want = (int) Math.min(remaining - tw, b.length);
                int n = HostFileChannels.pread(fd, position + tw, b, 0, want);
                if (n <= 0)
                    break;
                ByteBuffer bb = ByteBuffer.wrap(b, 0, n);
                int w = 0;
                while (bb.hasRemaining()) {
                    int k = target.write(bb);
                    if (k <= 0)
                        break;
                    w += k;
                }
                tw += w;
                if (w < n)
                    break;
            }
            return tw;
        } catch (IOException x) {
            if (tw > 0)
                return tw;
            throw x;
        }
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count)
        throws IOException
    {
        ensureOpen();
        if (!src.isOpen())
            throw new ClosedChannelException();
        if (src instanceof FileChannelImpl fci && !fci.readable)
            throw new NonReadableChannelException();
        if (!writable)
            throw new NonWritableChannelException();
        if ((position < 0) || (count < 0))
            throw new IllegalArgumentException();
        if (position > size())
            return 0;
        long tw = 0;
        ByteBuffer bb = ByteBuffer.allocate((int) Math.min(Math.max(count, 0), TRANSFER_SIZE));
        try {
            while (tw < count) {
                bb.clear();
                bb.limit((int) Math.min(count - tw, bb.capacity()));
                int n = src.read(bb);
                if (n <= 0)
                    break;
                bb.flip();
                HostFileChannels.pwrite(fd, position + tw, bb.array(), 0, n);
                tw += n;
            }
            return tw;
        } catch (IOException x) {
            if (tw > 0)
                return tw;
            throw x;
        }
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        if (dst == null)
            throw new NullPointerException();
        if (position < 0)
            throw new IllegalArgumentException("Negative position");
        ensureOpen();
        if (!readable)
            throw new NonReadableChannelException();
        int n = 0;
        try {
            begin();
            if (!isOpen())
                return -1;
            n = readInto(dst, position);
            return n;
        } finally {
            end(n > 0);
        }
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        if (src == null)
            throw new NullPointerException();
        if (position < 0)
            throw new IllegalArgumentException("Negative position");
        ensureOpen();
        if (!writable)
            throw new NonWritableChannelException();
        int n = 0;
        try {
            begin();
            if (!isOpen())
                return -1;
            n = writeFrom(src, position);
            return n;
        } finally {
            end(n > 0);
        }
    }

    // -- Memory-mapped buffers --

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        if (size > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Size exceeds Integer.MAX_VALUE");
        ensureOpen();
        if (mode == null)
            throw new NullPointerException("Mode is null");
        if (position < 0L)
            throw new IllegalArgumentException("Negative position");
        if (size < 0L)
            throw new IllegalArgumentException("Negative size");
        if (position + size < 0)
            throw new IllegalArgumentException("Position + size overflow");
        boolean readOnly, writeBack;
        if (mode == MapMode.READ_ONLY) {
            readOnly = true; writeBack = false;
        } else if (mode == MapMode.READ_WRITE) {
            readOnly = false; writeBack = true;
        } else if (mode == MapMode.PRIVATE) {
            readOnly = false; writeBack = false;
        } else {
            // the extended modes (READ_ONLY_SYNC, READ_WRITE_SYNC: non-volatile memory)
            throw new UnsupportedOperationException();
        }
        if (!readOnly && !writable)
            throw new NonWritableChannelException();
        if (!readable)
            throw new NonReadableChannelException();
        byte[] contents = new byte[(int) size];
        boolean done = false;
        try {
            begin();
            if (!isOpen())
                return null;
            synchronized (fd) {
                int h = handle();
                long filesize = HostFiles.length(h);
                if (filesize < position + size) { // Extend file size
                    if (!writable) {
                        throw new IOException("Channel not open for writing " +
                            "- cannot extend file to required size");
                    }
                    HostFiles.setLength(h, position + size);
                }
                int got = 0;
                while (got < size) {
                    int n = HostFileChannels.pread(fd, position + got, contents, got, (int) size - got);
                    if (n <= 0)
                        break;
                    got += n;
                }
            }
            done = true;
        } finally {
            end(done);
        }
        HostFileChannels.Mapping m = new HostFileChannels.Mapping(fd, path, position, writeBack);
        MappedByteBuffer mbb = HostFileChannels.map(contents, m, readOnly);
        if (writeBack && size > 0) {
            synchronized (mappings) {
                mappings.add(mbb);
            }
        }
        return mbb;
    }

    // -- Locks --

    @Override
    public FileLock lock(long position, long size, boolean shared)
        throws IOException
    {
        ensureOpen();
        if (shared && !readable)
            throw new NonReadableChannelException();
        if (!shared && !writable)
            throw new NonWritableChannelException();
        if (size == 0)
            size = Long.MAX_VALUE - Math.max(0, position);
        FileLockImpl fli = new FileLockImpl(this, position, size, shared);
        HostFileChannels.addLock(lockKey, fli);
        boolean completed = false;
        try {
            begin();
            completed = isOpen();
        } finally {
            if (!completed)
                HostFileChannels.removeLock(lockKey, fli);
            try {
                end(completed);
            } catch (ClosedByInterruptException e) {
                throw new FileLockInterruptionException();
            }
        }
        return fli;
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared)
        throws IOException
    {
        ensureOpen();
        if (shared && !readable)
            throw new NonReadableChannelException();
        if (!shared && !writable)
            throw new NonWritableChannelException();
        if (size == 0)
            size = Long.MAX_VALUE - Math.max(0, position);
        FileLockImpl fli = new FileLockImpl(this, position, size, shared);
        HostFileChannels.addLock(lockKey, fli);
        return fli;
    }

    void release(FileLockImpl fli) throws IOException {
        ensureOpen();
        HostFileChannels.removeLock(lockKey, fli);
    }
}
