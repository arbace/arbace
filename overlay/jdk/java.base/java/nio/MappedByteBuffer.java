/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F7 (write-ahead)"): jrt's
 * java.nio.MappedByteBuffer, written for jrt in place of jdk26u's (whose buffers address memory
 * the VM maps, mmap, through Unsafe and ScopedMemoryAccess's native mapping operations). The Go
 * build maps a file region into a heap copy (jdk.internal.jrt.HostFileChannels.Mapping): the
 * buffer's bytes are a byte array, as a heap buffer's, read from the file when mapped, written
 * back to it by force and when the mapping's channel closes (a READ_WRITE mapping; READ_ONLY and
 * PRIVATE ones never are). A direct buffer (ByteBuffer.allocateDirect) is one too, with no file:
 * jrt's DirectByteBuffer, this class's one subclass, as in the JDK. The public members are
 * jdk26u's, with their documented results (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/java/nio/MappedByteBuffer.java; Copyright (c) Oracle and/or its
 * affiliates, GPL 2 with the Classpath Exception: LICENSE.md): isLoaded is true and load does
 * nothing (the copy is in memory), force(index, length) writes the region back; unmapper answers
 * null (no unmapping of a heap copy).
 * The rest Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.nio;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.foreign.MemorySegment;
import jdk.internal.access.foreign.UnmapperProxy;
import jdk.internal.jrt.HostFileChannels;

/**
 * A direct byte buffer whose content is a memory-mapped region of a file: in the Go build, a
 * heap copy of the region, written back to the file by {@link #force()}.
 */
public abstract sealed class MappedByteBuffer
    extends ByteBuffer
    permits DirectByteBuffer
{

    // The mapping of a file region, null for a buffer that maps no file (allocateDirect)
    final HostFileChannels.Mapping mapping;

    // For mapped buffers, the FileDescriptor the region was mapped through; null if the buffer
    // is not mapped (jdk26u's field)
    private final FileDescriptor fd;

    // Invoked by DirectByteBuffer's constructors: a buffer over the heap copy hb from offset
    MappedByteBuffer(int mark, int pos, int lim, int cap, // package-private
                     byte[] hb, int offset, MemorySegment segment,
                     HostFileChannels.Mapping mapping) {
        super(mark, pos, lim, cap, hb, offset, segment);
        this.mapping = mapping;
        this.fd = (mapping == null) ? null : mapping.fd();
    }

    // The JDK's unmapper of a buffer (Buffer's JavaNioAccess, for memory segments): none, a heap
    // copy is not unmapped
    UnmapperProxy unmapper() {
        return null;
    }

    final boolean isSync() { // package-private
        return false;
    }

    /**
     * Tells whether or not this buffer's content is resident in physical memory: always true, the
     * content being a heap copy.
     */
    public final boolean isLoaded() {
        return true;
    }

    /** Loads this buffer's content into physical memory: nothing to do. */
    public final MappedByteBuffer load() {
        return this;
    }

    /**
     * Forces any changes made to this buffer's content to be written to the storage device
     * containing the mapped file: the whole region, written back when the buffer was mapped
     * read/write.
     */
    public final MappedByteBuffer force() {
        if (fd == null) {
            return this;
        }
        return force(0, capacity());
    }

    /**
     * Forces any changes made to a region of this buffer's content to be written to the storage
     * device containing the mapped file.
     */
    public final MappedByteBuffer force(int index, int length) {
        if (fd == null) {
            return this;
        }
        int capacity = capacity();
        if ((index < 0) || (length < 0) || (index > (capacity - length))) {
            throw new IndexOutOfBoundsException("Index or length out of bounds: index " + index
                                                + ", length " + length + ", capacity " + capacity);
        }
        try {
            mapping.force(hb, offset + index, length);
        } catch (IOException x) {
            throw new UncheckedIOException(x);
        }
        return this;
    }

    // -- Covariant return type overrides (jdk26u's)

    @Override
    public final MappedByteBuffer position(int newPosition) {
        super.position(newPosition);
        return this;
    }

    @Override
    public final MappedByteBuffer limit(int newLimit) {
        super.limit(newLimit);
        return this;
    }

    @Override
    public final MappedByteBuffer mark() {
        super.mark();
        return this;
    }

    @Override
    public final MappedByteBuffer reset() {
        super.reset();
        return this;
    }

    @Override
    public final MappedByteBuffer clear() {
        super.clear();
        return this;
    }

    @Override
    public final MappedByteBuffer flip() {
        super.flip();
        return this;
    }

    @Override
    public final MappedByteBuffer rewind() {
        super.rewind();
        return this;
    }

    @Override
    public abstract MappedByteBuffer slice();

    @Override
    public abstract MappedByteBuffer slice(int index, int length);

    @Override
    public abstract MappedByteBuffer duplicate();

    @Override
    public abstract MappedByteBuffer compact();
}
