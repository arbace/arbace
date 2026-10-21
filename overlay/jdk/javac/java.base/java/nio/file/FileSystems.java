/*
 * Arbace (overlay/jdk/javac, doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN3"): jrt's
 * java.nio.file.FileSystems for the world of javac's tool build, written for jrt in place of
 * jdk26u's, which finds file system providers through ServiceLoader and the platform's
 * DefaultFileSystemProvider: jrt's paths (overlay/jdk/java.base/java/nio/file/Path.java) have no
 * provider and no file system object. getDefault() is a file system of jrt's paths that gives
 * paths (getPath, as Path.of), its separator and root, and throws for the rest; no other file
 * system can be made or found (ProviderNotFoundException, as on a JVM without the provider).
 * javac's PathFileObject holds the default file system in a static field, initialized when
 * ClassWriter asks it a file's simple name (the SourceFile attribute); javac on Arbace reads and
 * writes its files through its own file manager (JFS5 (b)), which never asks this one. Written
 * from jdk26u's documented API (https://github.com/openjdk/jdk26u at baf63fb), without its code:
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.nio.file;

import java.io.IOException;
import java.net.URI;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Factory methods for file systems: the default one, of jrt's paths. */
public final class FileSystems {
    private FileSystems() {
    }

    /** The default file system: jrt's paths. */
    private static final class Default extends FileSystem {
        static final Default INSTANCE = new Default();

        @Override
        public FileSystemProvider provider() {
            throw new UnsupportedOperationException("java.nio.file.FileSystem.provider: not in jrt");
        }

        @Override
        public void close() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public String getSeparator() {
            return "/";
        }

        @Override
        public Iterable<Path> getRootDirectories() {
            return List.of(Path.of("/"));
        }

        @Override
        public Iterable<FileStore> getFileStores() {
            throw new UnsupportedOperationException("java.nio.file.FileSystem.getFileStores: not in jrt");
        }

        @Override
        public Set<String> supportedFileAttributeViews() {
            return Set.of();
        }

        @Override
        public Path getPath(String first, String... more) {
            return Path.of(first, more);
        }

        @Override
        public PathMatcher getPathMatcher(String syntaxAndPattern) {
            throw new UnsupportedOperationException("java.nio.file.FileSystem.getPathMatcher: not in jrt");
        }

        @Override
        public UserPrincipalLookupService getUserPrincipalLookupService() {
            throw new UnsupportedOperationException("java.nio.file.FileSystem.getUserPrincipalLookupService: not in jrt");
        }

        @Override
        public WatchService newWatchService() throws IOException {
            throw new UnsupportedOperationException("java.nio.file.FileSystem.newWatchService: not in jrt");
        }
    }

    public static FileSystem getDefault() {
        return Default.INSTANCE;
    }

    public static FileSystem getFileSystem(URI uri) {
        throw new ProviderNotFoundException("Provider \"" + uri.getScheme() + "\" not found");
    }

    public static FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        throw new ProviderNotFoundException("Provider \"" + uri.getScheme() + "\" not found");
    }

    public static FileSystem newFileSystem(URI uri, Map<String, ?> env, ClassLoader loader) throws IOException {
        throw new ProviderNotFoundException("Provider \"" + uri.getScheme() + "\" not found");
    }

    public static FileSystem newFileSystem(Path path, ClassLoader loader) throws IOException {
        throw new ProviderNotFoundException("Provider not found");
    }

    public static FileSystem newFileSystem(Path path, Map<String, ?> env) throws IOException {
        throw new ProviderNotFoundException("Provider not found");
    }

    public static FileSystem newFileSystem(Path path) throws IOException {
        throw new ProviderNotFoundException("Provider not found");
    }

    public static FileSystem newFileSystem(Path path, Map<String, ?> env, ClassLoader loader) throws IOException {
        throw new ProviderNotFoundException("Provider not found");
    }
}
