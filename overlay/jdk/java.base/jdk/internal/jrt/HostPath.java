/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Files"): jrt's java.nio.file.Path of the host's file
 * system (overlay/jdk/java.base/java/nio/file/Path.java), the Go build's sun.nio.fs.UnixPath. Its
 * path operations transcribe UnixPath's, from https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.base/unix/classes/sun/nio/fs/UnixPath.java (normalizeAndCheck, normalize(String, int,
 * int), the name offsets, getFileName, getParent, getName, subpath, resolve, relativize,
 * normalize, startsWith, endsWith), over the path's chars where UnixPath works on its bytes in
 * the platform encoding (UTF-8: the names, separated by '/', are the same; compareTo and
 * hashCode use the UTF-8 bytes, as UnixPath's): Copyright (c) 2008, 2025, Oracle and/or its
 * affiliates, GNU General Public License version 2 with the Classpath Exception (LICENSE.md).
 * The rest (toUri, toAbsolutePath, toRealPath over java.io.File) is Arbace's own, Eclipse Public
 * License 1.0.
 */
package jdk.internal.jrt;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.util.Arrays;
import java.util.Objects;

/** A path of the host's file system. */
public final class HostPath implements Path {

    private final String path;

    private volatile int[] offsets;

    private int hash;

    private HostPath(String normalized) {
        this.path = normalized;
    }

    /** The path of the joined strings (Path.of). */
    public static Path of(String first, String... more) {
        String path;
        if (more.length == 0) {
            path = first;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(first);
            for (String segment : more) {
                if (!segment.isEmpty()) {
                    if (sb.length() > 0)
                        sb.append('/');
                    sb.append(segment);
                }
            }
            path = sb.toString();
        }
        return new HostPath(normalizeAndCheck(path));
    }

    /** The path of a java.io.File's path string (File.toPath). */
    public static Path ofFile(String path) {
        return new HostPath(normalizeAndCheck(path));
    }

    /** The path of a file: URI (Path.of(URI)). */
    public static Path of(URI uri) {
        if (!uri.isAbsolute())
            throw new IllegalArgumentException("URI is not absolute");
        String scheme = uri.getScheme();
        if (!scheme.equalsIgnoreCase("file"))
            throw new IllegalArgumentException("URI scheme is not \"file\"");
        if (uri.isOpaque())
            throw new IllegalArgumentException("URI is not hierarchical");
        if (uri.getRawAuthority() != null)
            throw new IllegalArgumentException("URI has an authority component");
        if (uri.getRawFragment() != null)
            throw new IllegalArgumentException("URI has a fragment component");
        if (uri.getRawQuery() != null)
            throw new IllegalArgumentException("URI has a query component");
        String p = uri.getPath();
        if (p.isEmpty())
            throw new IllegalArgumentException("URI path component is empty");
        if (p.endsWith("/") && (p.length() > 1))
            p = p.substring(0, p.length() - 1);
        return new HostPath(normalizeAndCheck(p));
    }

    static String normalizeAndCheck(String input) {
        int n = input.length();
        char prevChar = 0;
        for (int i = 0; i < n; i++) {
            char c = input.charAt(i);
            if ((c == '/') && (prevChar == '/'))
                return normalize(input, n, i - 1);
            checkNotNul(input, c);
            prevChar = c;
        }
        if (prevChar == '/' && n > 1) {
            return input.substring(0, n - 1);
        }
        return input;
    }

    private static void checkNotNul(String input, char c) {
        if (c == '\u0000')
            throw new InvalidPathException(input, "Nul character not allowed");
    }

    private static String normalize(String input, int len, int off) {
        if (len == 0)
            return input;
        int n = len;
        while ((n > 0) && (input.charAt(n - 1) == '/')) n--;
        if (n == 0)
            return "/";
        StringBuilder sb = new StringBuilder(input.length());
        if (off > 0)
            sb.append(input, 0, off);
        char prevChar = 0;
        for (int i = off; i < n; i++) {
            char c = input.charAt(i);
            if ((c == '/') && (prevChar == '/'))
                continue;
            checkNotNul(input, c);
            sb.append(c);
            prevChar = c;
        }
        return sb.toString();
    }

    private static HostPath toHostPath(Path obj) {
        if (obj == null)
            throw new NullPointerException();
        if (!(obj instanceof HostPath))
            throw new ProviderMismatchException();
        return (HostPath) obj;
    }

    private int[] offsets() {
        int[] result = offsets;
        if (result == null) {
            int count = 0, index = 0, n = path.length();
            if (n == 0) {
                // empty path has one name
                count = 1;
            } else {
                while (index < n) {
                    char c = path.charAt(index++);
                    if (c != '/') {
                        count++;
                        while (index < n && path.charAt(index) != '/')
                            index++;
                    }
                }
            }
            result = new int[count];
            count = 0;
            index = 0;
            while (index < n) {
                char c = path.charAt(index);
                if (c == '/') {
                    index++;
                } else {
                    result[count++] = index++;
                    while (index < n && path.charAt(index) != '/')
                        index++;
                }
            }
            offsets = result;
        }
        return result;
    }

    private boolean isEmpty() {
        return path.isEmpty();
    }

    private static HostPath emptyPath() {
        return new HostPath("");
    }

    private boolean hasDotOrDotDot() {
        int n = getNameCount();
        for (int i = 0; i < n; i++) {
            String name = ((HostPath) getName(i)).path;
            if (name.equals(".") || name.equals(".."))
                return true;
        }
        return false;
    }

    @Override
    public boolean isAbsolute() {
        return path.startsWith("/");
    }

    @Override
    public Path getRoot() {
        return isAbsolute() ? new HostPath("/") : null;
    }

    @Override
    public Path getFileName() {
        int[] offs = offsets();
        int count = offs.length;
        // no elements so no name
        if (count == 0)
            return null;
        // one name element and no root component
        if (count == 1 && !path.isEmpty() && path.charAt(0) != '/')
            return this;
        return new HostPath(path.substring(offs[count - 1]));
    }

    @Override
    public Path getParent() {
        int[] offs = offsets();
        int count = offs.length;
        if (count == 0) {
            // no elements so no parent
            return null;
        }
        int len = offs[count - 1] - 1;
        if (len <= 0) {
            // parent is root only (may be null)
            return getRoot();
        }
        return new HostPath(path.substring(0, len));
    }

    @Override
    public int getNameCount() {
        return offsets().length;
    }

    @Override
    public Path getName(int index) {
        int[] offs = offsets();
        if (index < 0 || index >= offs.length)
            throw new IllegalArgumentException();
        int begin = offs[index];
        int end = (index == offs.length - 1) ? path.length() : offs[index + 1] - 1;
        return new HostPath(path.substring(begin, end));
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        int[] offs = offsets();
        if (beginIndex < 0 || beginIndex >= offs.length || endIndex > offs.length
            || beginIndex >= endIndex)
            throw new IllegalArgumentException();
        int begin = offs[beginIndex];
        int end = (endIndex == offs.length) ? path.length() : offs[endIndex] - 1;
        return new HostPath(path.substring(begin, end));
    }

    @Override
    public boolean startsWith(Path other) {
        if (!(Objects.requireNonNull(other) instanceof HostPath))
            return false;
        HostPath that = (HostPath) other;
        // other path is longer
        if (that.path.length() > path.length())
            return false;
        int thisOffsetCount = getNameCount();
        int thatOffsetCount = that.getNameCount();
        // other path has no name elements
        if (thatOffsetCount == 0 && this.isAbsolute()) {
            return that.isEmpty() ? false : true;
        }
        // given path has more elements that this path
        if (thatOffsetCount > thisOffsetCount)
            return false;
        // same number of elements so must be exact match
        if ((thatOffsetCount == thisOffsetCount) && (path.length() != that.path.length())) {
            return false;
        }
        // check offsets of elements match
        int[] thisOffs = offsets();
        int[] thatOffs = that.offsets();
        for (int i = 0; i < thatOffsetCount; i++) {
            if (thisOffs[i] != thatOffs[i])
                return false;
        }
        // offsets match so need to compare chars
        int i = 0;
        while (i < that.path.length()) {
            if (this.path.charAt(i) != that.path.charAt(i))
                return false;
            i++;
        }
        // final check that match is on name boundary
        if (i < path.length() && this.path.charAt(i) != '/')
            return false;
        return true;
    }

    @Override
    public boolean endsWith(Path other) {
        if (!(Objects.requireNonNull(other) instanceof HostPath))
            return false;
        HostPath that = (HostPath) other;
        int thisLen = path.length();
        int thatLen = that.path.length();
        // other path is longer
        if (thatLen > thisLen)
            return false;
        // other path is the empty path
        if (thisLen > 0 && thatLen == 0)
            return false;
        // other path is absolute so this path must be absolute
        if (that.isAbsolute() && !this.isAbsolute())
            return false;
        int thisOffsetCount = getNameCount();
        int thatOffsetCount = that.getNameCount();
        // given path has more elements that this path
        if (thatOffsetCount > thisOffsetCount) {
            return false;
        } else {
            // same number of elements
            if (thatOffsetCount == thisOffsetCount) {
                if (thisOffsetCount == 0)
                    return true;
                int expectedLen = thisLen;
                if (this.isAbsolute() && !that.isAbsolute())
                    expectedLen--;
                if (thatLen != expectedLen)
                    return false;
            } else {
                // this path has more elements so given path must be relative
                if (that.isAbsolute())
                    return false;
            }
        }
        // compare chars
        int thisPos = offsets()[thisOffsetCount - thatOffsetCount];
        int thatPos = that.offsets()[0];
        return path.substring(thisPos).equals(that.path.substring(thatPos));
    }

    @Override
    public Path normalize() {
        final int count = getNameCount();
        if (count == 0 || isEmpty())
            return this;
        int[] offs = offsets();
        boolean[] ignore = new boolean[count];      // true => ignore name
        int[] size = new int[count];                // length of name
        int remaining = count;                      // number of names remaining
        boolean hasDotDot = false;                  // has at least one ..
        boolean isAbsolute = isAbsolute();

        // first pass: the lengths of the names, the "." to ignore, any ".."
        for (int i = 0; i < count; i++) {
            int begin = offs[i];
            int len = (i == offs.length - 1) ? path.length() - begin : offs[i + 1] - begin - 1;
            size[i] = len;
            if (path.charAt(begin) == '.') {
                if (len == 1) {
                    ignore[i] = true;  // ignore  "."
                    remaining--;
                } else {
                    if (path.charAt(begin + 1) == '.')   // ".." found
                        hasDotDot = true;
                }
            }
        }

        // multiple passes to eliminate all occurrences of name/..
        if (hasDotDot) {
            int prevRemaining;
            do {
                prevRemaining = remaining;
                int prevName = -1;
                for (int i = 0; i < count; i++) {
                    if (ignore[i])
                        continue;
                    // not a ".."
                    if (size[i] != 2) {
                        prevName = i;
                        continue;
                    }
                    int begin = offs[i];
                    if (path.charAt(begin) != '.' || path.charAt(begin + 1) != '.') {
                        prevName = i;
                        continue;
                    }
                    // ".." found
                    if (prevName >= 0) {
                        // name/<ignored>/.. found so mark name and ".." to be ignored
                        ignore[prevName] = true;
                        ignore[i] = true;
                        remaining = remaining - 2;
                        prevName = -1;
                    } else {
                        // Case: /<ignored>/.. so mark ".." as ignored
                        if (isAbsolute) {
                            boolean hasPrevious = false;
                            for (int j = 0; j < i; j++) {
                                if (!ignore[j]) {
                                    hasPrevious = true;
                                    break;
                                }
                            }
                            if (!hasPrevious) {
                                // all proceeding names are ignored
                                ignore[i] = true;
                                remaining--;
                            }
                        }
                    }
                }
            } while (prevRemaining > remaining);
        }

        // no redundant names
        if (remaining == count)
            return this;
        // corner case - all names removed
        if (remaining == 0) {
            return isAbsolute ? new HostPath("/") : emptyPath();
        }
        StringBuilder sb = new StringBuilder(path.length());
        if (isAbsolute)
            sb.append('/');
        for (int i = 0; i < count; i++) {
            if (!ignore[i]) {
                sb.append(path, offs[i], offs[i] + size[i]);
                if (--remaining > 0) {
                    sb.append('/');
                }
            }
        }
        return new HostPath(sb.toString());
    }

    @Override
    public Path resolve(Path obj) {
        String other = toHostPath(obj).path;
        if (other.startsWith("/"))
            return obj;
        if (other.isEmpty())
            return this;
        if (path.isEmpty())
            return obj;
        if (path.equals("/"))
            return new HostPath("/" + other);
        return new HostPath(path + "/" + other);
    }

    @Override
    public Path relativize(Path obj) {
        HostPath child = toHostPath(obj);
        if (child.equals(this))
            return emptyPath();
        // can only relativize paths of the same type
        if (this.isAbsolute() != child.isAbsolute())
            throw new IllegalArgumentException("'other' is different type of Path");
        // this path is the empty path
        if (this.isEmpty())
            return child;

        HostPath base = this;
        if (base.hasDotOrDotDot() || child.hasDotOrDotDot()) {
            base = (HostPath) base.normalize();
            child = (HostPath) child.normalize();
        }
        int baseCount = base.getNameCount();
        int childCount = child.getNameCount();

        // skip matching names
        int n = Math.min(baseCount, childCount);
        int i = 0;
        while (i < n) {
            if (!base.getName(i).equals(child.getName(i)))
                break;
            i++;
        }

        // remaining elements in child
        HostPath childRemaining;
        boolean isChildEmpty;
        if (i == childCount) {
            childRemaining = emptyPath();
            isChildEmpty = true;
        } else {
            childRemaining = (HostPath) child.subpath(i, childCount);
            isChildEmpty = childRemaining.isEmpty();
        }

        // matched all of base
        if (i == baseCount) {
            return childRemaining;
        }

        // the remainder of base cannot contain ".."
        HostPath baseRemaining = (HostPath) base.subpath(i, baseCount);
        if (baseRemaining.hasDotOrDotDot()) {
            throw new IllegalArgumentException("Unable to compute relative "
                    + " path from " + this + " to " + obj);
        }
        if (baseRemaining.isEmpty())
            return childRemaining;

        // number of ".." needed
        int dotdots = baseRemaining.getNameCount();
        if (dotdots == 0) {
            return childRemaining;
        }

        // a "../" for each remaining name in base followed by the remaining names in child; no
        // final trailing slash when the remainder is the empty path
        StringBuilder sb = new StringBuilder();
        while (dotdots > 0) {
            sb.append("..");
            if (isChildEmpty) {
                if (dotdots > 1) sb.append('/');
            } else {
                sb.append('/');
            }
            dotdots--;
        }
        sb.append(childRemaining.path);
        return new HostPath(sb.toString());
    }

    @Override
    public URI toUri() {
        String abs = toAbsolutePath().toString();
        if (!abs.endsWith("/") && new File(abs).isDirectory())
            abs = abs + "/";
        try {
            return new URI("file", "", abs, null, null);
        } catch (URISyntaxException x) {
            throw new AssertionError(x);
        }
    }

    @Override
    public Path toAbsolutePath() {
        if (isAbsolute())
            return this;
        return new HostPath(normalizeAndCheck(System.getProperty("user.dir"))).resolve(this);
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        File f = new File(toAbsolutePath().toString());
        if (!f.exists())
            throw new NoSuchFileException(path);
        return new HostPath(normalizeAndCheck(f.getCanonicalPath()));
    }

    @Override
    public File toFile() {
        return new File(path);
    }

    private byte[] bytes() {
        return path.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public int compareTo(Path other) {
        return Arrays.compareUnsigned(bytes(), ((HostPath) other).bytes());
    }

    @Override
    public boolean equals(Object ob) {
        return ob instanceof HostPath p && compareTo(p) == 0;
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            for (byte b : bytes())
                h = 31 * h + (b & 0xff);
            hash = h;
        }
        return h;
    }

    @Override
    public String toString() {
        return path;
    }
}
