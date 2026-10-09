/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Phase 2C"): jrt's java.io.OutputStreamWriter,
 * written for jrt in place of jdk26u's (whose sun.nio.cs.StreamEncoder needs java.nio's
 * buffers and encoders, cut). It encodes in jrt's three charsets (UTF-8, ISO-8859-1,
 * US-ASCII; C2G-SPEC §12) as StreamEncoder does with the encoder's REPLACE actions: a
 * malformed (lone) surrogate or an unmappable character becomes '?', a surrogate pair
 * counting as one character; a high surrogate at the end of a write waits for the next one,
 * and close writes it as '?'. Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.io;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Objects;

/** A bridge from characters to bytes, encoded with a charset. */
public class OutputStreamWriter extends Writer {

    private static final int UTF8 = 0;
    private static final int LATIN1 = 1;
    private static final int ASCII = 2;

    private final OutputStream out;
    private final Charset cs;
    private final int kind;
    private final byte[] bb = new byte[8192];
    private int count;
    /** A high surrogate waiting for the next write, or 0. */
    private char leftover;
    private boolean closed;

    /** A writer encoding with the named charset. */
    public OutputStreamWriter(OutputStream out, String charsetName)
        throws UnsupportedEncodingException
    {
        super(out);
        if (charsetName == null)
            throw new NullPointerException("charsetName");
        Charset c;
        try {
            c = Charset.forName(charsetName);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException x) {
            throw new UnsupportedEncodingException(charsetName);
        }
        this.out = out;
        this.cs = c;
        this.kind = kindOf(c);
    }

    /** A writer encoding with the default charset, or a PrintStream's. */
    public OutputStreamWriter(OutputStream out) {
        super(out);
        this.out = out;
        this.cs = out instanceof PrintStream ps ? ps.charset() : Charset.defaultCharset();
        this.kind = kindOf(cs);
    }

    /** A writer encoding with charset cs. */
    public OutputStreamWriter(OutputStream out, Charset cs) {
        super(out);
        if (cs == null)
            throw new NullPointerException("charset");
        this.out = out;
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

    /** The charset's historical name, or null when the writer is closed. */
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

    private void put(int b) throws IOException {
        if (count == bb.length)
            writeBytes();
        bb[count++] = (byte) b;
    }

    private void writeBytes() throws IOException {
        if (count > 0) {
            int n = count;
            count = 0;
            out.write(bb, 0, n);
        }
    }

    /** Encodes code point cp (a lone surrogate is malformed: '?'). */
    private void encode(int cp) throws IOException {
        if (Character.isSurrogate((char) cp) && cp <= 0xFFFF) {
            put('?');
        } else if (kind == LATIN1) {
            put(cp <= 0xFF ? cp : '?');
        } else if (kind == ASCII) {
            put(cp <= 0x7F ? cp : '?');
        } else if (cp < 0x80) {
            put(cp);
        } else if (cp < 0x800) {
            put(0xC0 | (cp >> 6));
            put(0x80 | (cp & 0x3F));
        } else if (cp < 0x10000) {
            put(0xE0 | (cp >> 12));
            put(0x80 | ((cp >> 6) & 0x3F));
            put(0x80 | (cp & 0x3F));
        } else {
            put(0xF0 | (cp >> 18));
            put(0x80 | ((cp >> 12) & 0x3F));
            put(0x80 | ((cp >> 6) & 0x3F));
            put(0x80 | (cp & 0x3F));
        }
    }

    /** Encodes one char, pairing surrogates across calls. */
    private void encodeChar(char c) throws IOException {
        if (leftover != 0) {
            char h = leftover;
            leftover = 0;
            if (Character.isLowSurrogate(c)) {
                encode(Character.toCodePoint(h, c));
                return;
            }
            put('?');
        }
        if (Character.isHighSurrogate(c))
            leftover = c;
        else
            encode(c);
    }

    @Override
    public void write(int c) throws IOException {
        synchronized (lock) {
            ensureOpen();
            encodeChar((char) c);
        }
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        synchronized (lock) {
            ensureOpen();
            Objects.checkFromIndexSize(off, len, cbuf.length);
            for (int i = off; i < off + len; i++)
                encodeChar(cbuf[i]);
        }
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        if (len < 0)
            throw new IndexOutOfBoundsException();
        synchronized (lock) {
            ensureOpen();
            Objects.checkFromIndexSize(off, len, str.length());
            for (int i = off; i < off + len; i++)
                encodeChar(str.charAt(i));
        }
    }

    @Override
    public Writer append(CharSequence csq, int start, int end) throws IOException {
        if (csq == null) csq = "null";
        return append(csq.subSequence(start, end));
    }

    @Override
    public Writer append(CharSequence csq) throws IOException {
        String s = String.valueOf(csq);
        write(s, 0, s.length());
        return this;
    }

    /** Writes the encoded bytes to the stream without flushing it (PrintStream's use). */
    void flushBuffer() throws IOException {
        synchronized (lock) {
            ensureOpen();
            writeBytes();
        }
    }

    @Override
    public void flush() throws IOException {
        synchronized (lock) {
            ensureOpen();
            writeBytes();
            out.flush();
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            if (closed)
                return;
            try {
                if (leftover != 0) {
                    leftover = 0;
                    put('?');
                }
                writeBytes();
                out.close();
            } finally {
                closed = true;
            }
        }
    }
}
