/*
 * Arbace (overlay/jdk, doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JC6b"): UTF-8 decoding
 * and encoding that report malformed input instead of replacing it, as jdk26u's String's
 * newStringOrThrow and getBytesUTF8OrThrow do through JavaLangAccess (jrt's JavaLangAccess has
 * neither): java.util.zip's UTF-8 coder of entry names and comments (the variant
 * overlay/jdk/variants/ZipCoder.ae), which throws IllegalArgumentException wrapping the
 * CharacterCodingException. Malformed input is what jdk26u's UTF-8 decoder reports (a byte that
 * starts no sequence, a missing or wrong continuation byte, an overlong form, a surrogate, a code
 * point past U+10FFFF); an unpaired surrogate is unmappable.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;

/** Strict UTF-8 for java.util.zip's names and comments. */
public final class Utf8Coder {

    private Utf8Coder() {
    }

    private static boolean cont(byte b) {
        return (b & 0xc0) == 0x80;
    }

    /** The string of the UTF-8 bytes b[off, off + len), or a MalformedInputException. */
    public static String decodeOrThrow(byte[] b, int off, int len) throws CharacterCodingException {
        char[] out = new char[len];
        int n = 0;
        int i = off, end = off + len;
        while (i < end) {
            int c = b[i] & 0xff;
            if (c < 0x80) {
                out[n++] = (char) c;
                i++;
            } else if (c >= 0xc2 && c <= 0xdf) {
                if (i + 1 >= end || !cont(b[i + 1]))
                    throw new MalformedInputException(1);
                out[n++] = (char) (((c & 0x1f) << 6) | (b[i + 1] & 0x3f));
                i += 2;
            } else if (c >= 0xe0 && c <= 0xef) {
                if (i + 1 >= end)
                    throw new MalformedInputException(1);
                int c1 = b[i + 1] & 0xff;
                boolean ok = c == 0xe0 ? c1 >= 0xa0 && c1 <= 0xbf
                           : c == 0xed ? c1 >= 0x80 && c1 <= 0x9f
                           : cont(b[i + 1]);
                if (!ok)
                    throw new MalformedInputException(1);
                if (i + 2 >= end || !cont(b[i + 2]))
                    throw new MalformedInputException(2);
                out[n++] = (char) (((c & 0x0f) << 12) | ((c1 & 0x3f) << 6) | (b[i + 2] & 0x3f));
                i += 3;
            } else if (c >= 0xf0 && c <= 0xf4) {
                if (i + 1 >= end)
                    throw new MalformedInputException(1);
                int c1 = b[i + 1] & 0xff;
                boolean ok = c == 0xf0 ? c1 >= 0x90 && c1 <= 0xbf
                           : c == 0xf4 ? c1 >= 0x80 && c1 <= 0x8f
                           : cont(b[i + 1]);
                if (!ok)
                    throw new MalformedInputException(1);
                if (i + 2 >= end || !cont(b[i + 2]))
                    throw new MalformedInputException(2);
                if (i + 3 >= end || !cont(b[i + 3]))
                    throw new MalformedInputException(3);
                int cp = ((c & 0x07) << 18) | ((c1 & 0x3f) << 12) | ((b[i + 2] & 0x3f) << 6) | (b[i + 3] & 0x3f);
                out[n++] = Character.highSurrogate(cp);
                out[n++] = Character.lowSurrogate(cp);
                i += 4;
            } else {
                throw new MalformedInputException(1);
            }
        }
        return new String(out, 0, n);
    }

    /** The UTF-8 bytes of s, or an UnmappableCharacterException for an unpaired surrogate. */
    public static byte[] encodeOrThrow(String s) throws CharacterCodingException {
        int len = s.length();
        byte[] out = new byte[len * 3];
        int n = 0;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c < 0x80) {
                out[n++] = (byte) c;
            } else if (c < 0x800) {
                out[n++] = (byte) (0xc0 | (c >> 6));
                out[n++] = (byte) (0x80 | (c & 0x3f));
            } else if (Character.isSurrogate(c)) {
                if (!Character.isHighSurrogate(c) || i + 1 >= len || !Character.isLowSurrogate(s.charAt(i + 1)))
                    throw new UnmappableCharacterException(1);
                int cp = Character.toCodePoint(c, s.charAt(++i));
                out[n++] = (byte) (0xf0 | (cp >> 18));
                out[n++] = (byte) (0x80 | ((cp >> 12) & 0x3f));
                out[n++] = (byte) (0x80 | ((cp >> 6) & 0x3f));
                out[n++] = (byte) (0x80 | (cp & 0x3f));
            } else {
                out[n++] = (byte) (0xe0 | (c >> 12));
                out[n++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                out[n++] = (byte) (0x80 | (c & 0x3f));
            }
        }
        return java.util.Arrays.copyOf(out, n);
    }

    /** ZipCoder's toString for UTF-8: IllegalArgumentException on malformed input. */
    public static String decode(byte[] b, int off, int len) {
        try {
            return decodeOrThrow(b, off, len);
        } catch (CharacterCodingException cce) {
            throw new IllegalArgumentException(cce);
        }
    }

    /** ZipCoder's getBytes for UTF-8: IllegalArgumentException on an unpaired surrogate. */
    public static byte[] encode(String s) {
        try {
            return encodeOrThrow(s);
        } catch (CharacterCodingException cce) {
            throw new IllegalArgumentException(cce);
        }
    }

    /**
     * ZipCoder.UTF8ZipCoder's compare: 0 (EXACT_MATCH) when b[off, off + len) is str's UTF-8, 1
     * (DIRECTORY_MATCH) when it is with a '/' after it and matchDirectory, else 2 (NO_MATCH).
     */
    public static byte compare(String str, byte[] b, int off, int len, boolean matchDirectory) {
        byte[] encoded;
        try {
            encoded = encodeOrThrow(str);
        } catch (CharacterCodingException e) {
            return 2;
        }
        int mismatch = java.util.Arrays.mismatch(encoded, 0, encoded.length, b, off, off + len);
        if (mismatch == -1) {
            return 0;
        } else if (matchDirectory && len == mismatch + 1 && len > 0 && b[off + len - 1] == '/') {
            return 1;
        } else {
            return 2;
        }
    }
}
