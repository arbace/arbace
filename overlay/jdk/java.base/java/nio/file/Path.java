/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Files"): jrt's java.nio.file.Path, written for jrt in
 * place of jdk26u's, whose paths belong to a FileSystem of the file system providers
 * (sun.nio.fs, the VM's natives), outside the Go build. This Path is the documented interface
 * less what needs a FileSystem (getFileSystem, register with a WatchService): the default file
 * system's paths only, implemented by jdk.internal.jrt.HostPath as sun.nio.fs.UnixPath behaves.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.nio.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.NoSuchElementException;
import jdk.internal.jrt.HostPath;

/** A path of the default (the host's) file system. */
public interface Path extends Comparable<Path>, Iterable<Path> {

    /** The path of the joined strings. */
    public static Path of(String first, String... more) {
        return HostPath.of(first, more);
    }

    /** The path of a file: URI. */
    public static Path of(URI uri) {
        return HostPath.of(uri);
    }

    boolean isAbsolute();

    Path getRoot();

    Path getFileName();

    Path getParent();

    int getNameCount();

    Path getName(int index);

    Path subpath(int beginIndex, int endIndex);

    boolean startsWith(Path other);

    default boolean startsWith(String other) {
        return startsWith(Path.of(other));
    }

    boolean endsWith(Path other);

    default boolean endsWith(String other) {
        return endsWith(Path.of(other));
    }

    Path normalize();

    Path resolve(Path other);

    default Path resolve(String other) {
        return resolve(Path.of(other));
    }

    default Path resolve(Path first, Path... more) {
        Path result = resolve(first);
        for (Path p : more) {
            result = result.resolve(p);
        }
        return result;
    }

    default Path resolve(String first, String... more) {
        Path result = resolve(first);
        for (String s : more) {
            result = result.resolve(s);
        }
        return result;
    }

    default Path resolveSibling(Path other) {
        if (other == null)
            throw new NullPointerException();
        Path parent = getParent();
        return (parent == null) ? other : parent.resolve(other);
    }

    default Path resolveSibling(String other) {
        return resolveSibling(Path.of(other));
    }

    Path relativize(Path other);

    URI toUri();

    Path toAbsolutePath();

    Path toRealPath(LinkOption... options) throws IOException;

    default File toFile() {
        return new File(toString());
    }

    @Override
    default Iterator<Path> iterator() {
        return new Iterator<>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return (i < getNameCount());
            }

            @Override
            public Path next() {
                if (i < getNameCount()) {
                    Path result = getName(i);
                    i++;
                    return result;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }

    @Override
    int compareTo(Path other);

    boolean equals(Object other);

    int hashCode();

    String toString();
}
