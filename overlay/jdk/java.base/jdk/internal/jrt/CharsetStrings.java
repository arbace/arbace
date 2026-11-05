/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F8, "As built: F8 (write-ahead)"): String's
 * Charset overloads for the charsets jrt's hand-written String does not code itself. jrt's String
 * (go/arbace/jrt, hand-written Go) codes UTF-8, ISO-8859-1, US-ASCII and UTF-16 (BE, LE) by name,
 * as before F8; any other charset, now that java.nio.charset and sun.nio.cs are jdk26u's, is coded
 * here, as jdk26u's String.decode(Charset, byte[], int, int), encodeWithEncoder and lookupCharset
 * code it (baf63fb): the charset's ArrayDecoder or ArrayEncoder when it has one, else its decoder
 * or encoder over heap buffers, malformed and unmappable input replaced. The String's coder is
 * Latin1 when every char is at most U+00FF (jdk26u's COMPACT_STRINGS), else UTF-16, whose bytes
 * are little-endian as jrt's StringUTF16 writes them (overlay/jdk/variants/CsStringUTF16.ae reads
 * them so). Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.Objects;
import sun.nio.cs.ArrayDecoder;
import sun.nio.cs.ArrayEncoder;

/** String's coding with jdk26u's charsets. */
public final class CharsetStrings {
    private CharsetStrings() {
    }

    /** String.lookupCharset: the charset named csn, or UnsupportedEncodingException. */
    public static Charset lookup(String csn) throws UnsupportedEncodingException {
        Objects.requireNonNull(csn);
        try {
            return Charset.forName(csn);
        } catch (UnsupportedCharsetException | IllegalCharsetNameException x) {
            throw new UnsupportedEncodingException(csn);
        }
    }

    private static int scale(int len, float expansionFactor) {
        return (int) (len * (double) expansionFactor);
    }

    /** new String(bytes, offset, length, cs) for a charset jrt's String does not code itself. */
    public static String decode(byte[] bytes, int offset, int length, Charset cs) {
        CharsetDecoder cd = cs.newDecoder();
        int en = scale(length, cd.maxCharsPerByte());
        cd.onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        char[] ca = new char[en];
        if (cd instanceof ArrayDecoder ad) {
            int clen = ad.decode(bytes, offset, length, ca);
            return new String(ca, 0, clen);
        }
        ByteBuffer bb = ByteBuffer.wrap(bytes, offset, length);
        CharBuffer cb = CharBuffer.wrap(ca, 0, ca.length);
        try {
            CoderResult cr = cd.decode(bb, cb, true);
            if (!cr.isUnderflow())
                cr.throwException();
            cr = cd.flush(cb);
            if (!cr.isUnderflow())
                cr.throwException();
        } catch (CharacterCodingException x) {
            // substitution is enabled, so this does not happen
            throw new Error(x);
        }
        return new String(ca, 0, cb.position());
    }

    /** s.getBytes(cs) for a charset jrt's String does not code itself. */
    public static byte[] encode(String s, Charset cs) {
        CharsetEncoder ce = cs.newEncoder();
        int len = s.length();
        int en = scale(len, ce.maxBytesPerChar());
        boolean latin1 = true;
        for (int i = 0; i < len && latin1; i++)
            latin1 = s.charAt(i) <= 0xFF;
        if (ce instanceof ArrayEncoder ae) {
            if (latin1) {
                byte[] val = new byte[len];
                boolean ascii = true;
                for (int i = 0; i < len; i++) {
                    val[i] = (byte) s.charAt(i);
                    ascii &= val[i] >= 0;
                }
                if (ae.isASCIICompatible() && ascii)
                    return val;
                byte[] ba = new byte[en];
                if (len == 0)
                    return ba;
                return trim(ba, ae.encodeFromLatin1(val, 0, len, ba, 0));
            }
            byte[] val = new byte[len << 1];
            for (int i = 0; i < len; i++) {
                char c = s.charAt(i);
                val[2 * i] = (byte) c;
                val[2 * i + 1] = (byte) (c >> 8);
            }
            byte[] ba = new byte[en];
            return trim(ba, ae.encodeFromUTF16(val, 0, len, ba, 0));
        }
        byte[] ba = new byte[en];
        if (len == 0)
            return ba;
        ce.onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        ByteBuffer bb = ByteBuffer.wrap(ba);
        CharBuffer cb = CharBuffer.wrap(s.toCharArray(), 0, len);
        try {
            CoderResult cr = ce.encode(cb, bb, true);
            if (!cr.isUnderflow())
                cr.throwException();
            cr = ce.flush(bb);
            if (!cr.isUnderflow())
                cr.throwException();
        } catch (CharacterCodingException x) {
            throw new Error(x);
        }
        return trim(ba, bb.position());
    }

    private static byte[] trim(byte[] ba, int len) {
        return len == ba.length ? ba : Arrays.copyOf(ba, len);
    }
}
