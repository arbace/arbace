/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Phase 2C"): the standard streams of jrt's System
 * (System.in, out, err), made as jdk26u's System.initPhase1 makes them: a BufferedInputStream
 * over the standard input's FileInputStream, and PrintStreams with autoflush over a
 * 128-byte BufferedOutputStream of the standard output's and error's FileOutputStream, in the
 * stdout.encoding and stderr.encoding properties' charsets. jrt's System is hand-written Go;
 * c2g's support code (System_out ...) calls these when it starts.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import sun.nio.cs.UTF_8;

/** The initial System.in, System.out and System.err. */
public final class StandardStreams {

    private StandardStreams() {
    }

    /** System.in. */
    public static InputStream in() {
        return new BufferedInputStream(new FileInputStream(FileDescriptor.in));
    }

    /** System.out. */
    public static PrintStream out() {
        return newPrintStream(new FileOutputStream(FileDescriptor.out), System.getProperty("stdout.encoding"));
    }

    /** System.err. */
    public static PrintStream err() {
        return newPrintStream(new FileOutputStream(FileDescriptor.err), System.getProperty("stderr.encoding"));
    }

    private static PrintStream newPrintStream(FileOutputStream out, String enc) {
        if (enc != null) {
            return new PrintStream(new BufferedOutputStream(out, 128), true,
                                   Charset.forName(enc, UTF_8.INSTANCE));
        }
        return new PrintStream(new BufferedOutputStream(out, 128), true);
    }
}
