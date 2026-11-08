/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F9 (write-ahead)"): what jdk26u's
 * java.nio.file.Files and sun.nio.fs.UnixPath ask of JavaLangAccess (uncheckedNewStringOrThrow,
 * uncheckedGetBytesOrThrow: String's coding that throws CharacterCodingException on malformed or unmappable input),
 * which jrt's hand-written JavaLangAccess has not: the same through the charset's decoder and encoder with
 * CodingErrorAction.REPORT (MalformedInputException and UnmappableCharacterException with their lengths).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;

/** String coding that reports malformed input. */
public final class NioStrings {

    private NioStrings() {
    }

    /** The string of bytes in cs; CharacterCodingException on malformed or unmappable input. */
    public static String newStringOrThrow(byte[] bytes, Charset cs) throws CharacterCodingException {
        return cs.newDecoder()
                 .onMalformedInput(CodingErrorAction.REPORT)
                 .onUnmappableCharacter(CodingErrorAction.REPORT)
                 .decode(ByteBuffer.wrap(bytes))
                 .toString();
    }

    /** The bytes of s in cs; CharacterCodingException on malformed or unmappable input. */
    public static byte[] getBytesOrThrow(String s, Charset cs) throws CharacterCodingException {
        ByteBuffer b = cs.newEncoder()
                         .onMalformedInput(CodingErrorAction.REPORT)
                         .onUnmappableCharacter(CodingErrorAction.REPORT)
                         .encode(CharBuffer.wrap(s));
        byte[] r = new byte[b.remaining()];
        b.get(r);
        return r;
    }
}
