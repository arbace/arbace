/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Files"): jrt's java.nio.file.Files, written for jrt
 * in place of jdk26u's, whose operations go to the file system providers (sun.nio.fs and the
 * VM's natives, outside the Go build). This Files holds the operations Arbace and Clojure's
 * suite use and their common kin, with the documented behaviour, over java.io.File and the file
 * streams (the host's file system, jrt's UnixFileSystem): reading and writing (streams,
 * readers, writers, bytes, strings, lines), the tests of a file (exists, isDirectory, size...),
 * creating and deleting files and directories, temporary files, copy and move of a file. Not
 * here: channels, attribute views, directory streams and walks, links, file stores.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.nio.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Stream;

/** Static methods on the files and directories of the host's file system. */
public final class Files {

    private Files() { }

    private static final Random RANDOM = new Random();

    private static File file(Path path) {
        return Objects.requireNonNull(path).toFile();
    }

    private static boolean has(OpenOption[] options, OpenOption option) {
        for (OpenOption o : options) {
            if (o == option)
                return true;
        }
        return false;
    }

    // -- streams, readers, writers --

    /** An input stream reading the file. */
    public static InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        for (OpenOption o : options) {
            if (o == StandardOpenOption.APPEND || o == StandardOpenOption.WRITE)
                throw new UnsupportedOperationException("'" + o + "' not allowed");
        }
        File f = file(path);
        if (!f.exists())
            throw new NoSuchFileException(path.toString());
        try {
            return new FileInputStream(f);
        } catch (FileNotFoundException x) {
            throw new FileSystemException(path.toString(), null, reason(x));
        }
    }

    /** An output stream writing the file: created or truncated by default, or as options say. */
    public static OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
        File f = file(path);
        boolean append = has(options, StandardOpenOption.APPEND);
        if (append && has(options, StandardOpenOption.TRUNCATE_EXISTING))
            throw new IllegalArgumentException("APPEND + TRUNCATE_EXISTING not allowed");
        if (has(options, StandardOpenOption.READ))
            throw new IllegalArgumentException("READ not allowed");
        boolean create = options.length == 0 || has(options, StandardOpenOption.CREATE)
            || has(options, StandardOpenOption.CREATE_NEW);
        if (has(options, StandardOpenOption.CREATE_NEW) && f.exists())
            throw new FileAlreadyExistsException(path.toString());
        if (!create && !f.exists())
            throw new NoSuchFileException(path.toString());
        try {
            return new FileOutputStream(f, append);
        } catch (FileNotFoundException x) {
            File parent = f.getAbsoluteFile().getParentFile();
            if (parent != null && !parent.exists())
                throw new NoSuchFileException(path.toString());
            throw new FileSystemException(path.toString(), null, reason(x));
        }
    }

    private static String reason(FileNotFoundException x) {
        String m = x.getMessage();
        int i = (m == null) ? -1 : m.lastIndexOf(" (");
        return (i < 0 || !m.endsWith(")")) ? m : m.substring(i + 2, m.length() - 1);
    }

    /** A buffered reader of the file in charset cs. */
    public static BufferedReader newBufferedReader(Path path, Charset cs) throws IOException {
        return new BufferedReader(new InputStreamReader(newInputStream(path), cs));
    }

    /** A buffered reader of the file in UTF-8. */
    public static BufferedReader newBufferedReader(Path path) throws IOException {
        return newBufferedReader(path, StandardCharsets.UTF_8);
    }

    /** A buffered writer of the file in charset cs. */
    public static BufferedWriter newBufferedWriter(Path path, Charset cs, OpenOption... options)
        throws IOException
    {
        return new BufferedWriter(new OutputStreamWriter(newOutputStream(path, options), cs));
    }

    /** A buffered writer of the file in UTF-8. */
    public static BufferedWriter newBufferedWriter(Path path, OpenOption... options) throws IOException {
        return newBufferedWriter(path, StandardCharsets.UTF_8, options);
    }

    // -- whole files --

    /** The bytes of the file. */
    public static byte[] readAllBytes(Path path) throws IOException {
        try (InputStream in = newInputStream(path)) {
            return in.readAllBytes();
        }
    }

    /** The text of the file in charset cs. */
    public static String readString(Path path, Charset cs) throws IOException {
        return new String(readAllBytes(path), cs);
    }

    /** The text of the file in UTF-8. */
    public static String readString(Path path) throws IOException {
        return readString(path, StandardCharsets.UTF_8);
    }

    /** The lines of the file in charset cs. */
    public static List<String> readAllLines(Path path, Charset cs) throws IOException {
        try (BufferedReader reader = newBufferedReader(path, cs)) {
            List<String> result = new ArrayList<>();
            for (;;) {
                String line = reader.readLine();
                if (line == null)
                    break;
                result.add(line);
            }
            return result;
        }
    }

    /** The lines of the file in UTF-8. */
    public static List<String> readAllLines(Path path) throws IOException {
        return readAllLines(path, StandardCharsets.UTF_8);
    }

    /** The lines of the file in charset cs, read as the stream is consumed. */
    public static Stream<String> lines(Path path, Charset cs) throws IOException {
        BufferedReader br = newBufferedReader(path, cs);
        return br.lines().onClose(() -> {
            try {
                br.close();
            } catch (IOException x) {
                throw new java.io.UncheckedIOException(x);
            }
        });
    }

    /** The lines of the file in UTF-8, read as the stream is consumed. */
    public static Stream<String> lines(Path path) throws IOException {
        return lines(path, StandardCharsets.UTF_8);
    }

    /** Writes bytes to the file (created or truncated by default). */
    public static Path write(Path path, byte[] bytes, OpenOption... options) throws IOException {
        Objects.requireNonNull(bytes);
        try (OutputStream out = newOutputStream(path, options)) {
            out.write(bytes);
        }
        return path;
    }

    /** Writes lines to the file in charset cs, each followed by the line separator. */
    public static Path write(Path path, Iterable<? extends CharSequence> lines, Charset cs,
                             OpenOption... options) throws IOException
    {
        Objects.requireNonNull(lines);
        try (BufferedWriter writer = newBufferedWriter(path, cs, options)) {
            for (CharSequence line : lines) {
                writer.append(line);
                writer.newLine();
            }
        }
        return path;
    }

    /** Writes lines to the file in UTF-8, each followed by the line separator. */
    public static Path write(Path path, Iterable<? extends CharSequence> lines, OpenOption... options)
        throws IOException
    {
        return write(path, lines, StandardCharsets.UTF_8, options);
    }

    /** Writes text to the file in charset cs. */
    public static Path writeString(Path path, CharSequence csq, Charset cs, OpenOption... options)
        throws IOException
    {
        Objects.requireNonNull(csq);
        return write(path, String.valueOf(csq).getBytes(cs), options);
    }

    /** Writes text to the file in UTF-8. */
    public static Path writeString(Path path, CharSequence csq, OpenOption... options) throws IOException {
        return writeString(path, csq, StandardCharsets.UTF_8, options);
    }

    // -- tests --

    public static boolean exists(Path path, LinkOption... options) {
        return file(path).exists();
    }

    public static boolean notExists(Path path, LinkOption... options) {
        return !file(path).exists();
    }

    public static boolean isDirectory(Path path, LinkOption... options) {
        return file(path).isDirectory();
    }

    public static boolean isRegularFile(Path path, LinkOption... options) {
        return file(path).isFile();
    }

    public static boolean isReadable(Path path) {
        return file(path).canRead();
    }

    public static boolean isWritable(Path path) {
        return file(path).canWrite();
    }

    public static boolean isExecutable(Path path) {
        return file(path).canExecute();
    }

    public static boolean isHidden(Path path) throws IOException {
        return file(path).isHidden();
    }

    public static boolean isSameFile(Path path, Path path2) throws IOException {
        if (path.equals(path2))
            return true;
        if (!exists(path))
            throw new NoSuchFileException(path.toString());
        if (!exists(path2))
            throw new NoSuchFileException(path2.toString());
        return path.toRealPath().equals(path2.toRealPath());
    }

    /** The size of the file in bytes. */
    public static long size(Path path) throws IOException {
        File f = file(path);
        if (!f.exists())
            throw new NoSuchFileException(path.toString());
        return f.length();
    }

    // -- creating and deleting --

    /** Creates an empty file, failing if it exists. */
    public static Path createFile(Path path, FileAttribute<?>... attrs) throws IOException {
        File f = file(path);
        boolean created;
        try {
            created = f.createNewFile();
        } catch (IOException x) {
            throw new NoSuchFileException(path.toString());
        }
        if (!created)
            throw new FileAlreadyExistsException(path.toString());
        return path;
    }

    /** Creates a directory, failing if it exists or its parent does not. */
    public static Path createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        File f = file(dir);
        if (f.mkdir())
            return dir;
        if (f.exists())
            throw new FileAlreadyExistsException(dir.toString());
        File parent = f.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists())
            throw new NoSuchFileException(dir.toString());
        throw new AccessDeniedException(dir.toString());
    }

    /** Creates a directory and its missing parents; nothing when it exists as a directory. */
    public static Path createDirectories(Path dir, FileAttribute<?>... attrs) throws IOException {
        File f = file(dir);
        if (f.isDirectory())
            return dir;
        if (f.exists())
            throw new FileAlreadyExistsException(dir.toString());
        Path parent = dir.toAbsolutePath().getParent();
        if (parent != null)
            createDirectories(parent);
        if (!f.mkdir() && !f.isDirectory())
            throw new AccessDeniedException(dir.toString());
        return dir;
    }

    private static Path createTemp(Path dir, String prefix, String suffix, boolean directory)
        throws IOException
    {
        if (prefix == null)
            prefix = "";
        if (suffix == null)
            suffix = directory ? "" : ".tmp";
        if (dir == null)
            dir = Path.of(System.getProperty("java.io.tmpdir"));
        if (prefix.indexOf('/') >= 0 || suffix.indexOf('/') >= 0)
            throw new IllegalArgumentException("Invalid prefix or suffix");
        for (;;) {
            String name = prefix + Long.toUnsignedString(RANDOM.nextLong()) + suffix;
            Path p = dir.resolve(name);
            File f = p.toFile();
            if (directory) {
                if (f.mkdir())
                    return p;
                if (!f.exists())
                    throw new NoSuchFileException(dir.toString());
            } else {
                boolean created;
                try {
                    created = f.createNewFile();
                } catch (IOException x) {
                    throw new NoSuchFileException(dir.toString());
                }
                if (created)
                    return p;
            }
        }
    }

    /** Creates a new empty file in dir, its name made of prefix, random digits and suffix. */
    public static Path createTempFile(Path dir, String prefix, String suffix, FileAttribute<?>... attrs)
        throws IOException
    {
        return createTemp(Objects.requireNonNull(dir), prefix, suffix, false);
    }

    /** Creates a new empty file in java.io.tmpdir. */
    public static Path createTempFile(String prefix, String suffix, FileAttribute<?>... attrs)
        throws IOException
    {
        return createTemp(null, prefix, suffix, false);
    }

    /** Creates a new directory in dir. */
    public static Path createTempDirectory(Path dir, String prefix, FileAttribute<?>... attrs)
        throws IOException
    {
        return createTemp(Objects.requireNonNull(dir), prefix, null, true);
    }

    /** Creates a new directory in java.io.tmpdir. */
    public static Path createTempDirectory(String prefix, FileAttribute<?>... attrs) throws IOException {
        return createTemp(null, prefix, null, true);
    }

    /** Deletes a file or an empty directory. */
    public static void delete(Path path) throws IOException {
        File f = file(path);
        if (f.delete())
            return;
        if (!f.exists())
            throw new NoSuchFileException(path.toString());
        String[] names = f.list();
        if (names != null && names.length > 0)
            throw new DirectoryNotEmptyException(path.toString());
        throw new AccessDeniedException(path.toString());
    }

    /** Deletes a file or an empty directory if it exists: whether it was deleted. */
    public static boolean deleteIfExists(Path path) throws IOException {
        if (!file(path).exists())
            return false;
        delete(path);
        return true;
    }

    // -- copy and move --

    /** Copies a file (a directory: an empty one), failing if target exists unless REPLACE_EXISTING. */
    public static Path copy(Path source, Path target, CopyOption... options) throws IOException {
        File from = file(source);
        File to = file(target);
        if (!from.exists())
            throw new NoSuchFileException(source.toString());
        if (to.exists()) {
            if (!hasCopyOption(options, StandardCopyOption.REPLACE_EXISTING))
                throw new FileAlreadyExistsException(target.toString());
            delete(target);
        }
        if (from.isDirectory()) {
            createDirectory(target);
        } else {
            try (InputStream in = new FileInputStream(from);
                 OutputStream out = newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                in.transferTo(out);
            }
        }
        if (hasCopyOption(options, StandardCopyOption.COPY_ATTRIBUTES))
            to.setLastModified(from.lastModified());
        return target;
    }

    /** Copies everything in to a file, failing if target exists unless REPLACE_EXISTING. */
    public static long copy(InputStream in, Path target, CopyOption... options) throws IOException {
        Objects.requireNonNull(in);
        if (file(target).exists()) {
            if (!hasCopyOption(options, StandardCopyOption.REPLACE_EXISTING))
                throw new FileAlreadyExistsException(target.toString());
            delete(target);
        }
        try (OutputStream out = newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
            return in.transferTo(out);
        }
    }

    /** Copies the file to out. */
    public static long copy(Path source, OutputStream out) throws IOException {
        Objects.requireNonNull(out);
        try (InputStream in = newInputStream(source)) {
            return in.transferTo(out);
        }
    }

    /** Moves (renames) a file, failing if target exists unless REPLACE_EXISTING. */
    public static Path move(Path source, Path target, CopyOption... options) throws IOException {
        File from = file(source);
        File to = file(target);
        if (!from.exists())
            throw new NoSuchFileException(source.toString());
        if (to.exists() && !source.toAbsolutePath().normalize().equals(target.toAbsolutePath().normalize())) {
            if (!hasCopyOption(options, StandardCopyOption.REPLACE_EXISTING))
                throw new FileAlreadyExistsException(target.toString());
            delete(target);
        }
        if (!from.renameTo(to))
            throw new FileSystemException(source.toString(), target.toString(), "Rename failed");
        return target;
    }

    private static boolean hasCopyOption(CopyOption[] options, CopyOption option) {
        for (CopyOption o : options) {
            if (o == option)
                return true;
        }
        return false;
    }
}
