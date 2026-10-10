/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "The JDK's resource data"): jrt's
 * java.util.zip.InflaterInputStream, written for jrt in place of jdk26u's, whose Inflater is the
 * VM's zlib (natives over native memory, a Cleaner, java.nio's direct buffers; cut). It reads
 * a zlib stream (RFC 1950) as jdk26u's does with its default Inflater, but inflates the whole
 * input at its first read, through the host (Go's compress/zlib; a native): enough for the
 * JDK's resources (java.lang.CharacterName's uniName.dat). A truncated stream throws
 * EOFException("Unexpected end of ZLIB input stream") and malformed data ZipException, as
 * jdk26u's. Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.util.zip;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/** An input stream uncompressing data in the zlib format. */
public class InflaterInputStream extends FilterInputStream {

    private static final String UNEXPECTED_EOF = "Unexpected end of ZLIB input stream";

    /** The uncompressed data, once inflated (at the first read). */
    private ByteArrayInputStream inflated;

    private boolean closed;

    /** A stream uncompressing the zlib data of in. */
    public InflaterInputStream(InputStream in) {
        super(in);
        if (in == null) {
            throw new NullPointerException();
        }
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
    }

    private ByteArrayInputStream inflated() throws IOException {
        if (inflated == null) {
            String[] error = new String[1];
            byte[] data = inflate0(in.readAllBytes(), error);
            if (data == null) {
                if (UNEXPECTED_EOF.equals(error[0])) {
                    throw new EOFException(UNEXPECTED_EOF);
                }
                throw new ZipException(error[0]);
            }
            inflated = new ByteArrayInputStream(data);
        }
        return inflated;
    }

    @Override
    public int read() throws IOException {
        ensureOpen();
        return inflated().read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            return 0;
        }
        return inflated().read(b, off, len);
    }

    /** 0 after the end of the data, else 1 (as jdk26u's). */
    @Override
    public int available() throws IOException {
        ensureOpen();
        return inflated().available() > 0 ? 1 : 0;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n < 0) {
            throw new IllegalArgumentException("negative skip length");
        }
        ensureOpen();
        return inflated().skip(n);
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            in.close();
        }
    }

    /** False: marks are not supported (as jdk26u's). */
    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public synchronized void mark(int readlimit) {
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    /** The data of the zlib stream input, or null with the reason in error[0]. */
    private static native byte[] inflate0(byte[] input, String[] error);
}
