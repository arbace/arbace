/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F7 (write-ahead)"): what
 * jrt's FileChannelImpl and its mapped buffers share over jrt's file table (HostFiles):
 * positional reads and writes, the table of the process's file locks, and the mappings of a
 * file into a heap copy (a MappedByteBuffer jrt makes, written back by force and when its channel
 * closes). The Go build has no pread, pwrite, flock or mmap natives: a positional operation saves
 * the descriptor's position, seeks, reads or writes, and restores it, under the descriptor's
 * monitor (FileChannelImpl takes the same for its own position); locks are the process's own,
 * checked against each other as the JDK's FileLockTable checks a JVM's (OverlappingFileLockException),
 * not against other processes; a mapping is a copy of the file's bytes, which other writers of the
 * file do not change. Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Positional I/O, file locks and heap mappings over jrt's file table. */
public final class HostFileChannels {

    private HostFileChannels() {
    }

    // -- positional reads and writes --

    /**
     * Reads up to len (> 0) bytes of fd's file at pos into b at off, the descriptor's position
     * unchanged: the count, or -1 at the end.
     */
    public static int pread(FileDescriptor fd, long pos, byte[] b, int off, int len) throws IOException {
        synchronized (fd) {
            int h = HostFiles.handle(fd);
            long saved = HostFiles.position(h);
            try {
                HostFiles.seek(h, pos);
                return HostFiles.read(h, b, off, len);
            } finally {
                HostFiles.seek(h, saved);
            }
        }
    }

    /** Writes len bytes of b from off to fd's file at pos, the descriptor's position unchanged. */
    public static void pwrite(FileDescriptor fd, long pos, byte[] b, int off, int len) throws IOException {
        synchronized (fd) {
            int h = HostFiles.handle(fd);
            long saved = HostFiles.position(h);
            try {
                HostFiles.seek(h, pos);
                HostFiles.write(h, b, off, len);
            } finally {
                HostFiles.seek(h, saved);
            }
        }
    }

    // -- the process's file locks --

    /** The locks held, by file key (an absolute path, or the descriptor of a stream without one). */
    private static final Map<Object, List<FileLock>> locks = new HashMap<>();

    /** The key of the locks of fd's file: path's absolute form when known, else fd itself. */
    public static Object lockKey(FileDescriptor fd, String path) {
        return (path == null) ? fd : new File(path).getAbsolutePath();
    }

    /**
     * Adds lock fl to key's locks: OverlappingFileLockException when a lock of the process
     * overlaps it, as the JDK's FileLockTable.add.
     */
    public static void addLock(Object key, FileLock fl) {
        synchronized (locks) {
            List<FileLock> list = locks.get(key);
            if (list == null) {
                list = new ArrayList<>();
                locks.put(key, list);
            }
            for (FileLock l : list) {
                if (l.overlaps(fl.position(), fl.size()))
                    throw new OverlappingFileLockException();
            }
            list.add(fl);
        }
    }

    /** Removes lock fl from key's locks. */
    public static void removeLock(Object key, FileLock fl) {
        synchronized (locks) {
            List<FileLock> list = locks.get(key);
            if (list != null) {
                list.remove(fl);
                if (list.isEmpty())
                    locks.remove(key);
            }
        }
    }

    /** Removes and returns key's locks acquired by channel ch (closing it). */
    public static List<FileLock> removeLocks(Object key, Object ch) {
        List<FileLock> out = new ArrayList<>();
        synchronized (locks) {
            List<FileLock> list = locks.get(key);
            if (list != null) {
                for (Iterator<FileLock> i = list.iterator(); i.hasNext(); ) {
                    FileLock l = i.next();
                    if (l.acquiredBy() == ch) {
                        out.add(l);
                        i.remove();
                    }
                }
                if (list.isEmpty())
                    locks.remove(key);
            }
        }
        return out;
    }

    // -- mappings --

    /**
     * A file's region mapped into a heap copy: the descriptor and path it was mapped through and
     * its position in the file, the copy's index 0. A mapping written back (MapMode.READ_WRITE)
     * writes its buffer's bytes at their positions in the file, never beyond the file's end (a
     * truncated file stays truncated, as the JDK's mapping of a truncated file is no longer
     * backed there); a private one (MapMode.PRIVATE) and a read-only one are never written.
     */
    public static final class Mapping {
        private final FileDescriptor fd;
        private final String path;
        private final long position;
        private final boolean writeBack;

        public Mapping(FileDescriptor fd, String path, long position, boolean writeBack) {
            this.fd = fd;
            this.path = path;
            this.position = position;
            this.writeBack = writeBack;
        }

        /** The descriptor the region was mapped through (MappedByteBuffer's fd). */
        public FileDescriptor fd() {
            return fd;
        }

        /**
         * Writes len bytes of the heap copy hb from index (the mapping's index 0 at hb's 0) to the
         * file: through the descriptor while it is open, else through the path opened again.
         */
        public void force(byte[] hb, int index, int len) throws IOException {
            if (!writeBack || len <= 0)
                return;
            if (fd.valid()) {
                synchronized (fd) {
                    int h = HostFiles.handle(fd);
                    long n = clip(HostFiles.length(h), index, len);
                    if (n > 0)
                        pwrite(fd, position + index, hb, index, (int) n);
                }
            } else if (path != null) {
                int h = HostFiles.open(path, HostFiles.READ_WRITE);
                try {
                    long n = clip(HostFiles.length(h), index, len);
                    if (n > 0) {
                        HostFiles.seek(h, position + index);
                        HostFiles.write(h, hb, index, (int) n);
                    }
                } finally {
                    HostFiles.close(h);
                }
            }
        }

        private long clip(long size, int index, int len) {
            long end = Math.min(size, position + index + len);
            return end - (position + index);
        }
    }

    /** The making of a mapped buffer of java.nio's (DirectByteBuffer's, jrt's own). */
    public interface MappedBuffers {
        MappedByteBuffer map(byte[] contents, Mapping m, boolean readOnly);
    }

    private static volatile MappedBuffers mappedBuffers;

    /** Called once, by DirectByteBuffer's static initializer. */
    public static void setMappedBuffers(MappedBuffers mb) {
        mappedBuffers = mb;
    }

    /** A mapped buffer of contents (read-only or not) for mapping m. */
    public static MappedByteBuffer map(byte[] contents, Mapping m, boolean readOnly) {
        if (mappedBuffers == null)
            ByteBuffer.allocateDirect(0); // initializes DirectByteBuffer
        return mappedBuffers.map(contents, m, readOnly);
    }
}
