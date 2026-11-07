/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F7 (write-ahead)"): jrt's
 * java.nio.DirectByteBufferR, in place of the JDK build's (generated from Direct-X-Buffer.java.template,
 * over memory addressed through Unsafe, which the Go build has not): a direct buffer of the Go
 * build is a heap buffer, its bytes a byte array, that is a MappedByteBuffer as the JDK's direct
 * buffers are (ByteBuffer.allocateDirect: no file; FileChannel.map: a file's region copied,
 * jdk.internal.jrt.HostFileChannels.Mapping). isDirect() answers false: the code that reads a
 * direct buffer through its address (jdk26u's isDirect branches) reads it as a heap buffer.
 * Derived mechanically from the JDK build's HeapByteBufferR.java (jdk26u at baf63fb,
 * make/modules/java.base/gensrc/GensrcBuffer.gmk from
 * src/java.base/share/classes/java/nio/Heap-X-Buffer.java.template; Copyright (c) Oracle and/or
 * its affiliates, GPL 2 with the Classpath Exception: LICENSE.md) by
 * the derivation "As built: F7 (write-ahead)" records: the class renamed, its superclass
 * MappedByteBuffer, the mapping passed to every buffer made from it, slice, duplicate and compact
 * returning a MappedByteBuffer.
 */
package java.nio;

import java.lang.foreign.MemorySegment;
import jdk.internal.foreign.AbstractMemorySegmentImpl;
import jdk.internal.foreign.MemorySessionImpl;
import jdk.internal.foreign.SegmentFactories;
import jdk.internal.vm.annotation.ForceInline;
import java.util.Objects;
import jdk.internal.jrt.HostFileChannels;

/**



 * A read-only DirectByteBuffer.  This class extends the corresponding
 * read/write class, overriding the mutation methods to throw a {@link
 * ReadOnlyBufferException} and overriding the view-buffer methods to return an
 * instance of this class rather than of the superclass.

 */



final

class DirectByteBufferR
    extends DirectByteBuffer



{















    DirectByteBufferR(int cap, int lim, MemorySegment segment) {            // package-private








        super(cap, lim, segment);
        this.isReadOnly = true;

    }

    DirectByteBufferR(byte[] buf, int off, int len, MemorySegment segment) { // package-private








        super(buf, off, len, segment);
        this.isReadOnly = true;

    }

    protected DirectByteBufferR(byte[] buf,
                                   int mark, int pos, int lim, int cap,
                                   int off, MemorySegment segment,
                                   HostFileChannels.Mapping mapping)
    {








        super(buf, mark, pos, lim, cap, off, segment, mapping);
        this.isReadOnly = true;

    }

    public MappedByteBuffer slice() {
        int pos = this.position();
        int lim = this.limit();
        int rem = (pos <= lim ? lim - pos : 0);
        return new DirectByteBufferR(hb,
                                        -1,
                                        0,
                                        rem,
                                        rem,
                                        pos + offset, segment, mapping);
    }

    @Override
    public MappedByteBuffer slice(int index, int length) {
        Objects.checkFromIndexSize(index, length, limit());
        return new DirectByteBufferR(hb,
                                        -1,
                                        0,
                                        length,
                                        length,
                                        index + offset, segment, mapping);
    }

    public MappedByteBuffer duplicate() {
        return new DirectByteBufferR(hb,
                                        this.markValue(),
                                        this.position(),
                                        this.limit(),
                                        this.capacity(),
                                        offset, segment, mapping);
    }

    public ByteBuffer asReadOnlyBuffer() {








        return duplicate();

    }




















































    public boolean isReadOnly() {
        return true;
    }

    public ByteBuffer put(byte x) {




        throw new ReadOnlyBufferException();

    }

    public ByteBuffer put(int i, byte x) {




        throw new ReadOnlyBufferException();

    }

    public ByteBuffer put(byte[] src, int offset, int length) {










        throw new ReadOnlyBufferException();

    }

    public ByteBuffer put(ByteBuffer src) {





        throw new ReadOnlyBufferException();

    }

    public ByteBuffer put(int index, ByteBuffer src, int offset, int length) {





        throw new ReadOnlyBufferException();

    }

    public ByteBuffer put(int index, byte[] src, int offset, int length) {







        throw new ReadOnlyBufferException();

    }





































































    public MappedByteBuffer compact() {











        throw new ReadOnlyBufferException();

    }





    byte _get(int i) {                          // package-private
        return hb[i];
    }

    void _put(int i, byte b) {                  // package-private



        throw new ReadOnlyBufferException();

    }

    // char













    public ByteBuffer putChar(char x) {




        throw new ReadOnlyBufferException();

    }

    public ByteBuffer putChar(int i, char x) {




        throw new ReadOnlyBufferException();

    }

    public CharBuffer asCharBuffer() {
        int pos = position();
        int size = (limit() - pos) >> 1;
        long addr = address + pos;
        return (bigEndian
                ? (CharBuffer)(new ByteBufferAsCharBufferRB(this,
                                                               -1,
                                                               0,
                                                               size,
                                                               size,
                                                               addr, segment))
                : (CharBuffer)(new ByteBufferAsCharBufferRL(this,
                                                               -1,
                                                               0,
                                                               size,
                                                               size,
                                                               addr, segment)));
    }


    // short













    public ByteBuffer putShort(short x) {




        throw new ReadOnlyBufferException();

    }

    public ByteBuffer putShort(int i, short x) {




        throw new ReadOnlyBufferException();

    }

    public ShortBuffer asShortBuffer() {
        int pos = position();
        int size = (limit() - pos) >> 1;
        long addr = address + pos;
        return (bigEndian
                ? (ShortBuffer)(new ByteBufferAsShortBufferRB(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 addr, segment))
                : (ShortBuffer)(new ByteBufferAsShortBufferRL(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 addr, segment)));
    }


    // int













    public ByteBuffer putInt(int x) {




        throw new ReadOnlyBufferException();

    }

    public ByteBuffer putInt(int i, int x) {




        throw new ReadOnlyBufferException();

    }

    public IntBuffer asIntBuffer() {
        int pos = position();
        int size = (limit() - pos) >> 2;
        long addr = address + pos;
        return (bigEndian
                ? (IntBuffer)(new ByteBufferAsIntBufferRB(this,
                                                             -1,
                                                             0,
                                                             size,
                                                             size,
                                                             addr, segment))
                : (IntBuffer)(new ByteBufferAsIntBufferRL(this,
                                                             -1,
                                                             0,
                                                             size,
                                                             size,
                                                             addr, segment)));
    }


    // long













    public ByteBuffer putLong(long x) {




        throw new ReadOnlyBufferException();

    }

    public ByteBuffer putLong(int i, long x) {




        throw new ReadOnlyBufferException();

    }

    public LongBuffer asLongBuffer() {
        int pos = position();
        int size = (limit() - pos) >> 3;
        long addr = address + pos;
        return (bigEndian
                ? (LongBuffer)(new ByteBufferAsLongBufferRB(this,
                                                               -1,
                                                               0,
                                                               size,
                                                               size,
                                                               addr, segment))
                : (LongBuffer)(new ByteBufferAsLongBufferRL(this,
                                                               -1,
                                                               0,
                                                               size,
                                                               size,
                                                               addr, segment)));
    }


    // float















    public ByteBuffer putFloat(float x) {





        throw new ReadOnlyBufferException();

    }

    public ByteBuffer putFloat(int i, float x) {





        throw new ReadOnlyBufferException();

    }

    public FloatBuffer asFloatBuffer() {
        int pos = position();
        int size = (limit() - pos) >> 2;
        long addr = address + pos;
        return (bigEndian
                ? (FloatBuffer)(new ByteBufferAsFloatBufferRB(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 addr, segment))
                : (FloatBuffer)(new ByteBufferAsFloatBufferRL(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 addr, segment)));
    }


    // double















    public ByteBuffer putDouble(double x) {





        throw new ReadOnlyBufferException();

    }

    public ByteBuffer putDouble(int i, double x) {





        throw new ReadOnlyBufferException();

    }

    public DoubleBuffer asDoubleBuffer() {
        int pos = position();
        int size = (limit() - pos) >> 3;
        long addr = address + pos;
        return (bigEndian
                ? (DoubleBuffer)(new ByteBufferAsDoubleBufferRB(this,
                                                                   -1,
                                                                   0,
                                                                   size,
                                                                   size,
                                                                   addr, segment))
                : (DoubleBuffer)(new ByteBufferAsDoubleBufferRL(this,
                                                                   -1,
                                                                   0,
                                                                   size,
                                                                   size,
                                                                   addr, segment)));
    }
































    @ForceInline
    @Override
    int scaleShifts() {
        return 0;
    }

    @ForceInline
    @Override
    AbstractMemorySegmentImpl heapSegment(Object base,
                                          long offset,
                                          long length,
                                          boolean readOnly,
                                          MemorySessionImpl bufferScope) {
        return SegmentFactories.arrayOfByteSegment(base, offset, length, readOnly, bufferScope);
    }














}
