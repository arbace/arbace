/*
 * Arbace (overlay/jdk/javac, doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN7"): jrt's
 * java.util.zip.Deflater for the world of javac's tool build, written for jrt in place of
 * jdk26u's, whose compression is zlib's C code behind native methods: the Go build has no zlib,
 * and Go's compress/flate writes other bytes. The JDK build's tools that run on Arbace write
 * zlib streams that must be the JDK's byte for byte (build.tools.generatecharacter.CharacterName's
 * uniName.dat, through DeflaterOutputStream), so the compressor here is zlib's own deflate,
 * ported to Java from zlib 1.3.2 as jdk26u bundles it (src/java.base/share/native/libzip/zlib:
 * deflate.c, deflate.h, trees.c; the host JDK's system zlib is the same release), line by line
 * for the paths it supports: the zlib and raw formats, levels 1 to 9 and the default (6), the
 * default and filtered strategies, the flush modes NO_FLUSH, SYNC_FLUSH, FULL_FLUSH and finish;
 * not a preset dictionary, level 0 (stored), HUFFMAN_ONLY or a change of the parameters after
 * the first deflate call (they throw UnsupportedOperationException). The public API is
 * jdk26u's (https://github.com/openjdk/jdk26u at baf63fb), its JNI glue
 * (src/java.base/share/native/libzip/Deflater.c) followed in deflate's return value.
 *
 * zlib: Copyright (C) 1995-2026 Jean-loup Gailly and Mark Adler, under the zlib License
 * (LICENSE.md). Arbace's port: Copyright (c) the Arbace authors, under the same zlib License.
 */
package java.util.zip;

import java.nio.ByteBuffer;
import java.util.Objects;

/** zlib's compressor (a port of zlib 1.3.2's deflate, as above). */
public class Deflater implements AutoCloseable {

    public static final int DEFLATED = 8;
    public static final int NO_COMPRESSION = 0;
    public static final int BEST_SPEED = 1;
    public static final int BEST_COMPRESSION = 9;
    public static final int DEFAULT_COMPRESSION = -1;
    public static final int FILTERED = 1;
    public static final int HUFFMAN_ONLY = 2;
    public static final int DEFAULT_STRATEGY = 0;
    public static final int NO_FLUSH = 0;
    public static final int SYNC_FLUSH = 2;
    public static final int FULL_FLUSH = 3;
    private static final int FINISH = 4;

    private ZState zs;
    private byte[] inputArray = new byte[0];
    private int inputPos, inputLim;
    private int level, strategy;
    private boolean setParams;
    private boolean finish, finished;
    private long bytesRead, bytesWritten;
    private final boolean nowrap;

    public Deflater(int level, boolean nowrap) {
        this.level = level;
        this.strategy = DEFAULT_STRATEGY;
        this.nowrap = nowrap;
        this.zs = new ZState(level, DEFAULT_STRATEGY, nowrap);
    }

    public Deflater(int level) {
        this(level, false);
    }

    public Deflater() {
        this(DEFAULT_COMPRESSION, false);
    }

    public void setInput(byte[] input, int off, int len) {
        Objects.checkFromIndexSize(off, len, input.length);
        synchronized (this) {
            this.inputArray = input;
            this.inputPos = off;
            this.inputLim = off + len;
        }
    }

    public void setInput(byte[] input) {
        setInput(input, 0, input.length);
    }

    public void setInput(ByteBuffer input) {
        throw new UnsupportedOperationException("Deflater.setInput(ByteBuffer) on Arbace");
    }

    public void setDictionary(byte[] dictionary, int off, int len) {
        throw new UnsupportedOperationException("Deflater.setDictionary on Arbace");
    }

    public void setDictionary(byte[] dictionary) {
        setDictionary(dictionary, 0, dictionary.length);
    }

    public void setStrategy(int strategy) {
        switch (strategy) {
            case DEFAULT_STRATEGY: case FILTERED: case HUFFMAN_ONLY: break;
            default: throw new IllegalArgumentException();
        }
        synchronized (this) {
            if (this.strategy != strategy) {
                this.strategy = strategy;
                setParams = true;
            }
        }
    }

    public void setLevel(int level) {
        if ((level < 0 || level > 9) && level != DEFAULT_COMPRESSION) {
            throw new IllegalArgumentException("invalid compression level");
        }
        synchronized (this) {
            if (this.level != level) {
                this.level = level;
                setParams = true;
            }
        }
    }

    public boolean needsInput() {
        synchronized (this) {
            return inputLim == inputPos;
        }
    }

    public void finish() {
        synchronized (this) {
            finish = true;
        }
    }

    public boolean finished() {
        synchronized (this) {
            return finished;
        }
    }

    public int deflate(byte[] output, int off, int len) {
        return deflate(output, off, len, NO_FLUSH);
    }

    public int deflate(byte[] output) {
        return deflate(output, 0, output.length, NO_FLUSH);
    }

    public int deflate(ByteBuffer output) {
        throw new UnsupportedOperationException("Deflater.deflate(ByteBuffer) on Arbace");
    }

    public int deflate(byte[] output, int off, int len, int flush) {
        Objects.checkFromIndexSize(off, len, output.length);
        if (flush != NO_FLUSH && flush != SYNC_FLUSH && flush != FULL_FLUSH) {
            throw new IllegalArgumentException();
        }
        synchronized (this) {
            ensureOpen();
            if (finish) {
                flush = FINISH;
            }
            if (setParams) {
                // deflateParams before the stream has started is a new configuration (zlib's
                // deflateParams with last_flush == -2); later it would flush a block
                if (!zs.fresh()) {
                    throw new UnsupportedOperationException("Deflater: parameters changed after deflate on Arbace");
                }
                zs = new ZState(level, strategy, nowrap);
                setParams = false;
            }
            ZState s = zs;
            s.nextIn = inputArray; s.nextInIndex = inputPos; s.availIn = inputLim - inputPos;
            s.nextOut = output; s.nextOutIndex = off; s.availOut = len;
            int res = s.deflate(flush);
            int read = (inputLim - inputPos) - s.availIn;
            int written = len - s.availOut;
            if (res == ZState.Z_STREAM_END) {
                finished = true;
            }
            s.nextIn = null; s.nextOut = null;
            inputPos += read;
            bytesWritten += written;
            bytesRead += read;
            return written;
        }
    }

    public int deflate(ByteBuffer output, int flush) {
        throw new UnsupportedOperationException("Deflater.deflate(ByteBuffer, int) on Arbace");
    }

    public int getAdler() {
        synchronized (this) {
            ensureOpen();
            return (int) zs.adler;
        }
    }

    public int getTotalIn() {
        return (int) getBytesRead();
    }

    public long getBytesRead() {
        synchronized (this) {
            ensureOpen();
            return bytesRead;
        }
    }

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
            zs = new ZState(level, strategy, nowrap);
            setParams = false;
            finish = false;
            finished = false;
            inputArray = new byte[0];
            inputPos = inputLim = 0;
            bytesRead = bytesWritten = 0;
        }
    }

    public void end() {
        synchronized (this) {
            zs = null;
            inputArray = null;
        }
    }

    @Override
    public void close() {
        end();
    }

    private void ensureOpen() {
        if (zs == null) {
            throw new IllegalStateException("Deflater has been closed");
        }
    }

    /**
     * zlib's z_stream and deflate_state in one (deflate.h), with deflate.c's and trees.c's
     * functions for the supported paths, in zlib's names.
     */
    private static final class ZState {
        static final int Z_OK = 0, Z_STREAM_END = 1, Z_BUF_ERROR = -5;
        static final int Z_NO_FLUSH = 0, Z_PARTIAL_FLUSH = 1, Z_SYNC_FLUSH = 2, Z_FULL_FLUSH = 3,
                Z_FINISH = 4, Z_BLOCK = 5;
        static final int Z_FILTERED = 1;
        static final int MIN_MATCH = 3, MAX_MATCH = 258;
        static final int MIN_LOOKAHEAD = MAX_MATCH + MIN_MATCH + 1;
        static final int WIN_INIT = MAX_MATCH;
        static final int TOO_FAR = 4096;
        static final int NIL = 0;
        static final int LENGTH_CODES = 29, LITERALS = 256, L_CODES = LITERALS + 1 + LENGTH_CODES,
                D_CODES = 30, BL_CODES = 19, HEAP_SIZE = 2 * L_CODES + 1, MAX_BITS = 15,
                MAX_BL_BITS = 7, END_BLOCK = 256, REP_3_6 = 16, REPZ_3_10 = 17, REPZ_11_138 = 18,
                Buf_size = 16;
        static final int STORED_BLOCK = 0, STATIC_TREES = 1, DYN_TREES = 2;
        static final int INIT_STATE = 42, BUSY_STATE = 113, FINISH_STATE = 666;
        // block_state
        static final int need_more = 0, block_done = 1, finish_started = 2, finish_done = 3;

        // configuration_table: good lazy nice chain, and whether deflate_slow
        static final int[][] CONFIG = {
            {0, 0, 0, 0, 0}, {4, 4, 8, 4, 0}, {4, 5, 16, 8, 0}, {4, 6, 32, 32, 0},
            {4, 4, 16, 16, 1}, {8, 16, 32, 32, 1}, {8, 16, 128, 128, 1}, {8, 32, 128, 256, 1},
            {32, 128, 258, 1024, 1}, {32, 258, 258, 4096, 1}};

        static final int[] extra_lbits =
            {0,0,0,0,0,0,0,0,1,1,1,1,2,2,2,2,3,3,3,3,4,4,4,4,5,5,5,5,0};
        static final int[] extra_dbits =
            {0,0,0,0,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10,11,11,12,12,13,13};
        static final int[] extra_blbits =
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,3,7};
        static final int[] bl_order =
            {16,17,18,0,8,7,9,6,10,5,11,4,12,3,13,2,14,1,15};

        // trees.c's static tables, made as tr_static_init makes them
        static final short[] static_ltree_code = new short[L_CODES + 2];
        static final short[] static_ltree_len = new short[L_CODES + 2];
        static final short[] static_dtree_code = new short[D_CODES];
        static final short[] static_dtree_len = new short[D_CODES];
        static final byte[] _dist_code = new byte[512];
        static final byte[] _length_code = new byte[MAX_MATCH - MIN_MATCH + 1];
        static final int[] base_length = new int[LENGTH_CODES];
        static final int[] base_dist = new int[D_CODES];

        static {
            int n, bits, length, code, dist;
            short[] bl_count = new short[MAX_BITS + 1];
            length = 0;
            for (code = 0; code < LENGTH_CODES - 1; code++) {
                base_length[code] = length;
                for (n = 0; n < (1 << extra_lbits[code]); n++) {
                    _length_code[length++] = (byte) code;
                }
            }
            _length_code[length - 1] = (byte) code;
            dist = 0;
            for (code = 0; code < 16; code++) {
                base_dist[code] = dist;
                for (n = 0; n < (1 << extra_dbits[code]); n++) {
                    _dist_code[dist++] = (byte) code;
                }
            }
            dist >>= 7;
            for (; code < D_CODES; code++) {
                base_dist[code] = dist << 7;
                for (n = 0; n < (1 << (extra_dbits[code] - 7)); n++) {
                    _dist_code[256 + dist++] = (byte) code;
                }
            }
            for (bits = 0; bits <= MAX_BITS; bits++) bl_count[bits] = 0;
            n = 0;
            while (n <= 143) { static_ltree_len[n++] = 8; bl_count[8]++; }
            while (n <= 255) { static_ltree_len[n++] = 9; bl_count[9]++; }
            while (n <= 279) { static_ltree_len[n++] = 7; bl_count[7]++; }
            while (n <= 287) { static_ltree_len[n++] = 8; bl_count[8]++; }
            gen_codes(static_ltree_code, static_ltree_len, L_CODES + 1, bl_count);
            for (n = 0; n < D_CODES; n++) {
                static_dtree_len[n] = 5;
                static_dtree_code[n] = (short) bi_reverse(n, 5);
            }
        }

        // the stream
        byte[] nextIn; int nextInIndex; int availIn;
        byte[] nextOut; int nextOutIndex; int availOut;
        long totalIn, totalOut;
        long adler;

        // the state
        int status;
        final byte[] pending_buf;
        int pending_out;
        int pending;
        int wrap;
        int last_flush;
        final int w_size, w_bits, w_mask;
        final byte[] window;
        int window_size;
        final short[] prev;
        final short[] head;
        int ins_h;
        final int hash_size, hash_bits, hash_mask, hash_shift;
        int block_start;
        int match_length, prev_match;
        boolean match_available;
        int strstart, match_start, lookahead;
        int prev_length;
        int max_chain_length, max_lazy_match;
        final int level, strategy;
        int good_match, nice_match;
        final boolean slow;

        // trees: the dynamic trees' freq/code and dad/len unions as two arrays each
        final short[] dyn_ltree_fc = new short[HEAP_SIZE], dyn_ltree_dl = new short[HEAP_SIZE];
        final short[] dyn_dtree_fc = new short[2 * D_CODES + 1], dyn_dtree_dl = new short[2 * D_CODES + 1];
        final short[] bl_tree_fc = new short[2 * BL_CODES + 1], bl_tree_dl = new short[2 * BL_CODES + 1];
        final TreeDesc l_desc, d_desc, bl_desc;
        final short[] bl_count = new short[MAX_BITS + 1];
        final int[] heap = new int[2 * L_CODES + 1];
        int heap_len, heap_max;
        final byte[] depth = new byte[2 * L_CODES + 1];
        final byte[] sym_buf;
        final int lit_bufsize;
        int sym_next, sym_end;
        long opt_len, static_len;
        int matches;
        int insert;
        int bi_buf;
        int bi_valid;
        int high_water;

        static final class TreeDesc {
            final short[] fc, dl;
            int max_code;
            final short[] stree_len; // the static tree's lengths, or null
            final int[] extra_bits;
            final int extra_base, elems, max_length;

            TreeDesc(short[] fc, short[] dl, short[] stree_len, int[] extra_bits, int extra_base,
                     int elems, int max_length) {
                this.fc = fc; this.dl = dl; this.stree_len = stree_len; this.extra_bits = extra_bits;
                this.extra_base = extra_base; this.elems = elems; this.max_length = max_length;
            }
        }

        /** deflateInit2 (window bits 15, or -15 raw; memLevel 8), then deflateReset. */
        ZState(int level, int strategy, boolean nowrap) {
            if (level == DEFAULT_COMPRESSION) level = 6;
            if (level == 0) {
                throw new UnsupportedOperationException("Deflater: level 0 (stored) on Arbace");
            }
            if (strategy == HUFFMAN_ONLY) {
                throw new UnsupportedOperationException("Deflater: HUFFMAN_ONLY on Arbace");
            }
            int memLevel = 8;
            wrap = nowrap ? 0 : 1;
            w_bits = 15;
            w_size = 1 << w_bits;
            w_mask = w_size - 1;
            hash_bits = memLevel + 7;
            hash_size = 1 << hash_bits;
            hash_mask = hash_size - 1;
            hash_shift = (hash_bits + MIN_MATCH - 1) / MIN_MATCH;
            window = new byte[w_size * 2];
            prev = new short[w_size];
            head = new short[hash_size];
            high_water = 0;
            lit_bufsize = 1 << (memLevel + 6);
            // pending_buf and sym_buf apart (zlib overlays them, never overlapping what it uses)
            pending_buf = new byte[lit_bufsize * 4];
            sym_buf = new byte[lit_bufsize * 3];
            sym_end = (lit_bufsize - 1) * 3;
            this.level = level;
            this.strategy = strategy;
            this.slow = CONFIG[level][4] == 1;
            l_desc = new TreeDesc(dyn_ltree_fc, dyn_ltree_dl, static_ltree_len, extra_lbits, LITERALS + 1, L_CODES, MAX_BITS);
            d_desc = new TreeDesc(dyn_dtree_fc, dyn_dtree_dl, static_dtree_len, extra_dbits, 0, D_CODES, MAX_BITS);
            bl_desc = new TreeDesc(bl_tree_fc, bl_tree_dl, null, extra_blbits, 0, BL_CODES, MAX_BL_BITS);
            // deflateResetKeep
            totalIn = totalOut = 0;
            pending = 0;
            pending_out = 0;
            status = INIT_STATE;
            adler = 1;
            last_flush = -2;
            _tr_init();
            // lm_init
            window_size = 2 * w_size;
            // CLEAR_HASH: head is zero
            max_lazy_match = CONFIG[level][1];
            good_match = CONFIG[level][0];
            nice_match = CONFIG[level][2];
            max_chain_length = CONFIG[level][3];
            strstart = 0;
            block_start = 0;
            lookahead = 0;
            insert = 0;
            match_length = prev_length = MIN_MATCH - 1;
            match_available = false;
            ins_h = 0;
        }

        boolean fresh() {
            return last_flush == -2;
        }

        static int adler32(long adler, byte[] buf, int off, int len) {
            long a = adler & 0xffff, b = (adler >>> 16) & 0xffff;
            for (int i = 0; i < len; i++) {
                a = (a + (buf[off + i] & 0xff)) % 65521;
                b = (b + a) % 65521;
            }
            return (int) ((b << 16) | a);
        }

        // ---------------------------------------------------------------- deflate.c

        void put_byte(int c) {
            pending_buf[pending++] = (byte) c;
        }

        void putShortMSB(int b) {
            put_byte(b >> 8);
            put_byte(b & 0xff);
        }

        int maxDist() {
            return w_size - MIN_LOOKAHEAD;
        }

        int updateHash(int h, int c) {
            return ((h << hash_shift) ^ c) & hash_mask;
        }

        /** INSERT_STRING: returns match_head. */
        int insertString(int str) {
            ins_h = updateHash(ins_h, window[str + (MIN_MATCH - 1)] & 0xff);
            int match_head = head[ins_h] & 0xffff;
            prev[str & w_mask] = (short) match_head;
            head[ins_h] = (short) str;
            return match_head;
        }

        void clearHash() {
            java.util.Arrays.fill(head, (short) 0);
        }

        void slide_hash() {
            int n, m;
            int wsize = w_size;
            n = hash_size;
            int p = n;
            do {
                m = head[--p] & 0xffff;
                head[p] = (short) (m >= wsize ? m - wsize : NIL);
            } while (--n != 0);
            n = wsize;
            p = n;
            do {
                m = prev[--p] & 0xffff;
                prev[p] = (short) (m >= wsize ? m - wsize : NIL);
            } while (--n != 0);
        }

        int read_buf(byte[] buf, int off, int size) {
            int len = availIn;
            if (len > size) len = size;
            if (len == 0) return 0;
            availIn -= len;
            System.arraycopy(nextIn, nextInIndex, buf, off, len);
            if (wrap == 1) {
                adler = adler32(adler, buf, off, len) & 0xffffffffL;
            }
            nextInIndex += len;
            totalIn += len;
            return len;
        }

        void fill_window() {
            int n;
            int more;
            int wsize = w_size;
            do {
                more = window_size - lookahead - strstart;
                if (strstart >= wsize + maxDist()) {
                    System.arraycopy(window, wsize, window, 0, wsize - more);
                    match_start -= wsize;
                    strstart -= wsize;
                    block_start -= wsize;
                    if (insert > strstart)
                        insert = strstart;
                    slide_hash();
                    more += wsize;
                }
                if (availIn == 0) break;
                n = read_buf(window, strstart + lookahead, more);
                lookahead += n;
                if (lookahead + insert >= MIN_MATCH) {
                    int str = strstart - insert;
                    ins_h = window[str] & 0xff;
                    ins_h = updateHash(ins_h, window[str + 1] & 0xff);
                    while (insert != 0) {
                        ins_h = updateHash(ins_h, window[str + MIN_MATCH - 1] & 0xff);
                        prev[str & w_mask] = head[ins_h];
                        head[ins_h] = (short) str;
                        str++;
                        insert--;
                        if (lookahead + insert < MIN_MATCH)
                            break;
                    }
                }
            } while (lookahead < MIN_LOOKAHEAD && availIn != 0);
            if (high_water < window_size) {
                int curr = strstart + lookahead;
                int init;
                if (high_water < curr) {
                    init = window_size - curr;
                    if (init > WIN_INIT)
                        init = WIN_INIT;
                    java.util.Arrays.fill(window, curr, curr + init, (byte) 0);
                    high_water = curr + init;
                } else if (high_water < curr + WIN_INIT) {
                    init = curr + WIN_INIT - high_water;
                    if (init > window_size - high_water)
                        init = window_size - high_water;
                    java.util.Arrays.fill(window, high_water, high_water + init, (byte) 0);
                    high_water += init;
                }
            }
        }

        void flush_pending() {
            _tr_flush_bits();
            int len = pending > availOut ? availOut : pending;
            if (len == 0) return;
            System.arraycopy(pending_buf, pending_out, nextOut, nextOutIndex, len);
            nextOutIndex += len;
            pending_out += len;
            totalOut += len;
            availOut -= len;
            pending -= len;
            if (pending == 0) {
                pending_out = 0;
            }
        }

        static int rank(int f) {
            return (f * 2) - (f > 4 ? 9 : 0);
        }

        int deflate(int flush) {
            int old_flush;
            if (availOut == 0) return Z_BUF_ERROR;
            if (status == FINISH_STATE && flush != Z_FINISH) {
                throw new IllegalStateException("Deflater: deflate after finish");
            }
            old_flush = last_flush;
            last_flush = flush;
            if (pending != 0) {
                flush_pending();
                if (availOut == 0) {
                    last_flush = -1;
                    return Z_OK;
                }
            } else if (availIn == 0 && rank(flush) <= rank(old_flush) && flush != Z_FINISH) {
                return Z_BUF_ERROR;
            }
            if (status == FINISH_STATE && availIn != 0) {
                return Z_BUF_ERROR;
            }
            if (status == INIT_STATE && wrap == 0)
                status = BUSY_STATE;
            if (status == INIT_STATE) {
                int header = (DEFLATED + ((w_bits - 8) << 4)) << 8;
                int level_flags;
                if (level < 2)
                    level_flags = 0;
                else if (level < 6)
                    level_flags = 1;
                else if (level == 6)
                    level_flags = 2;
                else
                    level_flags = 3;
                header |= (level_flags << 6);
                header += 31 - (header % 31);
                putShortMSB(header);
                adler = 1;
                status = BUSY_STATE;
                flush_pending();
                if (pending != 0) {
                    last_flush = -1;
                    return Z_OK;
                }
            }
            if (availIn != 0 || lookahead != 0 || (flush != Z_NO_FLUSH && status != FINISH_STATE)) {
                int bstate = slow ? deflate_slow(flush) : deflate_fast(flush);
                if (bstate == finish_started || bstate == finish_done) {
                    status = FINISH_STATE;
                }
                if (bstate == need_more || bstate == finish_started) {
                    if (availOut == 0) {
                        last_flush = -1;
                    }
                    return Z_OK;
                }
                if (bstate == block_done) {
                    if (flush == Z_PARTIAL_FLUSH) {
                        _tr_align();
                    } else if (flush != Z_BLOCK) {
                        _tr_stored_block(null, 0, 0, false);
                        if (flush == Z_FULL_FLUSH) {
                            clearHash();
                            if (lookahead == 0) {
                                strstart = 0;
                                block_start = 0;
                                insert = 0;
                            }
                        }
                    }
                    flush_pending();
                    if (availOut == 0) {
                        last_flush = -1;
                        return Z_OK;
                    }
                }
            }
            if (flush != Z_FINISH) return Z_OK;
            if (wrap <= 0) return Z_STREAM_END;
            putShortMSB((int) (adler >>> 16));
            putShortMSB((int) (adler & 0xffff));
            flush_pending();
            if (wrap > 0) wrap = -wrap;
            return pending != 0 ? Z_OK : Z_STREAM_END;
        }

        int longest_match(int cur_match) {
            int chain_length = max_chain_length;
            int scan = strstart;
            int match;
            int len;
            int best_len = prev_length;
            int nice = nice_match;
            int limit = strstart > maxDist() ? strstart - maxDist() : NIL;
            int wmask = w_mask;
            int strend = strstart + MAX_MATCH;
            byte scan_end1 = window[scan + best_len - 1];
            byte scan_end = window[scan + best_len];
            if (prev_length >= good_match) {
                chain_length >>>= 2;
            }
            if (nice > lookahead) nice = lookahead;
            do {
                match = cur_match;
                if (window[match + best_len] != scan_end ||
                    window[match + best_len - 1] != scan_end1 ||
                    window[match] != window[scan] ||
                    window[++match] != window[scan + 1]) continue;
                scan += 2;
                match++;
                do {
                } while (window[++scan] == window[++match] && window[++scan] == window[++match] &&
                         window[++scan] == window[++match] && window[++scan] == window[++match] &&
                         window[++scan] == window[++match] && window[++scan] == window[++match] &&
                         window[++scan] == window[++match] && window[++scan] == window[++match] &&
                         scan < strend);
                len = MAX_MATCH - (strend - scan);
                scan = strend - MAX_MATCH;
                if (len > best_len) {
                    match_start = cur_match;
                    best_len = len;
                    if (len >= nice) break;
                    scan_end1 = window[scan + best_len - 1];
                    scan_end = window[scan + best_len];
                }
            } while ((cur_match = prev[cur_match & wmask] & 0xffff) > limit
                     && --chain_length != 0);
            if (best_len <= lookahead) return best_len;
            return lookahead;
        }

        /** FLUSH_BLOCK_ONLY */
        void flushBlockOnly(boolean last) {
            _tr_flush_block(block_start >= 0 ? window : null, block_start >= 0 ? block_start : 0,
                            strstart - block_start, last);
            block_start = strstart;
            flush_pending();
        }

        boolean tallyLit(int c) {
            sym_buf[sym_next++] = 0;
            sym_buf[sym_next++] = 0;
            sym_buf[sym_next++] = (byte) c;
            dyn_ltree_fc[c]++;
            return sym_next == sym_end;
        }

        boolean tallyDist(int distance, int length) {
            int len = length & 0xff;
            int dist = distance & 0xffff;
            sym_buf[sym_next++] = (byte) dist;
            sym_buf[sym_next++] = (byte) (dist >> 8);
            sym_buf[sym_next++] = (byte) len;
            dist--;
            dyn_ltree_fc[(_length_code[len] & 0xff) + LITERALS + 1]++;
            dyn_dtree_fc[d_code(dist)]++;
            return sym_next == sym_end;
        }

        static int d_code(int dist) {
            return dist < 256 ? _dist_code[dist] & 0xff : _dist_code[256 + (dist >> 7)] & 0xff;
        }

        int deflate_fast(int flush) {
            int hash_head;
            boolean bflush;
            for (;;) {
                if (lookahead < MIN_LOOKAHEAD) {
                    fill_window();
                    if (lookahead < MIN_LOOKAHEAD && flush == Z_NO_FLUSH) {
                        return need_more;
                    }
                    if (lookahead == 0) break;
                }
                hash_head = NIL;
                if (lookahead >= MIN_MATCH) {
                    hash_head = insertString(strstart);
                }
                if (hash_head != NIL && strstart - hash_head <= maxDist()) {
                    match_length = longest_match(hash_head);
                }
                if (match_length >= MIN_MATCH) {
                    bflush = tallyDist(strstart - match_start, match_length - MIN_MATCH);
                    lookahead -= match_length;
                    // max_insert_length is max_lazy_match
                    if (match_length <= max_lazy_match && lookahead >= MIN_MATCH) {
                        match_length--;
                        do {
                            strstart++;
                            hash_head = insertString(strstart);
                        } while (--match_length != 0);
                        strstart++;
                    } else {
                        strstart += match_length;
                        match_length = 0;
                        ins_h = window[strstart] & 0xff;
                        ins_h = updateHash(ins_h, window[strstart + 1] & 0xff);
                    }
                } else {
                    bflush = tallyLit(window[strstart] & 0xff);
                    lookahead--;
                    strstart++;
                }
                if (bflush) {
                    flushBlockOnly(false);
                    if (availOut == 0) return need_more;
                }
            }
            insert = strstart < MIN_MATCH - 1 ? strstart : MIN_MATCH - 1;
            if (flush == Z_FINISH) {
                flushBlockOnly(true);
                if (availOut == 0) return finish_started;
                return finish_done;
            }
            if (sym_next != 0) {
                flushBlockOnly(false);
                if (availOut == 0) return need_more;
            }
            return block_done;
        }

        int deflate_slow(int flush) {
            int hash_head;
            boolean bflush;
            for (;;) {
                if (lookahead < MIN_LOOKAHEAD) {
                    fill_window();
                    if (lookahead < MIN_LOOKAHEAD && flush == Z_NO_FLUSH) {
                        return need_more;
                    }
                    if (lookahead == 0) break;
                }
                hash_head = NIL;
                if (lookahead >= MIN_MATCH) {
                    hash_head = insertString(strstart);
                }
                prev_length = match_length;
                prev_match = match_start;
                match_length = MIN_MATCH - 1;
                if (hash_head != NIL && prev_length < max_lazy_match &&
                    strstart - hash_head <= maxDist()) {
                    match_length = longest_match(hash_head);
                    if (match_length <= 5 && (strategy == Z_FILTERED
                        || (match_length == MIN_MATCH &&
                            strstart - match_start > TOO_FAR))) {
                        match_length = MIN_MATCH - 1;
                    }
                }
                if (prev_length >= MIN_MATCH && match_length <= prev_length) {
                    int max_insert = strstart + lookahead - MIN_MATCH;
                    bflush = tallyDist(strstart - 1 - prev_match, prev_length - MIN_MATCH);
                    lookahead -= prev_length - 1;
                    prev_length -= 2;
                    do {
                        if (++strstart <= max_insert) {
                            hash_head = insertString(strstart);
                        }
                    } while (--prev_length != 0);
                    match_available = false;
                    match_length = MIN_MATCH - 1;
                    strstart++;
                    if (bflush) {
                        flushBlockOnly(false);
                        if (availOut == 0) return need_more;
                    }
                } else if (match_available) {
                    bflush = tallyLit(window[strstart - 1] & 0xff);
                    if (bflush) {
                        flushBlockOnly(false);
                    }
                    strstart++;
                    lookahead--;
                    if (availOut == 0) return need_more;
                } else {
                    match_available = true;
                    strstart++;
                    lookahead--;
                }
            }
            if (match_available) {
                tallyLit(window[strstart - 1] & 0xff);
                match_available = false;
            }
            insert = strstart < MIN_MATCH - 1 ? strstart : MIN_MATCH - 1;
            if (flush == Z_FINISH) {
                flushBlockOnly(true);
                if (availOut == 0) return finish_started;
                return finish_done;
            }
            if (sym_next != 0) {
                flushBlockOnly(false);
                if (availOut == 0) return need_more;
            }
            return block_done;
        }

        // ---------------------------------------------------------------- trees.c

        void put_short(int w) {
            put_byte(w & 0xff);
            put_byte((w & 0xffff) >>> 8);
        }

        static int bi_reverse(int code, int len) {
            int res = 0;
            do {
                res |= code & 1;
                code >>>= 1;
                res <<= 1;
            } while (--len > 0);
            return res >>> 1;
        }

        void bi_flush() {
            if (bi_valid == 16) {
                put_short(bi_buf);
                bi_buf = 0;
                bi_valid = 0;
            } else if (bi_valid >= 8) {
                put_byte(bi_buf);
                bi_buf = (bi_buf & 0xffff) >>> 8;
                bi_valid -= 8;
            }
        }

        void bi_windup() {
            if (bi_valid > 8) {
                put_short(bi_buf);
            } else if (bi_valid > 0) {
                put_byte(bi_buf);
            }
            bi_buf = 0;
            bi_valid = 0;
        }

        static void gen_codes(short[] code_, short[] len_, int max_code, short[] bl_count) {
            int[] next_code = new int[MAX_BITS + 1];
            int code = 0;
            int bits;
            int n;
            for (bits = 1; bits <= MAX_BITS; bits++) {
                code = (code + bl_count[bits - 1]) << 1;
                next_code[bits] = code & 0xffff;
            }
            for (n = 0; n <= max_code; n++) {
                int len = len_[n];
                if (len == 0) continue;
                code_[n] = (short) bi_reverse(next_code[len]++, len);
                next_code[len] &= 0xffff;
            }
        }

        void send_bits(int value, int length) {
            int len = length;
            if (bi_valid > Buf_size - len) {
                int val = value;
                bi_buf |= (val << bi_valid) & 0xffff;
                put_short(bi_buf);
                bi_buf = (val & 0xffff) >>> (Buf_size - bi_valid);
                bi_valid += len - Buf_size;
            } else {
                bi_buf |= (value << bi_valid) & 0xffff;
                bi_valid += len;
            }
        }

        void send_code(int c, short[] code, short[] len) {
            send_bits(code[c] & 0xffff, len[c] & 0xffff);
        }

        void init_block() {
            int n;
            for (n = 0; n < L_CODES; n++) dyn_ltree_fc[n] = 0;
            for (n = 0; n < D_CODES; n++) dyn_dtree_fc[n] = 0;
            for (n = 0; n < BL_CODES; n++) bl_tree_fc[n] = 0;
            dyn_ltree_fc[END_BLOCK] = 1;
            opt_len = static_len = 0;
            sym_next = matches = 0;
        }

        void _tr_init() {
            bi_buf = 0;
            bi_valid = 0;
            init_block();
        }

        static final int SMALLEST = 1;

        boolean smaller(short[] fc, int n, int m) {
            int fn = fc[n] & 0xffff, fm = fc[m] & 0xffff;
            return fn < fm || (fn == fm && (depth[n] & 0xff) <= (depth[m] & 0xff));
        }

        void pqdownheap(short[] fc, int k) {
            int v = heap[k];
            int j = k << 1;
            while (j <= heap_len) {
                if (j < heap_len && smaller(fc, heap[j + 1], heap[j])) {
                    j++;
                }
                if (smaller(fc, v, heap[j])) break;
                heap[k] = heap[j];
                k = j;
                j <<= 1;
            }
            heap[k] = v;
        }

        void gen_bitlen(TreeDesc desc) {
            short[] fc = desc.fc, dl = desc.dl;
            int max_code = desc.max_code;
            short[] stree = desc.stree_len;
            int[] extra = desc.extra_bits;
            int base = desc.extra_base;
            int max_length = desc.max_length;
            int h;
            int n, m;
            int bits;
            int xbits;
            int f;
            int overflow = 0;
            for (bits = 0; bits <= MAX_BITS; bits++) bl_count[bits] = 0;
            dl[heap[heap_max]] = 0;
            for (h = heap_max + 1; h < HEAP_SIZE; h++) {
                n = heap[h];
                bits = (dl[dl[n] & 0xffff] & 0xffff) + 1;
                if (bits > max_length) { bits = max_length; overflow++; }
                dl[n] = (short) bits;
                if (n > max_code) continue;
                bl_count[bits]++;
                xbits = 0;
                if (n >= base) xbits = extra[n - base];
                f = fc[n] & 0xffff;
                opt_len += (long) f * (bits + xbits);
                if (stree != null) static_len += (long) f * ((stree[n] & 0xffff) + xbits);
            }
            if (overflow == 0) return;
            do {
                bits = max_length - 1;
                while (bl_count[bits] == 0) bits--;
                bl_count[bits]--;
                bl_count[bits + 1] += 2;
                bl_count[max_length]--;
                overflow -= 2;
            } while (overflow > 0);
            for (bits = max_length; bits != 0; bits--) {
                n = bl_count[bits];
                while (n != 0) {
                    m = heap[--h];
                    if (m > max_code) continue;
                    if ((dl[m] & 0xffff) != bits) {
                        opt_len += ((long) bits - (dl[m] & 0xffff)) * (fc[m] & 0xffff);
                        dl[m] = (short) bits;
                    }
                    n--;
                }
            }
        }

        void build_tree(TreeDesc desc) {
            short[] fc = desc.fc, dl = desc.dl;
            short[] stree = desc.stree_len;
            int elems = desc.elems;
            int n, m;
            int max_code = -1;
            int node;
            heap_len = 0;
            heap_max = HEAP_SIZE;
            for (n = 0; n < elems; n++) {
                if (fc[n] != 0) {
                    heap[++heap_len] = max_code = n;
                    depth[n] = 0;
                } else {
                    dl[n] = 0;
                }
            }
            while (heap_len < 2) {
                node = heap[++heap_len] = (max_code < 2 ? ++max_code : 0);
                fc[node] = 1;
                depth[node] = 0;
                opt_len--;
                if (stree != null) static_len -= stree[node] & 0xffff;
            }
            desc.max_code = max_code;
            for (n = heap_len / 2; n >= 1; n--) pqdownheap(fc, n);
            node = elems;
            do {
                // pqremove
                n = heap[SMALLEST];
                heap[SMALLEST] = heap[heap_len--];
                pqdownheap(fc, SMALLEST);
                m = heap[SMALLEST];
                heap[--heap_max] = n;
                heap[--heap_max] = m;
                fc[node] = (short) ((fc[n] & 0xffff) + (fc[m] & 0xffff));
                depth[node] = (byte) (((depth[n] & 0xff) >= (depth[m] & 0xff) ?
                                       (depth[n] & 0xff) : (depth[m] & 0xff)) + 1);
                dl[n] = dl[m] = (short) node;
                heap[SMALLEST] = node++;
                pqdownheap(fc, SMALLEST);
            } while (heap_len >= 2);
            heap[--heap_max] = heap[SMALLEST];
            gen_bitlen(desc);
            // gen_codes: the codes go into fc (the union's code), the lengths are in dl
            gen_codes(fc, dl, max_code, bl_count);
        }

        void scan_tree(short[] dl, int max_code) {
            int n;
            int prevlen = -1;
            int curlen;
            int nextlen = dl[0] & 0xffff;
            int count = 0;
            int max_count = 7;
            int min_count = 4;
            if (nextlen == 0) { max_count = 138; min_count = 3; }
            dl[max_code + 1] = (short) 0xffff;
            for (n = 0; n <= max_code; n++) {
                curlen = nextlen;
                nextlen = dl[n + 1] & 0xffff;
                if (++count < max_count && curlen == nextlen) {
                    continue;
                } else if (count < min_count) {
                    bl_tree_fc[curlen] += (short) count;
                } else if (curlen != 0) {
                    if (curlen != prevlen) bl_tree_fc[curlen]++;
                    bl_tree_fc[REP_3_6]++;
                } else if (count <= 10) {
                    bl_tree_fc[REPZ_3_10]++;
                } else {
                    bl_tree_fc[REPZ_11_138]++;
                }
                count = 0;
                prevlen = curlen;
                if (nextlen == 0) {
                    max_count = 138; min_count = 3;
                } else if (curlen == nextlen) {
                    max_count = 6; min_count = 3;
                } else {
                    max_count = 7; min_count = 4;
                }
            }
        }

        void send_tree(short[] dl, int max_code) {
            int n;
            int prevlen = -1;
            int curlen;
            int nextlen = dl[0] & 0xffff;
            int count = 0;
            int max_count = 7;
            int min_count = 4;
            if (nextlen == 0) { max_count = 138; min_count = 3; }
            for (n = 0; n <= max_code; n++) {
                curlen = nextlen;
                nextlen = dl[n + 1] & 0xffff;
                if (++count < max_count && curlen == nextlen) {
                    continue;
                } else if (count < min_count) {
                    do { send_code(curlen, bl_tree_fc, bl_tree_dl); } while (--count != 0);
                } else if (curlen != 0) {
                    if (curlen != prevlen) {
                        send_code(curlen, bl_tree_fc, bl_tree_dl);
                        count--;
                    }
                    send_code(REP_3_6, bl_tree_fc, bl_tree_dl);
                    send_bits(count - 3, 2);
                } else if (count <= 10) {
                    send_code(REPZ_3_10, bl_tree_fc, bl_tree_dl);
                    send_bits(count - 3, 3);
                } else {
                    send_code(REPZ_11_138, bl_tree_fc, bl_tree_dl);
                    send_bits(count - 11, 7);
                }
                count = 0;
                prevlen = curlen;
                if (nextlen == 0) {
                    max_count = 138; min_count = 3;
                } else if (curlen == nextlen) {
                    max_count = 6; min_count = 3;
                } else {
                    max_count = 7; min_count = 4;
                }
            }
        }

        int build_bl_tree() {
            int max_blindex;
            scan_tree(dyn_ltree_dl, l_desc.max_code);
            scan_tree(dyn_dtree_dl, d_desc.max_code);
            build_tree(bl_desc);
            for (max_blindex = BL_CODES - 1; max_blindex >= 3; max_blindex--) {
                if (bl_tree_dl[bl_order[max_blindex]] != 0) break;
            }
            opt_len += 3 * ((long) max_blindex + 1) + 5 + 5 + 4;
            return max_blindex;
        }

        void send_all_trees(int lcodes, int dcodes, int blcodes) {
            int rank;
            send_bits(lcodes - 257, 5);
            send_bits(dcodes - 1, 5);
            send_bits(blcodes - 4, 4);
            for (rank = 0; rank < blcodes; rank++) {
                send_bits(bl_tree_dl[bl_order[rank]] & 0xffff, 3);
            }
            send_tree(dyn_ltree_dl, lcodes - 1);
            send_tree(dyn_dtree_dl, dcodes - 1);
        }

        void _tr_stored_block(byte[] buf, int off, int stored_len, boolean last) {
            send_bits((STORED_BLOCK << 1) + (last ? 1 : 0), 3);
            bi_windup();
            put_short(stored_len);
            put_short(~stored_len);
            if (stored_len != 0)
                System.arraycopy(buf, off, pending_buf, pending, stored_len);
            pending += stored_len;
        }

        void _tr_flush_bits() {
            bi_flush();
        }

        void _tr_align() {
            send_bits(STATIC_TREES << 1, 3);
            send_code(END_BLOCK, static_ltree_code, static_ltree_len);
            bi_flush();
        }

        void compress_block(short[] lcode, short[] llen, short[] dcode, short[] dlen) {
            int dist;
            int lc;
            int sx = 0;
            int code;
            int extra;
            if (sym_next != 0) do {
                dist = sym_buf[sx++] & 0xff;
                dist += (sym_buf[sx++] & 0xff) << 8;
                lc = sym_buf[sx++] & 0xff;
                if (dist == 0) {
                    send_code(lc, lcode, llen);
                } else {
                    code = _length_code[lc] & 0xff;
                    send_code(code + LITERALS + 1, lcode, llen);
                    extra = extra_lbits[code];
                    if (extra != 0) {
                        lc -= base_length[code];
                        send_bits(lc, extra);
                    }
                    dist--;
                    code = d_code(dist);
                    send_code(code, dcode, dlen);
                    extra = extra_dbits[code];
                    if (extra != 0) {
                        dist -= base_dist[code];
                        send_bits(dist, extra);
                    }
                }
            } while (sx < sym_next);
            send_code(END_BLOCK, lcode, llen);
        }

        void _tr_flush_block(byte[] buf, int off, int stored_len, boolean last) {
            long opt_lenb, static_lenb;
            int max_blindex = 0;
            build_tree(l_desc);
            build_tree(d_desc);
            max_blindex = build_bl_tree();
            opt_lenb = (opt_len + 3 + 7) >>> 3;
            static_lenb = (static_len + 3 + 7) >>> 3;
            if (static_lenb <= opt_lenb)
                opt_lenb = static_lenb;
            if (stored_len + 4 <= opt_lenb && buf != null) {
                _tr_stored_block(buf, off, stored_len, last);
            } else if (static_lenb == opt_lenb) {
                send_bits((STATIC_TREES << 1) + (last ? 1 : 0), 3);
                compress_block(static_ltree_code, static_ltree_len, static_dtree_code, static_dtree_len);
            } else {
                send_bits((DYN_TREES << 1) + (last ? 1 : 0), 3);
                send_all_trees(l_desc.max_code + 1, d_desc.max_code + 1, max_blindex + 1);
                compress_block(dyn_ltree_fc, dyn_ltree_dl, dyn_dtree_fc, dyn_dtree_dl);
            }
            init_block();
            if (last) {
                bi_windup();
            }
        }
    }
}
