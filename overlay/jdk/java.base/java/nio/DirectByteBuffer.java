/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F7 (write-ahead)"): jrt's
 * java.nio.DirectByteBuffer, in place of the JDK build's (generated from Direct-X-Buffer.java.template,
 * over memory addressed through Unsafe, which the Go build has not): a direct buffer of the Go
 * build is a heap buffer, its bytes a byte array, that is a MappedByteBuffer as the JDK's direct
 * buffers are (ByteBuffer.allocateDirect: no file; FileChannel.map: a file's region copied,
 * jdk.internal.jrt.HostFileChannels.Mapping). isDirect() answers false: the code that reads a
 * direct buffer through its address (jdk26u's isDirect branches) reads it as a heap buffer.
 * Derived mechanically from the JDK build's HeapByteBuffer.java (jdk26u at baf63fb,
 * make/modules/java.base/gensrc/GensrcBuffer.gmk from
 * src/java.base/share/classes/java/nio/Heap-X-Buffer.java.template; Copyright (c) Oracle and/or
 * its affiliates, GPL 2 with the Classpath Exception: LICENSE.md) by
 * the derivation "As built: F7 (write-ahead)" records: the class renamed, its superclass
 * MappedByteBuffer, the mapping passed to every buffer made from it, slice, duplicate and compact
 * returning a MappedByteBuffer; and DirectByteBuffer(int), ByteBuffer.allocateDirect's, the JDK's constructors over an
 * address (refusing: UnsupportedOperationException) and the static initializer giving HostFileChannels
 * the making of mapped buffers.
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

 * A read/write DirectByteBuffer.






 */

sealed



class DirectByteBuffer
    extends MappedByteBuffer

    permits DirectByteBufferR

{

    // Cached array base offset
    private static final long ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);

    // Cached array index scale
    private static final long ARRAY_INDEX_SCALE = UNSAFE.arrayIndexScale(byte[].class);

    // For speed these fields are actually declared in X-Buffer;
    // these declarations are here as documentation
    /*
    protected final byte[] hb;
    protected final int offset;
    */


    // ByteBuffer.allocateDirect's: a buffer of cap bytes, mapping no file
    DirectByteBuffer(int cap) {                                          // package-private
        this(cap, cap, null);
    }

    // The JDK's constructors over memory addresses (Buffer's JavaNioAccess: JNI's NewDirectByteBuffer,
    // memory segments' buffers): the Go build has no memory addressed so
    DirectByteBuffer(long addr, int cap, Object obj, MemorySegment segment) {
        this(addr, cap);
    }

    DirectByteBuffer(long addr, int cap, Object obj, java.io.FileDescriptor fd, boolean isSync,
                     MemorySegment segment) {
        this(addr, cap);
    }

    DirectByteBuffer(long addr, int cap) {
        this(0, 0, null);
        throw new UnsupportedOperationException("no direct memory in the Go build");
    }

    static {
        HostFileChannels.setMappedBuffers(new HostFileChannels.MappedBuffers() {
            public MappedByteBuffer map(byte[] contents, HostFileChannels.Mapping m, boolean readOnly) {
                int n = contents.length;
                return readOnly
                    ? new DirectByteBufferR(contents, -1, 0, n, n, 0, null, m)
                    : new DirectByteBuffer(contents, -1, 0, n, n, 0, null, m);
            }
        });
    }

    DirectByteBuffer(int cap, int lim, MemorySegment segment) {            // package-private

        super(-1, 0, lim, cap, new byte[cap], 0, segment, null);
        /*
        hb = new byte[cap];
        offset = 0;
        */
        this.address = ARRAY_BASE_OFFSET;




    }

    DirectByteBuffer(byte[] buf, int off, int len, MemorySegment segment) { // package-private

        super(-1, off, off + len, buf.length, buf, 0, segment, null);
        /*
        hb = buf;
        offset = 0;
        */
        this.address = ARRAY_BASE_OFFSET;




    }

    protected DirectByteBuffer(byte[] buf,
                                   int mark, int pos, int lim, int cap,
                                   int off, MemorySegment segment,
                                   HostFileChannels.Mapping mapping)
    {

        super(mark, pos, lim, cap, buf, off, segment, mapping);
        /*
        hb = buf;
        offset = off;
        */
        this.address = ARRAY_BASE_OFFSET + off * ARRAY_INDEX_SCALE;




    }

    public MappedByteBuffer slice() {
        int pos = this.position();
        int lim = this.limit();
        int rem = (pos <= lim ? lim - pos : 0);
        return new DirectByteBuffer(hb,
                                        -1,
                                        0,
                                        rem,
                                        rem,
                                        pos + offset, segment, mapping);
    }

    @Override
    public MappedByteBuffer slice(int index, int length) {
        Objects.checkFromIndexSize(index, length, limit());
        return new DirectByteBuffer(hb,
                                        -1,
                                        0,
                                        length,
                                        length,
                                        index + offset, segment, mapping);
    }

    public MappedByteBuffer duplicate() {
        return new DirectByteBuffer(hb,
                                        this.markValue(),
                                        this.position(),
                                        this.limit(),
                                        this.capacity(),
                                        offset, segment, mapping);
    }

    public ByteBuffer asReadOnlyBuffer() {

        return new DirectByteBufferR(hb,
                                     this.markValue(),
                                     this.position(),
                                     this.limit(),
                                     this.capacity(),
                                     offset, segment, mapping);



    }



    protected int ix(int i) {
        return i + offset;
    }


    private long byteOffset(long i) {
        return address + i;
    }


    public byte get() {
        return hb[ix(nextGetIndex())];
    }

    public byte get(int i) {
        return hb[ix(checkIndex(i))];
    }







    public ByteBuffer get(byte[] dst, int offset, int length) {
        checkSession();
        Objects.checkFromIndexSize(offset, length, dst.length);
        int pos = position();
        if (length > limit() - pos)
            throw new BufferUnderflowException();
        System.arraycopy(hb, ix(pos), dst, offset, length);
        position(pos + length);
        return this;
    }

    public ByteBuffer get(int index, byte[] dst, int offset, int length) {
        checkSession();
        Objects.checkFromIndexSize(index, length, limit());
        Objects.checkFromIndexSize(offset, length, dst.length);
        System.arraycopy(hb, ix(index), dst, offset, length);
        return this;
    }

    public boolean isDirect() {
        return false;
    }



    public boolean isReadOnly() {
        return false;
    }

    public ByteBuffer put(byte x) {

        hb[ix(nextPutIndex())] = x;
        return this;



    }

    public ByteBuffer put(int i, byte x) {

        hb[ix(checkIndex(i))] = x;
        return this;



    }

    public ByteBuffer put(byte[] src, int offset, int length) {

        checkSession();
        Objects.checkFromIndexSize(offset, length, src.length);
        int pos = position();
        if (length > limit() - pos)
            throw new BufferOverflowException();
        System.arraycopy(src, offset, hb, ix(pos), length);
        position(pos + length);
        return this;



    }

    public ByteBuffer put(ByteBuffer src) {

        checkSession();
        super.put(src);
        return this;



    }

    public ByteBuffer put(int index, ByteBuffer src, int offset, int length) {

        checkSession();
        super.put(index, src, offset, length);
        return this;



    }

    public ByteBuffer put(int index, byte[] src, int offset, int length) {

        checkSession();
        Objects.checkFromIndexSize(index, length, limit());
        Objects.checkFromIndexSize(offset, length, src.length);
        System.arraycopy(src, offset, hb, ix(index), length);
        return this;



    }





































































    public MappedByteBuffer compact() {

        int pos = position();
        int lim = limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        System.arraycopy(hb, ix(pos), hb, ix(0), rem);
        position(rem);
        limit(capacity());
        discardMark();
        return this;



    }





    byte _get(int i) {                          // package-private
        return hb[i];
    }

    void _put(int i, byte b) {                  // package-private

        hb[i] = b;



    }

    // char



    public char getChar() {
        return SCOPED_MEMORY_ACCESS.getCharUnaligned(session(), hb, byteOffset(nextGetIndex(2)), bigEndian);
    }

    public char getChar(int i) {
        return SCOPED_MEMORY_ACCESS.getCharUnaligned(session(), hb, byteOffset(checkIndex(i, 2)), bigEndian);
    }



    public ByteBuffer putChar(char x) {

        SCOPED_MEMORY_ACCESS.putCharUnaligned(session(), hb, byteOffset(nextPutIndex(2)), x, bigEndian);
        return this;



    }

    public ByteBuffer putChar(int i, char x) {

        SCOPED_MEMORY_ACCESS.putCharUnaligned(session(), hb, byteOffset(checkIndex(i, 2)), x, bigEndian);
        return this;



    }

    public CharBuffer asCharBuffer() {
        int pos = position();
        int size = (limit() - pos) >> 1;
        long addr = address + pos;
        return (bigEndian
                ? (CharBuffer)(new ByteBufferAsCharBufferB(this,
                                                               -1,
                                                               0,
                                                               size,
                                                               size,
                                                               addr, segment))
                : (CharBuffer)(new ByteBufferAsCharBufferL(this,
                                                               -1,
                                                               0,
                                                               size,
                                                               size,
                                                               addr, segment)));
    }


    // short



    public short getShort() {
        return SCOPED_MEMORY_ACCESS.getShortUnaligned(session(), hb, byteOffset(nextGetIndex(2)), bigEndian);
    }

    public short getShort(int i) {
        return SCOPED_MEMORY_ACCESS.getShortUnaligned(session(), hb, byteOffset(checkIndex(i, 2)), bigEndian);
    }



    public ByteBuffer putShort(short x) {

        SCOPED_MEMORY_ACCESS.putShortUnaligned(session(), hb, byteOffset(nextPutIndex(2)), x, bigEndian);
        return this;



    }

    public ByteBuffer putShort(int i, short x) {

        SCOPED_MEMORY_ACCESS.putShortUnaligned(session(), hb, byteOffset(checkIndex(i, 2)), x, bigEndian);
        return this;



    }

    public ShortBuffer asShortBuffer() {
        int pos = position();
        int size = (limit() - pos) >> 1;
        long addr = address + pos;
        return (bigEndian
                ? (ShortBuffer)(new ByteBufferAsShortBufferB(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 addr, segment))
                : (ShortBuffer)(new ByteBufferAsShortBufferL(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 addr, segment)));
    }


    // int



    public int getInt() {
        return SCOPED_MEMORY_ACCESS.getIntUnaligned(session(), hb, byteOffset(nextGetIndex(4)), bigEndian);
    }

    public int getInt(int i) {
        return SCOPED_MEMORY_ACCESS.getIntUnaligned(session(), hb, byteOffset(checkIndex(i, 4)), bigEndian);
    }



    public ByteBuffer putInt(int x) {

        SCOPED_MEMORY_ACCESS.putIntUnaligned(session(), hb, byteOffset(nextPutIndex(4)), x, bigEndian);
        return this;



    }

    public ByteBuffer putInt(int i, int x) {

        SCOPED_MEMORY_ACCESS.putIntUnaligned(session(), hb, byteOffset(checkIndex(i, 4)), x, bigEndian);
        return this;



    }

    public IntBuffer asIntBuffer() {
        int pos = position();
        int size = (limit() - pos) >> 2;
        long addr = address + pos;
        return (bigEndian
                ? (IntBuffer)(new ByteBufferAsIntBufferB(this,
                                                             -1,
                                                             0,
                                                             size,
                                                             size,
                                                             addr, segment))
                : (IntBuffer)(new ByteBufferAsIntBufferL(this,
                                                             -1,
                                                             0,
                                                             size,
                                                             size,
                                                             addr, segment)));
    }


    // long



    public long getLong() {
        return SCOPED_MEMORY_ACCESS.getLongUnaligned(session(), hb, byteOffset(nextGetIndex(8)), bigEndian);
    }

    public long getLong(int i) {
        return SCOPED_MEMORY_ACCESS.getLongUnaligned(session(), hb, byteOffset(checkIndex(i, 8)), bigEndian);
    }



    public ByteBuffer putLong(long x) {

        SCOPED_MEMORY_ACCESS.putLongUnaligned(session(), hb, byteOffset(nextPutIndex(8)), x, bigEndian);
        return this;



    }

    public ByteBuffer putLong(int i, long x) {

        SCOPED_MEMORY_ACCESS.putLongUnaligned(session(), hb, byteOffset(checkIndex(i, 8)), x, bigEndian);
        return this;



    }

    public LongBuffer asLongBuffer() {
        int pos = position();
        int size = (limit() - pos) >> 3;
        long addr = address + pos;
        return (bigEndian
                ? (LongBuffer)(new ByteBufferAsLongBufferB(this,
                                                               -1,
                                                               0,
                                                               size,
                                                               size,
                                                               addr, segment))
                : (LongBuffer)(new ByteBufferAsLongBufferL(this,
                                                               -1,
                                                               0,
                                                               size,
                                                               size,
                                                               addr, segment)));
    }


    // float



    public float getFloat() {
        int x = SCOPED_MEMORY_ACCESS.getIntUnaligned(session(), hb, byteOffset(nextGetIndex(4)), bigEndian);
        return Float.intBitsToFloat(x);
    }

    public float getFloat(int i) {
        int x = SCOPED_MEMORY_ACCESS.getIntUnaligned(session(), hb, byteOffset(checkIndex(i, 4)), bigEndian);
        return Float.intBitsToFloat(x);
    }



    public ByteBuffer putFloat(float x) {

        int y = Float.floatToRawIntBits(x);
        SCOPED_MEMORY_ACCESS.putIntUnaligned(session(), hb, byteOffset(nextPutIndex(4)), y, bigEndian);
        return this;



    }

    public ByteBuffer putFloat(int i, float x) {

        int y = Float.floatToRawIntBits(x);
        SCOPED_MEMORY_ACCESS.putIntUnaligned(session(), hb, byteOffset(checkIndex(i, 4)), y, bigEndian);
        return this;



    }

    public FloatBuffer asFloatBuffer() {
        int pos = position();
        int size = (limit() - pos) >> 2;
        long addr = address + pos;
        return (bigEndian
                ? (FloatBuffer)(new ByteBufferAsFloatBufferB(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 addr, segment))
                : (FloatBuffer)(new ByteBufferAsFloatBufferL(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 addr, segment)));
    }


    // double



    public double getDouble() {
        long x = SCOPED_MEMORY_ACCESS.getLongUnaligned(session(), hb, byteOffset(nextGetIndex(8)), bigEndian);
        return Double.longBitsToDouble(x);
    }

    public double getDouble(int i) {
        long x = SCOPED_MEMORY_ACCESS.getLongUnaligned(session(), hb, byteOffset(checkIndex(i, 8)), bigEndian);
        return Double.longBitsToDouble(x);
    }



    public ByteBuffer putDouble(double x) {

        long y = Double.doubleToRawLongBits(x);
        SCOPED_MEMORY_ACCESS.putLongUnaligned(session(), hb, byteOffset(nextPutIndex(8)), y, bigEndian);
        return this;



    }

    public ByteBuffer putDouble(int i, double x) {

        long y = Double.doubleToRawLongBits(x);
        SCOPED_MEMORY_ACCESS.putLongUnaligned(session(), hb, byteOffset(checkIndex(i, 8)), y, bigEndian);
        return this;



    }

    public DoubleBuffer asDoubleBuffer() {
        int pos = position();
        int size = (limit() - pos) >> 3;
        long addr = address + pos;
        return (bigEndian
                ? (DoubleBuffer)(new ByteBufferAsDoubleBufferB(this,
                                                                   -1,
                                                                   0,
                                                                   size,
                                                                   size,
                                                                   addr, segment))
                : (DoubleBuffer)(new ByteBufferAsDoubleBufferL(this,
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
