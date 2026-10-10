/*
 * Arbace (overlay/jdk, doc/go/EVAL-NOTES.md "The suite's Java fixtures"): the glob patterns of
 * jrt's Files.newDirectoryStream (overlay/jdk/java.base/java/nio/file/Files.java), as jdk26u's
 * UnixFileSystem.getPathMatcher("glob:...") makes them, through sun.nio.fs.Globs (translated),
 * whose method is package-private.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.nio.fs;

/** Globs' Unix regular expressions, for java.nio.file. */
public final class GlobPatterns {

    private GlobPatterns() {
    }

    /** The regular expression of glob pattern glob on Unix (Globs.toUnixRegexPattern). */
    public static String toUnixRegex(String glob) {
        return Globs.toUnixRegexPattern(glob);
    }
}
