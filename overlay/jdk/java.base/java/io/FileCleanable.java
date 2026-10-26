/*
 * Arbace (overlay/jdk, doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JC6b"): jrt's
 * java.io.FileCleanable, written for jrt in place of jdk26u's, a PhantomCleanable that closes a
 * file descriptor when its stream becomes unreachable (the Cleaner and phantom references are not
 * in the Go build's world). Here registering does nothing: a file of jrt's table is closed when its
 * stream is closed, as jrt's FileInputStream's and FileOutputStream's are (JRT-NOTES.md "Phase
 * 2C"). Its two methods are those jdk26u's RandomAccessFile calls.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.io;

/** The cleanup of an unreachable stream's descriptor, which the Go build does not do. */
final class FileCleanable {

    private FileCleanable() {
    }

    /** Nothing to do: the descriptor is closed when its stream is. */
    static void register(FileDescriptor fdo) {
    }

    /** Nothing to do. */
    static void unregister(FileDescriptor fdo) {
    }
}
