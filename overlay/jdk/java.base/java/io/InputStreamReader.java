/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Phase 2C"): jrt's java.io.InputStreamReader,
 * written for jrt in place of jdk26u's (whose sun.nio.cs.StreamDecoder needs java.nio's
 * buffers and decoders, cut). It decodes jrt's three charsets (UTF-8, ISO-8859-1, US-ASCII;
 * C2G-SPEC §12) as StreamDecoder does with the decoder's REPLACE actions: UTF-8's malformed
 * sequences become U+FFFD with the lengths of jdk26u's sun.nio.cs.UTF_8.Decoder, bytes
 * outside US-ASCII become U+FFFD, and an incomplete sequence at the end of the input becomes
 * one U+FFFD. Like StreamDecoder, a read blocks on the stream only while it has decoded
 * nothing. Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.io;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Objects;

/** A bridge from bytes to characters, decoded with a charset. */
public class InputStreamReader extends Reader {

    private static final int UTF8 = 0;
    private static final int LATIN1 = 1;
    private static final int ASCII = 2;
    private static final char REPLACEMENT = '�';

    private final InputStream in;
    private final Charset cs;
    private final int kind;
    private final byte[] bb = new byte[8192];
    private int bpos;
    private int blim;
    private boolean eof;
    /** The low surrogate of a decoded pair whose high half was returned, or 0. */
    private char pending;
    private boolean closed;

    /** A reader decoding with the default charset. */
    public InputStreamReader(InputStream in) {
        super(in);
        this.in = in;
        this.cs = Charset.defaultCharset();
        this.kind = kindOf(cs);
    }

    /** A reader decoding with the named charset. */
    public InputStreamReader(InputStream in, String charsetName)
        throws UnsupportedEncodingException
    {
        super(in);
        if (charsetName == null)
            throw new NullPointerException("charsetName");
        Charset c;
        try {
            c = Charset.forName(charsetName);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException x) {
            throw new UnsupportedEncodingException(charsetName);
        }
        this.in = in;
        this.cs = c;
        this.kind = kindOf(c);
    }

    /** A reader decoding with charset cs. */
    public InputStreamReader(InputStream in, Charset cs) {
        super(in);
        if (cs == null)
            throw new NullPointerException("charset");
        this.in = in;
        this.cs = cs;
        this.kind = kindOf(cs);
    }

    private static int kindOf(Charset cs) {
        String n = cs.name();
        if (n.equals("ISO-8859-1"))
            return LATIN1;
        if (n.equals("US-ASCII"))
            return ASCII;
        return UTF8;
    }

    /** The charset's historical name, or null when the reader is closed. */
    public String getEncoding() {
        synchronized (lock) {
            if (closed)
                return null;
            switch (kind) {
                case LATIN1: return "ISO8859_1";
                case ASCII: return "ASCII";
                default: return "UTF8";
            }
        }
    }

    private void ensureOpen() throws IOException {
        if (closed)
            throw new IOException("Stream closed");
    }

    @Override
    public int read() throws IOException {
        char[] one = new char[1];
        int n = read(one, 0, 1);
        return n < 0 ? -1 : one[0];
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        synchronized (lock) {
            ensureOpen();
            Objects.checkFromIndexSize(off, len, cbuf.length);
            if (len == 0)
                return 0;
            int n = 0;
            if (pending != 0) {
                cbuf[off + n++] = pending;
                pending = 0;
            }
            for (;;) {
                n = decode(cbuf, off, len, n);
                if (n > 0 && (n == len || !inReady()))
                    return n;
                if (eof) {
                    if (bpos < blim) {
                        // an incomplete sequence at the end: one replacement
                        bpos = blim;
                        cbuf[off + n++] = REPLACEMENT;
                        continue;
                    }
                    return n == 0 ? -1 : n;
                }
                if (n == len)
                    return n;
                fill();
            }
        }
    }

    private boolean inReady() {
        try {
            return in.available() > 0;
        } catch (IOException x) {
            return false;
        }
    }

    /** Reads more bytes, keeping the undecoded ones. */
    private void fill() throws IOException {
        if (bpos > 0) {
            System.arraycopy(bb, bpos, bb, 0, blim - bpos);
            blim -= bpos;
            bpos = 0;
        }
        int r = in.read(bb, blim, bb.length - blim);
        if (r < 0)
            eof = true;
        else
            blim += r;
    }

    private static boolean notCont(int b) {
        return (b & 0xc0) != 0x80;
    }

    /**
     * Decodes from the buffer into cbuf[off + n ...] while there is room and the bytes are
     * complete; returns the new n.
     */
    private int decode(char[] cbuf, int off, int len, int n) {
        while (bpos < blim && n < len) {
            int b1 = bb[bpos];
            if (kind != UTF8) {
                cbuf[off + n++] = (kind == ASCII && b1 < 0) ? REPLACEMENT : (char) (b1 & 0xff);
                bpos++;
                continue;
            }
            int rem = blim - bpos;
            if (b1 >= 0) {
                cbuf[off + n++] = (char) b1;
                bpos++;
            } else if ((b1 >> 5) == -2 && (b1 & 0x1e) != 0) {
                if (rem < 2)
                    return n;
                int b2 = bb[bpos + 1];
                if (notCont(b2)) {
                    malformed(cbuf, off, n++, 1);
                    continue;
                }
                cbuf[off + n++] = (char) (((b1 & 0x1f) << 6) | (b2 & 0x3f));
                bpos += 2;
            } else if ((b1 >> 4) == -2) {
                if (rem < 3) {
                    if (rem > 1 && ((b1 == (byte) 0xe0 && (bb[bpos + 1] & 0xe0) == 0x80) || notCont(bb[bpos + 1]))) {
                        malformed(cbuf, off, n++, 1);
                        continue;
                    }
                    return n;
                }
                int b2 = bb[bpos + 1];
                int b3 = bb[bpos + 2];
                if ((b1 == (byte) 0xe0 && (b2 & 0xe0) == 0x80) || notCont(b2) || notCont(b3)) {
                    malformed(cbuf, off, n++,
                              ((b1 == (byte) 0xe0 && (b2 & 0xe0) == 0x80) || notCont(b2)) ? 1 : 2);
                    continue;
                }
                char c = (char) (((b1 & 0x0f) << 12) | ((b2 & 0x3f) << 6) | (b3 & 0x3f));
                if (Character.isSurrogate(c)) {
                    malformed(cbuf, off, n++, 3);
                    continue;
                }
                cbuf[off + n++] = c;
                bpos += 3;
            } else if ((b1 >> 3) == -2) {
                int u1 = b1 & 0xff;
                if (rem < 4) {
                    if (u1 > 0xf4 || rem > 1 && malformed4First(u1, bb[bpos + 1] & 0xff)) {
                        malformed(cbuf, off, n++, 1);
                        continue;
                    }
                    if (rem > 2 && notCont(bb[bpos + 2])) {
                        malformed(cbuf, off, n++, 2);
                        continue;
                    }
                    return n;
                }
                int b2 = bb[bpos + 1];
                int b3 = bb[bpos + 2];
                int b4 = bb[bpos + 3];
                int uc = ((b1 & 0x07) << 18) | ((b2 & 0x3f) << 12) | ((b3 & 0x3f) << 6) | (b4 & 0x3f);
                if (notCont(b2) || notCont(b3) || notCont(b4) || !Character.isSupplementaryCodePoint(uc)) {
                    int u2 = b2 & 0xff;
                    malformed(cbuf, off, n++,
                              (u1 > 0xf4 || malformed4First(u1, u2)) ? 1 : notCont(b3) ? 2 : 3);
                    continue;
                }
                cbuf[off + n++] = Character.highSurrogate(uc);
                bpos += 4;
                if (n < len)
                    cbuf[off + n++] = Character.lowSurrogate(uc);
                else
                    pending = Character.lowSurrogate(uc);
            } else {
                malformed(cbuf, off, n++, 1);
            }
        }
        return n;
    }

    private static boolean malformed4First(int u1, int u2) {
        return (u1 == 0xf0 && (u2 < 0x90 || u2 > 0xbf)) ||
               (u1 == 0xf4 && (u2 & 0xf0) != 0x80) ||
               (u2 & 0xc0) != 0x80;
    }

    private void malformed(char[] cbuf, int off, int n, int length) {
        cbuf[off + n] = REPLACEMENT;
        bpos += length;
    }

    @Override
    public boolean ready() throws IOException {
        synchronized (lock) {
            ensureOpen();
            return pending != 0 || bpos < blim || inReady();
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            if (closed)
                return;
            try {
                in.close();
            } finally {
                closed = true;
            }
        }
    }
}
