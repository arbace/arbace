/*
 * Arbace (overlay/jdk, doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JC6b"): jrt's
 * java.util.zip.Inflater, written for jrt in place of jdk26u's, whose decompression is zlib's C
 * code behind native methods over native memory (a Cleaner, java.nio's direct buffers): the Go
 * build has no zlib. The decompressor here is zlib's own inflate, ported to Java from zlib 1.3.2
 * as jdk26u bundles it (src/java.base/share/native/libzip/zlib: inflate.c, inflate.h, inftrees.c,
 * inftrees.h), line by line for the paths jdk26u's Inflater takes: the zlib format (window bits
 * 15, the check value validated) and the raw format (nowrap), a preset dictionary; not the gzip
 * format (java.util.zip.GZIPInputStream reads its own header and trailer) and not inflate_fast
 * (inffast.c), an equivalent faster path of the LEN state whose output and consumption are the
 * slow path's. Like jdk26u's glue (src/java.base/share/native/libzip/Inflater.c), each inflate
 * call is zlib's inflate with Z_PARTIAL_FLUSH, its result read as Inflater.c reads it. The public
 * API is jdk26u's documented java.util.zip.Inflater (https://github.com/openjdk/jdk26u at
 * baf63fb); ByteBuffer input and output are read and written through their arrays when they have
 * them, else copied (the Go build has no direct buffers).
 *
 * zlib: Copyright (C) 1995-2026 Jean-loup Gailly and Mark Adler, under the zlib License
 * (LICENSE.md). Arbace's port: Copyright (c) the Arbace authors, under the same zlib License.
 */
package java.util.zip;

import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.Objects;

import jdk.internal.util.Preconditions;

/** zlib's decompressor (a port of zlib 1.3.2's inflate, as above). */
public class Inflater implements AutoCloseable {

    private static final byte[] EMPTY = new byte[0];

    private IState zs;
    private ByteBuffer input;
    private byte[] inputArray = EMPTY;
    private int inputPos, inputLim;
    private boolean finished;
    private boolean pendingOutput;
    private boolean needDict;
    private long bytesRead;
    private long bytesWritten;

    /** The input consumed and output produced by an inflate call that failed. */
    private int inputConsumed;
    private int outputConsumed;

    /** A decompressor of the raw format (nowrap) or of the zlib format. */
    public Inflater(boolean nowrap) {
        this.zs = new IState(nowrap);
    }

    /** A decompressor of the zlib format. */
    public Inflater() {
        this(false);
    }

    public void setInput(byte[] input, int off, int len) {
        Preconditions.checkFromIndexSize(off, len, input.length, Preconditions.AIOOBE_FORMATTER);
        synchronized (this) {
            this.input = null;
            this.inputArray = input;
            this.inputPos = off;
            this.inputLim = off + len;
        }
    }

    public void setInput(byte[] input) {
        setInput(input, 0, input.length);
    }

    public void setInput(ByteBuffer input) {
        Objects.requireNonNull(input);
        synchronized (this) {
            this.input = input;
            this.inputArray = null;
        }
    }

    public void setDictionary(byte[] dictionary, int off, int len) {
        Preconditions.checkFromIndexSize(off, len, dictionary.length, Preconditions.AIOOBE_FORMATTER);
        synchronized (this) {
            ensureOpen();
            setDictionary0(dictionary, off, len);
            needDict = false;
        }
    }

    public void setDictionary(byte[] dictionary) {
        setDictionary(dictionary, 0, dictionary.length);
    }

    public void setDictionary(ByteBuffer dictionary) {
        synchronized (this) {
            int position = dictionary.position();
            int remaining = Math.max(dictionary.limit() - position, 0);
            ensureOpen();
            if (dictionary.hasArray()) {
                setDictionary0(dictionary.array(), dictionary.arrayOffset() + position, remaining);
            } else {
                byte[] copy = new byte[remaining];
                dictionary.get(position, copy, 0, remaining);
                setDictionary0(copy, 0, remaining);
            }
            dictionary.position(position + remaining);
            needDict = false;
        }
    }

    /** inflateSetDictionary, its result read as Inflater.c's checkSetDictionaryResult. */
    private void setDictionary0(byte[] b, int off, int len) {
        int res = zs.inflateSetDictionary(b, off, len);
        if (res == IState.Z_STREAM_ERROR || res == IState.Z_DATA_ERROR) {
            throw new IllegalArgumentException(zs.msg);
        }
    }

    public int getRemaining() {
        synchronized (this) {
            ByteBuffer input = this.input;
            return input == null ? inputLim - inputPos : input.remaining();
        }
    }

    public boolean needsInput() {
        synchronized (this) {
            ByteBuffer input = this.input;
            return input == null ? inputLim == inputPos : ! input.hasRemaining();
        }
    }

    public boolean needsDictionary() {
        synchronized (this) {
            return needDict;
        }
    }

    public boolean finished() {
        synchronized (this) {
            return finished;
        }
    }

    public int inflate(byte[] output, int off, int len) throws DataFormatException {
        Preconditions.checkFromIndexSize(off, len, output.length, Preconditions.AIOOBE_FORMATTER);
        synchronized (this) {
            ensureOpen();
            ByteBuffer input = this.input;
            long result;
            int inputPos;
            try {
                if (input == null) {
                    inputPos = this.inputPos;
                    try {
                        result = doInflate(inputArray, inputPos, inputLim - inputPos, output, off, len);
                    } catch (DataFormatException e) {
                        this.inputPos = inputPos + inputConsumed;
                        throw e;
                    }
                } else {
                    inputPos = input.position();
                    int inputRem = Math.max(input.limit() - inputPos, 0);
                    try {
                        if (input.hasArray()) {
                            result = doInflate(input.array(), input.arrayOffset() + inputPos, inputRem,
                                               output, off, len);
                        } else {
                            byte[] in = new byte[inputRem];
                            input.get(inputPos, in, 0, inputRem);
                            result = doInflate(in, 0, inputRem, output, off, len);
                        }
                    } catch (DataFormatException e) {
                        input.position(inputPos + inputConsumed);
                        throw e;
                    }
                }
            } catch (DataFormatException e) {
                bytesRead += inputConsumed;
                inputConsumed = 0;
                int written = outputConsumed;
                bytesWritten += written;
                outputConsumed = 0;
                throw e;
            }
            int read = (int) (result & 0x7fff_ffffL);
            int written = (int) (result >>> 31 & 0x7fff_ffffL);
            if ((result >>> 62 & 1) != 0) {
                finished = true;
            }
            pendingOutput = written == len && !finished;
            if ((result >>> 63 & 1) != 0) {
                needDict = true;
            }
            if (input != null) {
                if (read > 0) {
                    input.position(inputPos + read);
                }
            } else {
                this.inputPos = inputPos + read;
            }
            bytesWritten += written;
            bytesRead += read;
            return written;
        }
    }

    public int inflate(byte[] output) throws DataFormatException {
        return inflate(output, 0, output.length);
    }

    public int inflate(ByteBuffer output) throws DataFormatException {
        if (output.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        synchronized (this) {
            ensureOpen();
            int outputPos = output.position();
            int outputRem = Math.max(output.limit() - outputPos, 0);
            byte[] outArray;
            int outOff;
            if (output.hasArray()) {
                outArray = output.array();
                outOff = output.arrayOffset() + outputPos;
            } else {
                outArray = new byte[outputRem];
                outOff = 0;
            }
            ByteBuffer input = this.input;
            long result;
            int inputPos;
            try {
                if (input == null) {
                    inputPos = this.inputPos;
                    try {
                        result = doInflate(inputArray, inputPos, inputLim - inputPos, outArray, outOff, outputRem);
                    } catch (DataFormatException e) {
                        this.inputPos = inputPos + inputConsumed;
                        throw e;
                    }
                } else {
                    inputPos = input.position();
                    int inputRem = Math.max(input.limit() - inputPos, 0);
                    try {
                        if (input.hasArray()) {
                            result = doInflate(input.array(), input.arrayOffset() + inputPos, inputRem,
                                               outArray, outOff, outputRem);
                        } else {
                            byte[] in = new byte[inputRem];
                            input.get(inputPos, in, 0, inputRem);
                            result = doInflate(in, 0, inputRem, outArray, outOff, outputRem);
                        }
                    } catch (DataFormatException e) {
                        input.position(inputPos + inputConsumed);
                        throw e;
                    }
                }
            } catch (DataFormatException e) {
                bytesRead += inputConsumed;
                inputConsumed = 0;
                int written = outputConsumed;
                if (!output.hasArray()) {
                    output.put(outputPos, outArray, 0, written);
                }
                output.position(outputPos + written);
                bytesWritten += written;
                outputConsumed = 0;
                throw e;
            }
            int read = (int) (result & 0x7fff_ffffL);
            int written = (int) (result >>> 31 & 0x7fff_ffffL);
            if ((result >>> 62 & 1) != 0) {
                finished = true;
            }
            if ((result >>> 63 & 1) != 0) {
                needDict = true;
            }
            if (input != null) {
                input.position(inputPos + read);
            } else {
                this.inputPos = inputPos + read;
            }
            if (!output.hasArray()) {
                output.put(outputPos, outArray, 0, written);
            }
            output.position(outputPos + written);
            bytesWritten += written;
            bytesRead += read;
            return written;
        }
    }

    /**
     * zlib's inflate with Z_PARTIAL_FLUSH over the input and output given, its result packed as
     * Inflater.c's checkInflateStatus packs it: the input used (bits 0-30), the output written
     * (31-61), finished (62) and the dictionary needed (63); a data error a DataFormatException
     * with zlib's message, the input and output used in inputConsumed and outputConsumed.
     */
    private long doInflate(byte[] in, int inOff, int inLen, byte[] out, int outOff, int outLen)
            throws DataFormatException {
        IState s = zs;
        s.nextIn = in; s.nextInIndex = inOff; s.availIn = inLen;
        s.nextOut = out; s.nextOutIndex = outOff; s.availOut = outLen;
        int ret;
        try {
            ret = s.inflate(IState.Z_PARTIAL_FLUSH);
        } finally {
            s.nextIn = null; s.nextOut = null;
        }
        int inputUsed = 0, outputUsed = 0;
        int finished = 0, needDict = 0;
        switch (ret) {
            case IState.Z_STREAM_END:
                finished = 1;
                // fall through
            case IState.Z_OK:
                inputUsed = inLen - s.availIn;
                outputUsed = outLen - s.availOut;
                break;
            case IState.Z_NEED_DICT:
                needDict = 1;
                inputUsed = inLen - s.availIn;
                outputUsed = outLen - s.availOut;
                break;
            case IState.Z_BUF_ERROR:
                break;
            case IState.Z_DATA_ERROR:
                inputConsumed = inLen - s.availIn;
                outputConsumed = outLen - s.availOut;
                throw new DataFormatException(s.msg);
            default:
                throw new InternalError(s.msg);
        }
        return ((long) inputUsed) | (((long) outputUsed) << 31) | (((long) finished) << 62)
                | (((long) needDict) << 63);
    }

    public int getAdler() {
        synchronized (this) {
            ensureOpen();
            return (int) zs.adler;
        }
    }

    @Deprecated(since = "23")
    public int getTotalIn() {
        return (int) getBytesRead();
    }

    public long getBytesRead() {
        synchronized (this) {
            ensureOpen();
            return bytesRead;
        }
    }

    @Deprecated(since = "23")
    public int getTotalOut() {
        return (int) getBytesWritten();
    }

    public long getBytesWritten() {
        synchronized (this) {
            ensureOpen();
            return bytesWritten;
        }
    }

    public void reset() {
        synchronized (this) {
            ensureOpen();
            zs.inflateReset();
            input = null;
            inputArray = EMPTY;
            inputPos = inputLim = 0;
            finished = false;
            needDict = false;
            bytesRead = bytesWritten = 0;
        }
    }

    public void end() {
        synchronized (this) {
            if (zs == null) {
                return;
            }
            zs = null;
            input = null;
            inputArray = EMPTY;
            inputPos = inputLim = 0;
        }
    }

    @Override
    public void close() {
        end();
    }

    private void ensureOpen() {
        if (zs == null) {
            throw new IllegalStateException("Inflater has been closed");
        }
    }

    /** Whether the last inflate filled its output without finishing (jdk26u's pendingOutput). */
    boolean hasPendingOutput() {
        synchronized (this) {
            return pendingOutput;
        }
    }

    /**
     * zlib's z_stream and inflate_state in one (inflate.h), with inflate.c's and inftrees.c's
     * functions for the paths above, in zlib's names. A code (inftrees.h's code) is an int:
     * op in bits 24-31, bits in 16-23, val in 0-15.
     */
    private static final class IState {
        static final int Z_OK = 0, Z_STREAM_END = 1, Z_NEED_DICT = 2, Z_STREAM_ERROR = -2,
                Z_DATA_ERROR = -3, Z_MEM_ERROR = -4, Z_BUF_ERROR = -5;
        static final int Z_PARTIAL_FLUSH = 1, Z_FINISH = 4, Z_BLOCK = 5, Z_TREES = 6;
        static final int Z_DEFLATED = 8;
        static final int MAXBITS = 15;
        static final int ENOUGH_LENS = 852, ENOUGH_DISTS = 592, ENOUGH = ENOUGH_LENS + ENOUGH_DISTS;
        static final int CODES = 0, LENS = 1, DISTS = 2;

        // inflate_mode, in inflate.h's order (the gzip header's modes kept for the order)
        static final int HEAD = 0, FLAGS = 1, TIME = 2, OS = 3, EXLEN = 4, EXTRA = 5, NAME = 6,
                COMMENT = 7, HCRC = 8, DICTID = 9, DICT = 10, TYPE = 11, TYPEDO = 12, STORED = 13,
                COPY_ = 14, COPY = 15, TABLE = 16, LENLENS = 17, CODELENS = 18, LEN_ = 19, LEN = 20,
                LENEXT = 21, DIST = 22, DISTEXT = 23, MATCH = 24, LIT = 25, CHECK = 26, LENGTH = 27,
                DONE = 28, BAD = 29, MEM = 30, SYNC = 31;

        static final short[] order =
            {16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15};
        static final short[] lbase = {
            3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
            35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258, 0, 0};
        static final short[] lext = {
            16, 16, 16, 16, 16, 16, 16, 16, 17, 17, 17, 17, 18, 18, 18, 18,
            19, 19, 19, 19, 20, 20, 20, 20, 21, 21, 21, 21, 16, 199, 75};
        static final short[] dbase = {
            1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
            257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145,
            8193, 12289, 16385, 24577, 0, 0};
        static final short[] dext = {
            16, 16, 16, 16, 17, 17, 18, 18, 19, 19, 20, 20, 21, 21, 22, 22,
            23, 23, 24, 24, 25, 25, 26, 26, 27, 27,
            28, 28, 29, 29, 64, 64};

        // the fixed codes' tables, built as inftrees.c's buildtables builds them (BUILDFIXED)
        static final int[] fixed = new int[544];
        static final int lenfix, distfix;

        static {
            short[] lens = new short[288];
            short[] work = new short[288];
            int sym = 0;
            while (sym < 144) lens[sym++] = 8;
            while (sym < 256) lens[sym++] = 9;
            while (sym < 280) lens[sym++] = 7;
            while (sym < 288) lens[sym++] = 8;
            int[] next = {0};
            int[] bits = {9};
            inflate_table(LENS, lens, 0, 288, fixed, next, bits, work);
            lenfix = 0;
            sym = 0;
            while (sym < 32) lens[sym++] = 5;
            distfix = next[0];
            bits[0] = 5;
            inflate_table(DISTS, lens, 0, 32, fixed, next, bits, work);
        }

        static int op(int c) { return c >>> 24; }
        static int bits(int c) { return (c >>> 16) & 0xff; }
        static int val(int c) { return c & 0xffff; }
        static int code(int op, int bits, int val) { return (op << 24) | (bits << 16) | (val & 0xffff); }

        // the stream
        byte[] nextIn; int nextInIndex; int availIn;
        byte[] nextOut; int nextOutIndex; int availOut;
        long totalIn, totalOut;
        String msg;
        long adler;

        // the state
        int mode;
        boolean last;
        int wrap;
        boolean havedict;
        int flags;
        int dmax;
        long check;
        long total;
        int wbits;
        int wsize;
        int whave;
        int wnext;
        byte[] window;
        long hold;
        int bits;
        int length;
        int offset;
        int extra;
        int[] lencode; int lenIndex;
        int[] distcode; int distIndex;
        int lenbits;
        int distbits;
        int ncode;
        int nlen;
        int ndist;
        int have;
        int next;
        final short[] lens = new short[320];
        final short[] work = new short[288];
        final int[] codes = new int[ENOUGH];
        boolean sane;
        int back;
        int was;

        /** inflateInit2 with window bits 15, or -15 (raw). */
        IState(boolean nowrap) {
            mode = HEAD;
            int windowBits = nowrap ? -15 : 15;
            // inflateReset2
            if (windowBits < 0) {
                wrap = 0;
                windowBits = -windowBits;
            } else {
                wrap = (windowBits >> 4) + 5;
                if (windowBits < 48)
                    windowBits &= 15;
            }
            wbits = windowBits;
            inflateReset();
        }

        void inflateResetKeep() {
            totalIn = totalOut = total = 0;
            msg = null;
            if (wrap != 0)
                adler = wrap & 1;
            mode = HEAD;
            last = false;
            havedict = false;
            flags = -1;
            dmax = 32768;
            hold = 0;
            bits = 0;
            lencode = distcode = codes;
            lenIndex = distIndex = next = 0;
            sane = true;
            back = -1;
        }

        void inflateReset() {
            wsize = 0;
            whave = 0;
            wnext = 0;
            inflateResetKeep();
        }

        static long adler32(long adler, byte[] buf, int off, int len) {
            long a = adler & 0xffff, b = (adler >>> 16) & 0xffff;
            while (len > 0) {
                int n = Math.min(len, 5552);
                len -= n;
                while (n-- > 0) {
                    a += buf[off++] & 0xff;
                    b += a;
                }
                a %= 65521;
                b %= 65521;
            }
            return (b << 16) | a;
        }

        /** updatewindow: the last wsize bytes of output ending at end into the window. */
        void updatewindow(byte[] src, int end, int copy) {
            int dist;
            if (window == null) {
                window = new byte[1 << wbits];
            }
            if (wsize == 0) {
                wsize = 1 << wbits;
                wnext = 0;
                whave = 0;
            }
            if (copy >= wsize) {
                System.arraycopy(src, end - wsize, window, 0, wsize);
                wnext = 0;
                whave = wsize;
            } else {
                dist = wsize - wnext;
                if (dist > copy) dist = copy;
                System.arraycopy(src, end - copy, window, wnext, dist);
                copy -= dist;
                if (copy != 0) {
                    System.arraycopy(src, end - copy, window, 0, copy);
                    wnext = copy;
                    whave = wsize;
                } else {
                    wnext += dist;
                    if (wnext == wsize) wnext = 0;
                    if (whave < wsize) whave += dist;
                }
            }
        }

        static long zswap32(long q) {
            return ((q >>> 24) & 0xff) | ((q >>> 8) & 0xff00) | ((q & 0xff00) << 8) | ((q & 0xff) << 24);
        }

        int inflate(int flush) {
            int next;           // next input
            int put;            // next output
            int have, left;     // available input and output
            long hold;          // bit buffer
            int bits;           // bits in bit buffer
            int in, out;        // save starting available input and output
            int copy;           // number of stored or match bytes to copy
            byte[] from;        // where to copy match bytes from
            int fromIndex;
            int here;           // current decoding table entry
            int last;           // parent table entry
            int len;            // length to copy for repeats, bits to drop
            int ret;            // return code
            byte[] input = nextIn, output = nextOut;

            if (output == null || (input == null && availIn != 0))
                return Z_STREAM_ERROR;

            if (mode == TYPE) mode = TYPEDO;    // skip check
            put = nextOutIndex; left = availOut; next = nextInIndex; have = availIn;
            hold = this.hold; bits = this.bits;
            in = have;
            out = left;
            ret = Z_OK;
            inf_leave:
            for (;;) {
                switch (mode) {
                    case HEAD:
                        if (wrap == 0) {
                            mode = TYPEDO;
                            break;
                        }
                        while (bits < 16) {
                            if (have == 0) break inf_leave;
                            have--;
                            hold += (long) (input[next++] & 0xff) << bits;
                            bits += 8;
                        }
                        if ((wrap & 1) == 0 ||
                                ((((int) hold & 0xff) << 8) + (int) (hold >>> 8)) % 31 != 0) {
                            msg = "incorrect header check";
                            mode = BAD;
                            break;
                        }
                        if (((int) hold & 0xf) != Z_DEFLATED) {
                            msg = "unknown compression method";
                            mode = BAD;
                            break;
                        }
                        hold >>>= 4; bits -= 4;
                        len = ((int) hold & 0xf) + 8;
                        if (wbits == 0)
                            wbits = len;
                        if (len > 15 || len > wbits) {
                            msg = "invalid window size";
                            mode = BAD;
                            break;
                        }
                        dmax = 1 << len;
                        flags = 0;              // indicate zlib header
                        adler = check = 1;      // adler32(0L, Z_NULL, 0)
                        mode = (hold & 0x200) != 0 ? DICTID : TYPE;
                        hold = 0; bits = 0;
                        break;
                    case DICTID:
                        while (bits < 32) {
                            if (have == 0) break inf_leave;
                            have--;
                            hold += (long) (input[next++] & 0xff) << bits;
                            bits += 8;
                        }
                        adler = check = zswap32(hold & 0xffffffffL);
                        hold = 0; bits = 0;
                        mode = DICT;
                        // fall through
                    case DICT:
                        if (!havedict) {
                            nextOutIndex = put; availOut = left; nextInIndex = next; availIn = have;
                            this.hold = hold; this.bits = bits;
                            return Z_NEED_DICT;
                        }
                        adler = check = 1;
                        mode = TYPE;
                        // fall through
                    case TYPE:
                        if (flush == Z_BLOCK || flush == Z_TREES) break inf_leave;
                        // fall through
                    case TYPEDO:
                        if (this.last) {
                            hold >>>= bits & 7; bits -= bits & 7;
                            mode = CHECK;
                            break;
                        }
                        while (bits < 3) {
                            if (have == 0) break inf_leave;
                            have--;
                            hold += (long) (input[next++] & 0xff) << bits;
                            bits += 8;
                        }
                        this.last = ((int) hold & 1) != 0;
                        hold >>>= 1; bits -= 1;
                        switch ((int) hold & 3) {
                            case 0:                     // stored block
                                mode = STORED;
                                break;
                            case 1:                     // fixed block
                                lencode = fixed; lenIndex = lenfix;
                                lenbits = 9;
                                distcode = fixed; distIndex = distfix;
                                distbits = 5;
                                mode = LEN_;            // decode codes
                                if (flush == Z_TREES) {
                                    hold >>>= 2; bits -= 2;
                                    break inf_leave;
                                }
                                break;
                            case 2:                     // dynamic block
                                mode = TABLE;
                                break;
                            default:
                                msg = "invalid block type";
                                mode = BAD;
                        }
                        hold >>>= 2; bits -= 2;
                        break;
                    case STORED:
                        hold >>>= bits & 7; bits -= bits & 7;   // go to byte boundary
                        while (bits < 32) {
                            if (have == 0) break inf_leave;
                            have--;
                            hold += (long) (input[next++] & 0xff) << bits;
                            bits += 8;
                        }
                        if ((hold & 0xffff) != (((hold >>> 16) & 0xffff) ^ 0xffff)) {
                            msg = "invalid stored block lengths";
                            mode = BAD;
                            break;
                        }
                        length = (int) hold & 0xffff;
                        hold = 0; bits = 0;
                        mode = COPY_;
                        if (flush == Z_TREES) break inf_leave;
                        // fall through
                    case COPY_:
                        mode = COPY;
                        // fall through
                    case COPY:
                        copy = length;
                        if (copy != 0) {
                            if (copy > have) copy = have;
                            if (copy > left) copy = left;
                            if (copy == 0) break inf_leave;
                            System.arraycopy(input, next, output, put, copy);
                            have -= copy;
                            next += copy;
                            left -= copy;
                            put += copy;
                            length -= copy;
                            break;
                        }
                        mode = TYPE;
                        break;
                    case TABLE:
                        while (bits < 14) {
                            if (have == 0) break inf_leave;
                            have--;
                            hold += (long) (input[next++] & 0xff) << bits;
                            bits += 8;
                        }
                        nlen = ((int) hold & 0x1f) + 257;
                        hold >>>= 5; bits -= 5;
                        ndist = ((int) hold & 0x1f) + 1;
                        hold >>>= 5; bits -= 5;
                        ncode = ((int) hold & 0xf) + 4;
                        hold >>>= 4; bits -= 4;
                        if (nlen > 286 || ndist > 30) {
                            msg = "too many length or distance symbols";
                            mode = BAD;
                            break;
                        }
                        this.have = 0;
                        mode = LENLENS;
                        // fall through
                    case LENLENS:
                        while (this.have < ncode) {
                            while (bits < 3) {
                                if (have == 0) break inf_leave;
                                have--;
                                hold += (long) (input[next++] & 0xff) << bits;
                                bits += 8;
                            }
                            lens[order[this.have++]] = (short) ((int) hold & 7);
                            hold >>>= 3; bits -= 3;
                        }
                        while (this.have < 19)
                            lens[order[this.have++]] = 0;
                        this.next = 0;
                        lencode = distcode = codes;
                        lenIndex = distIndex = 0;
                        lenbits = 7;
                        {
                            int[] nx = {this.next};
                            int[] bx = {lenbits};
                            ret = inflate_table(CODES, lens, 0, 19, codes, nx, bx, work);
                            this.next = nx[0];
                            lenbits = bx[0];
                        }
                        if (ret != 0) {
                            msg = "invalid code lengths set";
                            mode = BAD;
                            break;
                        }
                        this.have = 0;
                        mode = CODELENS;
                        // fall through
                    case CODELENS:
                        while (this.have < nlen + ndist) {
                            for (;;) {
                                here = lencode[lenIndex + ((int) hold & ((1 << lenbits) - 1))];
                                if (bits(here) <= bits) break;
                                if (have == 0) break inf_leave;
                                have--;
                                hold += (long) (input[next++] & 0xff) << bits;
                                bits += 8;
                            }
                            if (val(here) < 16) {
                                hold >>>= bits(here); bits -= bits(here);
                                lens[this.have++] = (short) val(here);
                            } else {
                                if (val(here) == 16) {
                                    while (bits < bits(here) + 2) {
                                        if (have == 0) break inf_leave;
                                        have--;
                                        hold += (long) (input[next++] & 0xff) << bits;
                                        bits += 8;
                                    }
                                    hold >>>= bits(here); bits -= bits(here);
                                    if (this.have == 0) {
                                        msg = "invalid bit length repeat";
                                        mode = BAD;
                                        break;
                                    }
                                    len = lens[this.have - 1];
                                    copy = 3 + ((int) hold & 3);
                                    hold >>>= 2; bits -= 2;
                                } else if (val(here) == 17) {
                                    while (bits < bits(here) + 3) {
                                        if (have == 0) break inf_leave;
                                        have--;
                                        hold += (long) (input[next++] & 0xff) << bits;
                                        bits += 8;
                                    }
                                    hold >>>= bits(here); bits -= bits(here);
                                    len = 0;
                                    copy = 3 + ((int) hold & 7);
                                    hold >>>= 3; bits -= 3;
                                } else {
                                    while (bits < bits(here) + 7) {
                                        if (have == 0) break inf_leave;
                                        have--;
                                        hold += (long) (input[next++] & 0xff) << bits;
                                        bits += 8;
                                    }
                                    hold >>>= bits(here); bits -= bits(here);
                                    len = 0;
                                    copy = 11 + ((int) hold & 0x7f);
                                    hold >>>= 7; bits -= 7;
                                }
                                if (this.have + copy > nlen + ndist) {
                                    msg = "invalid bit length repeat";
                                    mode = BAD;
                                    break;
                                }
                                while (copy-- != 0)
                                    lens[this.have++] = (short) len;
                            }
                        }

                        // handle error breaks in while
                        if (mode == BAD) break;

                        // check for end-of-block code (better have one)
                        if (lens[256] == 0) {
                            msg = "invalid code -- missing end-of-block";
                            mode = BAD;
                            break;
                        }

                        // build code tables
                        this.next = 0;
                        lencode = codes; lenIndex = 0;
                        lenbits = 9;
                        {
                            int[] nx = {this.next};
                            int[] bx = {lenbits};
                            ret = inflate_table(LENS, lens, 0, nlen, codes, nx, bx, work);
                            this.next = nx[0];
                            lenbits = bx[0];
                        }
                        if (ret != 0) {
                            msg = "invalid literal/lengths set";
                            mode = BAD;
                            break;
                        }
                        distcode = codes; distIndex = this.next;
                        distbits = 6;
                        {
                            int[] nx = {this.next};
                            int[] bx = {distbits};
                            ret = inflate_table(DISTS, lens, nlen, ndist, codes, nx, bx, work);
                            this.next = nx[0];
                            distbits = bx[0];
                        }
                        if (ret != 0) {
                            msg = "invalid distances set";
                            mode = BAD;
                            break;
                        }
                        mode = LEN_;
                        if (flush == Z_TREES) break inf_leave;
                        // fall through
                    case LEN_:
                        mode = LEN;
                        // fall through
                    case LEN:
                        back = 0;
                        for (;;) {
                            here = lencode[lenIndex + ((int) hold & ((1 << lenbits) - 1))];
                            if (bits(here) <= bits) break;
                            if (have == 0) break inf_leave;
                            have--;
                            hold += (long) (input[next++] & 0xff) << bits;
                            bits += 8;
                        }
                        if (op(here) != 0 && (op(here) & 0xf0) == 0) {
                            last = here;
                            for (;;) {
                                here = lencode[lenIndex + val(last)
                                        + (((int) hold & ((1 << (bits(last) + op(last))) - 1)) >>> bits(last))];
                                if (bits(last) + bits(here) <= bits) break;
                                if (have == 0) break inf_leave;
                                have--;
                                hold += (long) (input[next++] & 0xff) << bits;
                                bits += 8;
                            }
                            hold >>>= bits(last); bits -= bits(last);
                            back += bits(last);
                        }
                        hold >>>= bits(here); bits -= bits(here);
                        back += bits(here);
                        length = val(here);
                        if (op(here) == 0) {
                            mode = LIT;
                            break;
                        }
                        if ((op(here) & 32) != 0) {
                            back = -1;
                            mode = TYPE;
                            break;
                        }
                        if ((op(here) & 64) != 0) {
                            msg = "invalid literal/length code";
                            mode = BAD;
                            break;
                        }
                        extra = op(here) & 15;
                        mode = LENEXT;
                        // fall through
                    case LENEXT:
                        if (extra != 0) {
                            while (bits < extra) {
                                if (have == 0) break inf_leave;
                                have--;
                                hold += (long) (input[next++] & 0xff) << bits;
                                bits += 8;
                            }
                            length += (int) hold & ((1 << extra) - 1);
                            hold >>>= extra; bits -= extra;
                            back += extra;
                        }
                        was = length;
                        mode = DIST;
                        // fall through
                    case DIST:
                        for (;;) {
                            here = distcode[distIndex + ((int) hold & ((1 << distbits) - 1))];
                            if (bits(here) <= bits) break;
                            if (have == 0) break inf_leave;
                            have--;
                            hold += (long) (input[next++] & 0xff) << bits;
                            bits += 8;
                        }
                        if ((op(here) & 0xf0) == 0) {
                            last = here;
                            for (;;) {
                                here = distcode[distIndex + val(last)
                                        + (((int) hold & ((1 << (bits(last) + op(last))) - 1)) >>> bits(last))];
                                if (bits(last) + bits(here) <= bits) break;
                                if (have == 0) break inf_leave;
                                have--;
                                hold += (long) (input[next++] & 0xff) << bits;
                                bits += 8;
                            }
                            hold >>>= bits(last); bits -= bits(last);
                            back += bits(last);
                        }
                        hold >>>= bits(here); bits -= bits(here);
                        back += bits(here);
                        if ((op(here) & 64) != 0) {
                            msg = "invalid distance code";
                            mode = BAD;
                            break;
                        }
                        offset = val(here);
                        extra = op(here) & 15;
                        mode = DISTEXT;
                        // fall through
                    case DISTEXT:
                        if (extra != 0) {
                            while (bits < extra) {
                                if (have == 0) break inf_leave;
                                have--;
                                hold += (long) (input[next++] & 0xff) << bits;
                                bits += 8;
                            }
                            offset += (int) hold & ((1 << extra) - 1);
                            hold >>>= extra; bits -= extra;
                            back += extra;
                        }
                        mode = MATCH;
                        // fall through
                    case MATCH:
                        if (left == 0) break inf_leave;
                        copy = out - left;
                        if (offset > copy) {            // copy from window
                            copy = offset - copy;
                            if (copy > whave) {
                                if (sane) {
                                    msg = "invalid distance too far back";
                                    mode = BAD;
                                    break;
                                }
                            }
                            if (copy > wnext) {
                                copy -= wnext;
                                from = window; fromIndex = wsize - copy;
                            } else {
                                from = window; fromIndex = wnext - copy;
                            }
                            if (copy > length) copy = length;
                        } else {                        // copy from output
                            from = output; fromIndex = put - offset;
                            copy = length;
                        }
                        if (copy > left) copy = left;
                        left -= copy;
                        length -= copy;
                        do {
                            output[put++] = from[fromIndex++];
                        } while (--copy != 0);
                        if (length == 0) mode = LEN;
                        break;
                    case LIT:
                        if (left == 0) break inf_leave;
                        output[put++] = (byte) length;
                        left--;
                        mode = LEN;
                        break;
                    case CHECK:
                        if (wrap != 0) {
                            while (bits < 32) {
                                if (have == 0) break inf_leave;
                                have--;
                                hold += (long) (input[next++] & 0xff) << bits;
                                bits += 8;
                            }
                            out -= left;
                            totalOut += out;
                            total += out;
                            if ((wrap & 4) != 0 && out != 0)
                                adler = check = adler32(check, output, put - out, out);
                            out = left;
                            if ((wrap & 4) != 0 && zswap32(hold & 0xffffffffL) != check) {
                                msg = "incorrect data check";
                                mode = BAD;
                                break;
                            }
                            hold = 0; bits = 0;
                        }
                        mode = LENGTH;
                        // fall through
                    case LENGTH:
                        mode = DONE;
                        // fall through
                    case DONE:
                        ret = Z_STREAM_END;
                        break inf_leave;
                    case BAD:
                        ret = Z_DATA_ERROR;
                        break inf_leave;
                    case MEM:
                        return Z_MEM_ERROR;
                    default:
                        return Z_STREAM_ERROR;
                }
            }

            // inf_leave: the totals, the check value and the window updated
            nextOutIndex = put; availOut = left; nextInIndex = next; availIn = have;
            this.hold = hold; this.bits = bits;
            if (wsize != 0 || (out != availOut && mode < BAD && (mode < CHECK || flush != Z_FINISH)))
                updatewindow(output, nextOutIndex, out - availOut);
            in -= availIn;
            out -= availOut;
            totalIn += in;
            totalOut += out;
            total += out;
            if ((wrap & 4) != 0 && out != 0)
                adler = check = adler32(check, output, nextOutIndex - out, out);
            if (((in == 0 && out == 0) || flush == Z_FINISH) && ret == Z_OK)
                ret = Z_BUF_ERROR;
            return ret;
        }

        int inflateSetDictionary(byte[] dictionary, int off, int dictLength) {
            if (wrap != 0 && mode != DICT)
                return Z_STREAM_ERROR;
            if (mode == DICT) {
                long dictid = adler32(1, dictionary, off, dictLength);
                if (dictid != check)
                    return Z_DATA_ERROR;
            }
            updatewindow(dictionary, off + dictLength, dictLength);
            havedict = true;
            return Z_OK;
        }

        /**
         * inftrees.c's inflate_table: the decoding table of the code lengths lens[off, off +
         * codes) into table from next[0], next[0] and bits[0] updated as zlib's *table and
         * *bits are.
         */
        static int inflate_table(int type, short[] lens, int off, int codes, int[] table,
                                 int[] nextp, int[] bitsp, short[] work) {
            int len;            // a code's length in bits
            int sym;            // index of code symbols
            int min, max;       // minimum and maximum code lengths
            int root;           // number of index bits for root table
            int curr;           // number of index bits for current table
            int drop;           // code bits to drop for sub-table
            int left;           // number of prefix codes available
            int used;           // code entries in table used
            int huff;           // Huffman code
            int incr;           // for incrementing code, index
            int fill;           // index for replicating entries
            int low;            // low bits for current root entry
            int mask;           // mask for low root bits
            int hereOp, hereBits, hereVal;  // table entry for duplication
            int next;           // next available space in table
            short[] base = null;    // base value table to use
            short[] extra = null;   // extra bits table to use
            int match = 0;          // use base and extra for symbol >= match
            short[] count = new short[MAXBITS + 1];     // number of codes of each length
            short[] offs = new short[MAXBITS + 1];      // offsets in table for each length
            int tableStart = nextp[0];

            for (len = 0; len <= MAXBITS; len++)
                count[len] = 0;
            for (sym = 0; sym < codes; sym++)
                count[lens[off + sym]]++;

            root = bitsp[0];
            for (max = MAXBITS; max >= 1; max--)
                if (count[max] != 0) break;
            if (root > max) root = max;
            if (max == 0) {                     // no symbols to code at all
                int c = code(64, 1, 0);         // invalid code marker
                table[nextp[0]++] = c;          // make a table to force an error
                table[nextp[0]++] = c;
                bitsp[0] = 1;
                return 0;     // no symbols, but wait for decoding to report error
            }
            for (min = 1; min < max; min++)
                if (count[min] != 0) break;
            if (root < min) root = min;

            // check for an over-subscribed or incomplete set of lengths
            left = 1;
            for (len = 1; len <= MAXBITS; len++) {
                left <<= 1;
                left -= count[len];
                if (left < 0) return -1;        // over-subscribed
            }
            if (left > 0 && (type == CODES || max != 1))
                return -1;                      // incomplete set

            // generate offsets into symbol table for each length for sorting
            offs[1] = 0;
            for (len = 1; len < MAXBITS; len++)
                offs[len + 1] = (short) (offs[len] + count[len]);

            // sort symbols by length, by symbol order within each length
            for (sym = 0; sym < codes; sym++)
                if (lens[off + sym] != 0) work[offs[lens[off + sym]]++] = (short) sym;

            // set up for code type
            switch (type) {
                case CODES:
                    match = 20;
                    break;
                case LENS:
                    base = lbase;
                    extra = lext;
                    match = 257;
                    break;
                default:    // DISTS
                    base = dbase;
                    extra = dext;
            }

            // initialize state for loop
            huff = 0;                   // starting code
            sym = 0;                    // starting code symbol
            len = min;                  // starting code length
            next = tableStart;          // current table to fill in
            curr = root;                // current table index bits
            drop = 0;                   // current bits to drop from code for index
            low = -1;                   // trigger new sub-table when len > root
            used = 1 << root;           // use root table entries
            mask = used - 1;            // mask for comparing low

            // check available table space
            if ((type == LENS && used > ENOUGH_LENS) ||
                (type == DISTS && used > ENOUGH_DISTS))
                return 1;

            // process all codes and make table entries
            for (;;) {
                // create table entry
                hereBits = len - drop;
                int w = work[sym] & 0xffff;
                if (w + 1 < match) {
                    hereOp = 0;
                    hereVal = w;
                } else if (w >= match) {
                    hereOp = extra[w - match] & 0xff;
                    hereVal = base[w - match] & 0xffff;
                } else {
                    hereOp = 32 + 64;           // end of block
                    hereVal = 0;
                }
                int here = code(hereOp, hereBits, hereVal);

                // replicate for those indices with low len bits equal to huff
                incr = 1 << (len - drop);
                fill = 1 << curr;
                min = fill;                 // save offset to next table
                do {
                    fill -= incr;
                    table[next + (huff >>> drop) + fill] = here;
                } while (fill != 0);

                // backwards increment the len-bit code huff
                incr = 1 << (len - 1);
                while ((huff & incr) != 0)
                    incr >>>= 1;
                if (incr != 0) {
                    huff &= incr - 1;
                    huff += incr;
                } else
                    huff = 0;

                // go to next symbol, update count, len
                sym++;
                if (--(count[len]) == 0) {
                    if (len == max) break;
                    len = lens[off + (work[sym] & 0xffff)];
                }

                // create new sub-table if needed
                if (len > root && (huff & mask) != low) {
                    // if first time, transition to sub-tables
                    if (drop == 0)
                        drop = root;

                    // increment past last table
                    next += min;            // here min is 1 << curr

                    // determine length of next table
                    curr = len - drop;
                    left = 1 << curr;
                    while (curr + drop < max) {
                        left -= count[curr + drop];
                        if (left <= 0) break;
                        curr++;
                        left <<= 1;
                    }

                    // check for enough space
                    used += 1 << curr;
                    if ((type == LENS && used > ENOUGH_LENS) ||
                        (type == DISTS && used > ENOUGH_DISTS))
                        return 1;

                    // point entry in root table to sub-table
                    low = huff & mask;
                    table[tableStart + low] = code(curr, root, next - tableStart);
                }
            }

            // fill in remaining table entry if code is incomplete
            if (huff != 0) {
                table[next + huff] = code(64, len - drop, 0);   // invalid code marker
            }

            // set return parameters
            nextp[0] = tableStart + used;
            bitsp[0] = root;
            return 0;
        }
    }
}
