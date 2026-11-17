/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Resources by URL"): the Go build's class path as
 * the class loaders' getResource and getResources see it: the resources embedded in the
 * executable (the jar of the JVM build), then the directories of ARBACE_PATH, in RT.load's
 * order (jrt's ResourceOrPath). An embedded resource's URL is a jar: URL naming the executable,
 * "jar:file:/path/to/arbace!/arbace/core.ae", as the JVM's for a resource of arbace.jar,
 * opened by this class's handler (the jar: connection of the JDK is outside the world); a
 * file of ARBACE_PATH has its file: URL, as the JVM's class path directories give it. The
 * natives are jrt's Go (go/arbace/jrt/resources.ae).
 * Since A5 of the Ghidra case study's G1 plan (doc/go/J2G-SPEC.md §10.3, "Resources by URL"): a resource
 * that several of a program's resource directories hold (the jars of its class path) is embedded once
 * a directory, the first at its path, directory K's at META-INF/arbace/roots/K/PATH, listed in the index
 * META-INF/arbace/roots (j2g, arbace.j2g.meta), so that getResources enumerates one URL a directory, in
 * the class path's order; and a translated class's class file has a URL, "jar:...!/p/C.class", as the
 * JDK's class path gives one, though no bytes (the Go build has no class files: opening it fails, so
 * getResourceAsStream answers null, as the JDK's does for a URL it cannot open). ClassLoader's resource
 * methods in translated code call the statics below (j2g's support functions, arbace.j2g.model's
 * class-loader-resource-helpers).
 * Since GH4 (iii) of that plan: the class path's entries by name. j2g embeds META-INF/arbace/classpath
 * (K NAME a line, one a resource directory in the class path's order: java.class.path is these names,
 * joined by the path separator) and META-INF/arbace/owners (K PATH a line: the entry that has a plain path
 * first, which tells an entry's resources from the JDK's and another entry's). classPath() lists the
 * names, findIn(entry, name) resolves a resource of one entry to its URL, and inDirectory answers for a
 * directory that is an entry's name as the file: URL of a directory would (a URLClassLoader over
 * new File(entry).toURI().toURL() finds the entry's embedded resources). The names are names only: no
 * file of the host's file system has them (java.io.File does not see the embedded resources).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import sun.net.www.ParseUtil;

/** The resources of the Go build's class path, by URL. */
public final class ClassPathResources {

    private ClassPathResources() {
    }

    /** The executable's path (the jar: URLs' file), found once. */
    private static String executable;

    /** The handler of the embedded resources' URLs. */
    private static final EmbeddedHandler handler = new EmbeddedHandler();

    /** Is name an embedded resource? */
    private static native boolean embedded0(String name);

    /** The embedded resource's bytes, or null. */
    private static native byte[] bytes0(String name);

    /** The executable's absolute path ("" when the host cannot tell). */
    private static native String executable0();

    private static String executable() {
        synchronized (ClassPathResources.class) {
            if (executable == null) {
                String p = executable0();
                executable = p.isEmpty() ? "/arbace" : p;
            }
            return executable;
        }
    }

    /**
     * The directories of ARBACE_PATH, in order; not its store entries (HostStore.pathEntries),
     * whose resources have no URL.
     */
    private static List<String> path() {
        List<String> dirs = new ArrayList<>();
        String p = System.getenv("ARBACE_PATH");
        if (p != null) {
            for (String d : HostStore.pathEntries(p)) {
                if (!d.startsWith("store:"))
                    dirs.add(d);
            }
        }
        return dirs;
    }

    /** The embedded resource's bytes, or null (ServiceLoader's registry, META-INF/arbace/services). */
    public static byte[] embeddedBytes(String name) {
        return bytes0(name);
    }

    /** The resource roots' index (META-INF/arbace/roots: "K NAME" a line), read once: the K's in order. */
    private static List<String> roots;

    private static List<String> roots() {
        synchronized (ClassPathResources.class) {
            if (roots == null) {
                List<String> ks = new ArrayList<>();
                byte[] b = bytes0("META-INF/arbace/roots");
                if (b != null) {
                    for (String line : new String(b, java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
                        int i = line.indexOf(' ');
                        if (i > 0)
                            ks.add(line.substring(0, i));
                    }
                }
                roots = ks;
            }
            return roots;
        }
    }

    /** The entries' names (META-INF/arbace/classpath), by index K; empty when the program has no resource directory. */
    private static List<String> entries;

    private static synchronized List<String> entries() {
        if (entries == null) {
            List<String> names = new ArrayList<>();
            byte[] b = bytes0("META-INF/arbace/classpath");
            if (b != null) {
                for (String line : new String(b, java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
                    int i = line.indexOf(' ');
                    if (i > 0)
                        names.add(line.substring(i + 1));
                }
            }
            entries = names;
        }
        return entries;
    }

    /** The first entry (index) of each plain path of the resource directories (META-INF/arbace/owners), read once. */
    private static java.util.Map<String, Integer> owners;

    private static synchronized java.util.Map<String, Integer> owners() {
        if (owners == null) {
            java.util.Map<String, Integer> m = new java.util.HashMap<>();
            byte[] b = bytes0("META-INF/arbace/owners");
            if (b != null) {
                for (String line : new String(b, java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
                    int i = line.indexOf(' ');
                    if (i > 0)
                        m.put(line.substring(i + 1), Integer.parseInt(line.substring(0, i)));
                }
            }
            owners = m;
        }
        return owners;
    }

    /** The class path's entries' names, in order (java.class.path's parts): a copy. */
    public static List<String> classPath() {
        return new ArrayList<>(entries());
    }

    /** The index of the entry named name, or -1. */
    private static int entryIndex(String name) {
        return name == null ? -1 : entries().indexOf(name);
    }

    /**
     * The URL of resource name in the class path's entry (one of classPath()'s names), a jar: URL
     * of the embedded resource, or null: the entry has no such resource, or is no entry.
     */
    public static URL findIn(String entry, String name) {
        if (name == null)
            throw new NullPointerException();
        int k = entryIndex(entry);
        if (k < 0 || name.startsWith("/"))
            return null;
        String copy = "META-INF/arbace/roots/" + k + "/" + name;
        if (embedded0(copy))
            return embeddedURL(copy);
        if (embedded0(name)) {
            Integer o = owners().get(name);
            if (o != null && o.intValue() == k)
                return embeddedURL(name);
        }
        return null;
    }

    /** The entry whose name a directory is (the name, or the file it names as the working directory has it), or null. */
    private static String entryOf(File dir) {
        List<String> es = entries();
        if (es.isEmpty())
            return null;
        try {
            File d = dir.getCanonicalFile();
            for (String e : es) {
                if (e.equals(dir.getPath()) || new File(e).getCanonicalFile().equals(d))
                    return e;
            }
        } catch (IOException x) {
        }
        return null;
    }

    /** Is name the class file of a class of the program ("p/C.class", p.C in jrt's registry)? */
    private static boolean classFile(String name) {
        if (!name.endsWith(".class") || name.startsWith("/") || name.indexOf("..") >= 0)
            return false;
        String bn = name.substring(0, name.length() - ".class".length()).replace('/', '.');
        if (bn.isEmpty() || bn.endsWith("module-info") || bn.endsWith("package-info"))
            return false;
        try {
            Class.forName(bn, false, null);
            return true;
        } catch (ClassNotFoundException | LinkageError | IllegalArgumentException e) {
            return false;
        }
    }

    /** The URL of an embedded resource. */
    private static URL embeddedURL(String name) {
        try {
            return new URL(null, "jar:" + ParseUtil.fileToEncodedURL(new File(executable())) + "!/" + name, handler);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * The URL of resource name under base, a file: URL of a directory (ending with /), if that
     * file exists, as the JDK's URLClassPath.FileLoader finds it: base resolving the encoded
     * name, a name with ".." only within the directory; null otherwise.
     */
    public static URL inDirectory(URL base, String name) {
        try {
            File dir = new File(ParseUtil.decode(base.getFile().replace('/', File.separatorChar))).getCanonicalFile();
            URL normalizedBase = new URL(base, ".");
            URL url = new URL(base, ParseUtil.encodePath(name, false));
            if (!url.getFile().startsWith(normalizedBase.getFile()))
                return null;
            File file;
            if (name.indexOf("..") != -1) {
                file = new File(dir, name.replace('/', File.separatorChar)).getCanonicalFile();
                if (!file.getPath().startsWith(dir.getPath()))
                    return null;
            } else {
                file = new File(dir, name.replace('/', File.separatorChar));
            }
            if (!file.exists() && !dir.exists()) {
                String e = entryOf(dir);
                return e == null ? null : findIn(e, name);
            }
            return file.exists() ? url : null;
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }

    /** The file: URL of a directory of ARBACE_PATH, as the JVM's class path makes it, or null. */
    private static URL directoryURL(String d) {
        try {
            File f = new File(d).getAbsoluteFile();
            return f.isDirectory() ? ParseUtil.fileToEncodedURL(f) : null;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /** The first URL of resource name on the class path, or null. */
    public static URL find(String name) {
        if (name == null)
            throw new NullPointerException();
        if (embedded0(name))
            return embeddedURL(name);
        for (String d : path()) {
            URL b = directoryURL(d);
            URL u = b == null ? null : inDirectory(b, name);
            if (u != null)
                return u;
        }
        if (classFile(name))
            return embeddedURL(name);
        return null;
    }

    /** Every URL of resource name on the class path, in order. */
    public static List<URL> findAll(String name) {
        if (name == null)
            throw new NullPointerException();
        List<URL> urls = new ArrayList<>();
        if (embedded0(name))
            urls.add(embeddedURL(name));
        for (String k : roots()) {
            String copy = "META-INF/arbace/roots/" + k + "/" + name;
            if (embedded0(copy))
                urls.add(embeddedURL(copy));
        }
        if (urls.isEmpty() && classFile(name))
            urls.add(embeddedURL(name));
        for (String d : path()) {
            URL b = directoryURL(d);
            URL u = b == null ? null : inDirectory(b, name);
            if (u != null)
                urls.add(u);
        }
        return urls;
    }

    /**
     * ClassLoader.getResource of a loader: a URLClassLoader's own; the application loader's
     * the class path's; the platform loader and the bootstrap loader (null) find none (the
     * JDK's own resources are the embedded ones, found through the application loader).
     */
    public static URL getResource(ClassLoader loader, String name) {
        if (loader instanceof URLClassLoader)
            return ((URLClassLoader) loader).getResource(name);
        if (loader == ClassLoader.getSystemClassLoader())
            return find(name);
        if (name == null)
            throw new NullPointerException();
        return null;
    }

    /** ClassLoader.getResources of a loader, as getResource finds them. */
    public static Enumeration<URL> getResources(ClassLoader loader, String name) throws IOException {
        if (loader instanceof URLClassLoader)
            return ((URLClassLoader) loader).getResources(name);
        if (loader == ClassLoader.getSystemClassLoader())
            return Collections.enumeration(findAll(name));
        if (name == null)
            throw new NullPointerException();
        return Collections.emptyEnumeration();
    }

    /**
     * ClassLoader.getResourceAsStream of a loader, as the JDK's: getResource's URL opened, null when
     * there is none or it cannot be opened (a class file's).
     */
    public static InputStream getResourceAsStream(ClassLoader loader, String name) {
        URL u = loader == null ? find(name) : loader.getResource(name);
        try {
            return u == null ? null : u.openStream();
        } catch (IOException e) {
            return null;
        }
    }

    /** ClassLoader.getSystemResource: the system loader's. */
    public static URL systemResource(String name) {
        return getResource(ClassLoader.getSystemClassLoader(), name);
    }

    /** ClassLoader.getSystemResources: the system loader's. */
    public static Enumeration<URL> systemResources(String name) throws IOException {
        return getResources(ClassLoader.getSystemClassLoader(), name);
    }

    /** ClassLoader.getSystemResourceAsStream: the system loader's. */
    public static InputStream systemResourceAsStream(String name) {
        return getResourceAsStream(ClassLoader.getSystemClassLoader(), name);
    }

    /** The handler of the embedded resources' jar: URLs: their connection reads the resource. */
    static final class EmbeddedHandler extends sun.net.www.protocol.jar.Handler {
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            String s = u.getFile();
            int i = s.indexOf("!/");
            if (i < 0)
                throw new MalformedURLException("no !/ in spec");
            return new EmbeddedConnection(u, ParseUtil.decode(s.substring(i + 2)));
        }
    }

    /** The connection of an embedded resource's URL. */
    static final class EmbeddedConnection extends URLConnection {
        private final String name;
        private byte[] bytes;

        EmbeddedConnection(URL u, String name) {
            super(u);
            this.name = name;
        }

        @Override
        public void connect() throws IOException {
            if (!connected) {
                bytes = bytes0(name);
                if (bytes == null)
                    throw new FileNotFoundException("JAR entry " + name + " not found in " + executable());
                connected = true;
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            connect();
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public long getContentLengthLong() {
            try {
                connect();
            } catch (IOException e) {
                return -1;
            }
            return bytes.length;
        }
    }
}
